package rutasaereas.ui;

import java.util.List;
import javafx.animation.AnimationTimer;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.shape.Polyline;
import rutasaereas.modelo.ResultadoRuta;
import rutasaereas.modelo.Ruta;

/**
 * Mueve el avión tramo por tramo sobre la ruta.
 *
 * Funciona como una pequeña máquina de estados:
 *   VOLANDO   -> el avión avanza de A a B
 *   ESCALA    -> el avión se queda quieto un momento en el aeropuerto intermedio
 *   TERMINADO -> llegó al destino
 *
 * Interpolación: p va de 0 a 1 (fracción del tramo recorrida). La posición la da
 * VistaMapa.puntoEnRuta(tramo, p), que interpola sobre la curva del tramo:
 *   B(p) = (1-p)^2 * A  +  2(1-p)p * C  +  p^2 * B
 *
 * AnimationTimer llama a handle() unas 60 veces por segundo con la hora actual
 * en nanosegundos; con la diferencia entre llamadas (dt) se avanza el tiempo.
 */
public class AnimadorVuelo {

    /** Lo que el panel necesita saber en cada cuadro. */
    public interface Oyente {
        void actualizar(String tramo, String acumulado, double progresoTotal);
    }

    private enum Fase { VOLANDO, ESCALA, TERMINADO }

    private static final double PAUSA_ESCALA = 1.0;   // segundos detenido en cada escala

    private final VistaMapa mapa;
    private final Node avion;
    private final Polyline estela;
    private List<Ruta> tramos;
    private int indice;            // tramo actual
    private double tiempoFase;     // segundos dentro de la fase actual
    private Fase fase = Fase.TERMINADO;
    private boolean pausado;
    private long ultimoInstante = -1;

    private Oyente oyente = (a, b, c) -> { };

    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long ahora) {
            if (ultimoInstante < 0) {        // primer cuadro: solo tomar la referencia
                ultimoInstante = ahora;
                return;
            }
            double dt = (ahora - ultimoInstante) / 1_000_000_000.0;
            ultimoInstante = ahora;
            avanzar(dt);
        }
    };

    public AnimadorVuelo(VistaMapa mapa) {
        this.mapa = mapa;
        this.avion = mapa.getAvion();
        this.estela = mapa.getEstela();
    }

    public void setOyente(Oyente o) {
        this.oyente = o;
    }

    // ======================= Controles =======================

    /** Empieza a animar una ruta nueva desde el origen. */
    public void iniciar(ResultadoRuta ruta) {
        this.tramos = ruta.getTramos();
        reiniciar();
    }

    /** Vuelve al origen y arranca de nuevo. */
    public void reiniciar() {
        if (tramos == null || tramos.isEmpty()) return;
        indice = 0;
        tiempoFase = 0;
        fase = Fase.VOLANDO;
        pausado = false;
        ultimoInstante = -1;
        estela.getPoints().clear();
        avion.setVisible(true);
        avanzar(0);
        timer.start();
    }

    /** Alterna entre pausa y reanudar. Devuelve true si quedó pausado. */
    public boolean alternarPausa() {
        if (fase == Fase.TERMINADO) return false;
        pausado = !pausado;
        if (pausado) {
            timer.stop();
        } else {
            ultimoInstante = -1;    // para que el tiempo en pausa no cuente
            timer.start();
        }
        return pausado;
    }

    /** Detiene todo y oculta el avión. */
    public void detener() {
        timer.stop();
        fase = Fase.TERMINADO;
        pausado = false;
        tramos = null;
        avion.setVisible(false);
        estela.getPoints().clear();
        oyente.actualizar("Sin vuelo en curso", "—", 0);
    }

    // ======================= Lógica de cada cuadro =======================

    /** Duración en pantalla de un tramo: proporcional a sus minutos reales. */
    private double duracion(Ruta r) {
        return 0.8 + r.getMinutos() / 120.0;
    }

    private void avanzar(double dt) {
        tiempoFase += dt;
        Ruta tramo = tramos.get(indice);

        if (fase == Fase.VOLANDO) {
            double p = Math.min(1.0, tiempoFase / duracion(tramo));
            ubicarAvion(tramo, p);
            informar(tramo, p);

            if (p >= 1.0) {                              // llegó al final del tramo
                if (indice == tramos.size() - 1) {
                    fase = Fase.TERMINADO;
                    timer.stop();
                    oyente.actualizar("Llegó a " + tramo.getDestino() + " (destino)",
                            acumulado(tramos.size(), 0, 0), 1.0);
                } else {
                    fase = Fase.ESCALA;
                    tiempoFase = 0;
                    oyente.actualizar("Escala en " + tramo.getDestino()
                            + " · tramo " + (indice + 1) + " de " + tramos.size() + " completado",
                            acumulado(indice + 1, 0, 0), progreso(indice + 1, 0));
                }
            }
        } else if (fase == Fase.ESCALA && tiempoFase >= PAUSA_ESCALA) {
            indice++;                                    // despega al siguiente tramo
            fase = Fase.VOLANDO;
            tiempoFase = 0;
        }
    }

    private void ubicarAvion(Ruta r, double p) {
        Point2D pos = mapa.puntoEnRuta(r, p);       // interpolación sobre la curva
        avion.setLayoutX(pos.getX());
        avion.setLayoutY(pos.getY());
        avion.setRotate(mapa.anguloEnRuta(r, p));   // que apunte hacia donde va
        estela.getPoints().addAll(pos.getX(), pos.getY());
    }

    private void informar(Ruta r, double p) {
        String texto = "Tramo " + (indice + 1) + " de " + tramos.size() + ":  "
                + r.getOrigen() + " → " + r.getDestino();
        // Minutos: avanzan con el vuelo. Costo: el tramo se paga al despegar.
        oyente.actualizar(texto,
                acumulado(indice, (int) Math.round(r.getMinutos() * p), r.getCosto()),
                progreso(indice, p));
    }

    /** Fracción del viaje completo (por minutos) ya recorrida. */
    private double progreso(int completos, double pTramo) {
        double total = 0, hecho = 0;
        for (int i = 0; i < tramos.size(); i++) {
            int m = tramos.get(i).getMinutos();
            total += m;
            if (i < completos) hecho += m;
            else if (i == completos) hecho += m * pTramo;
        }
        return total == 0 ? 1 : hecho / total;
    }

    /** Suma de los primeros "completos" tramos más lo que llevamos del actual. */
    private String acumulado(int completos, int minExtra, int costoExtra) {
        int min = minExtra, costo = costoExtra;
        for (int i = 0; i < completos; i++) {
            min += tramos.get(i).getMinutos();
            costo += tramos.get(i).getCosto();
        }
        return "Acumulado:  " + min + " min  ·  $" + costo;
    }
}

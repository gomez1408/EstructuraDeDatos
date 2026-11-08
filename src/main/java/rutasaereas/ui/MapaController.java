package rutasaereas.ui;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import rutasaereas.algoritmos.BFS;
import rutasaereas.algoritmos.DFS;
import rutasaereas.algoritmos.Dijkstra;
import rutasaereas.modelo.Aeropuerto;
import rutasaereas.modelo.CargadorDatos;
import rutasaereas.modelo.Criterio;
import rutasaereas.modelo.Grafo;
import rutasaereas.modelo.ResultadoRuta;
import rutasaereas.modelo.Ruta;

/**
 * Controlador de mapa.fxml: conecta los botones con el grafo y los algoritmos.
 * Aquí NO hay lógica de grafos; solo se valida la entrada, se llama al algoritmo
 * correspondiente y se muestra el resultado (texto + mapa + animación).
 */
public class MapaController {

    @FXML private StackPane contenedorMapa;
    @FXML private ComboBox<String> cbOrigen;
    @FXML private ComboBox<String> cbDestino;
    @FXML private Label lblEstado;
    @FXML private Label lblTramo;
    @FXML private Label lblAcumulado;
    @FXML private Button btnPausa;
    @FXML private TextArea txtResultado;
    @FXML private Label lblVuelos;
    @FXML private Label lblTiempo;
    @FXML private Label lblCosto;
    @FXML private ProgressBar pbVuelo;
    @FXML private CheckBox chkFondo;

    private Grafo grafo;
    private final VistaMapa mapa = new VistaMapa();
    private AnimadorVuelo animador;
    private boolean siguienteClicEsOrigen = true;

    /** Lo llama JavaFX automáticamente después de cargar el FXML. */
    @FXML
    private void initialize() {
        // El mapa mide 1000x1000; se escala para que quepa en el espacio disponible.
        contenedorMapa.getChildren().add(new Group(mapa));
        contenedorMapa.widthProperty().addListener((o, a, b) -> ajustarEscala());
        contenedorMapa.heightProperty().addListener((o, a, b) -> ajustarEscala());

        animador = new AnimadorVuelo(mapa);
        animador.setOyente((tramo, acumulado, progreso) -> {
            lblTramo.setText(tramo);
            lblAcumulado.setText(acumulado);
            pbVuelo.setProgress(progreso);
        });

        mapa.setAlHacerClic(this::clicEnAeropuerto);
        cbOrigen.setOnAction(e -> refrescarMarcas());
        cbDestino.setOnAction(e -> refrescarMarcas());

        // Intenta cargar los archivos de la carpeta del proyecto
        File fa = new File("aeropuertos.txt");
        File fr = new File("rutas.txt");
        if (fa.exists() && fr.exists()) {
            cargar(fa, fr);
        } else {
            lblEstado.setText("No se encontraron aeropuertos.txt y rutas.txt en "
                    + new File(".").getAbsoluteFile().getParent() + ". Use «Cargar archivos…».");
        }
    }

    private void ajustarEscala() {
        double disponible = Math.min(contenedorMapa.getWidth(), contenedorMapa.getHeight()) - 16;
        double s = Math.max(0.2, disponible / VistaMapa.TAM);
        mapa.setScaleX(s);
        mapa.setScaleY(s);
    }

    // ======================= Carga de archivos =======================

    @FXML
    private void onCargarArchivos() {
        Window w = contenedorMapa.getScene().getWindow();
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(".").getAbsoluteFile());
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Texto", "*.txt"));

        fc.setTitle("1/2 · Archivo de AEROPUERTOS");
        File fa = fc.showOpenDialog(w);
        if (fa == null) return;
        fc.setInitialDirectory(fa.getParentFile());
        fc.setTitle("2/2 · Archivo de RUTAS");
        File fr = fc.showOpenDialog(w);
        if (fr == null) return;
        cargar(fa, fr);
    }

    private void cargar(File fa, File fr) {
        try {
            grafo = CargadorDatos.cargar(fa, fr);
        } catch (Exception ex) {
            lblEstado.setText("Error al cargar: " + ex.getMessage());
            return;
        }
        animador.detener();
        List<String> codigos = grafo.getCodigos();
        codigos.sort(null);
        cbOrigen.getItems().setAll(codigos);
        cbDestino.getItems().setAll(codigos);
        cbOrigen.setValue(null);
        cbDestino.setValue(null);
        mapa.dibujar(grafo);
        lblEstado.setText("Cargados " + grafo.cantidadAeropuertos() + " aeropuertos y "
                + grafo.cantidadRutas() + " rutas (" + fa.getName() + ", " + fr.getName() + ").");
        txtResultado.setText("Elija origen y destino (en las listas o con clic en el mapa) y un criterio.");
    }

    // ======================= Selección =======================

    /** Clics alternados: el 1.º fija el origen, el 2.º el destino. También muestra el aeropuerto (4.1). */
    private void clicEnAeropuerto(String codigo) {
        if (siguienteClicEsOrigen) cbOrigen.setValue(codigo);
        else cbDestino.setValue(codigo);
        siguienteClicEsOrigen = !siguienteClicEsOrigen;
        mostrarAeropuerto(codigo);
    }

    private void refrescarMarcas() {
        if (grafo == null) return;
        mapa.marcarOrigenDestino(texto(cbOrigen), texto(cbDestino));
    }

    /** Lee lo escrito o elegido en un ComboBox, en mayúsculas. */
    private String texto(ComboBox<String> cb) {
        String v = cb.isEditable() ? cb.getEditor().getText() : cb.getValue();
        if ((v == null || v.isBlank()) && cb.getValue() != null) v = cb.getValue();
        return v == null ? "" : v.trim().toUpperCase();
    }

    /**
     * Valida un código (caso especial 4.6: código inexistente).
     * Devuelve el código si es válido, o null después de mostrar el mensaje.
     */
    private String validar(ComboBox<String> cb, String nombre) {
        if (grafo == null) {
            txtResultado.setText("Primero cargue los archivos de datos.");
            return null;
        }
        String c = texto(cb);
        if (c.isEmpty()) {
            txtResultado.setText("Falta elegir el " + nombre + ".");
            return null;
        }
        if (!grafo.existe(c)) {
            txtResultado.setText("El aeropuerto «" + c + "» (" + nombre + ") no existe.");
            return null;
        }
        return c;
    }

    // ======================= Búsqueda de rutas (4.2 y 4.3) =======================

    @FXML private void onMenosEscalas() { buscar(null); }
    @FXML private void onMasRapida()    { buscar(Criterio.MINUTOS); }
    @FXML private void onMasBarata()    { buscar(Criterio.COSTO); }

    /** criterio == null -> BFS (menos escalas); si no, Dijkstra con ese criterio. */
    private void buscar(Criterio criterio) {
        String o = validar(cbOrigen, "origen");
        if (o == null) return;
        String d = validar(cbDestino, "destino");
        if (d == null) return;

        animador.detener();
        mapa.limpiarResaltado();
        mapa.marcarOrigenDestino(o, d);
        mostrarTotales(null);

        if (o.equals(d)) {   // caso especial 4.6
            lblVuelos.setText("0"); lblTiempo.setText("0 min"); lblCosto.setText("$0");
            txtResultado.setText("El origen y el destino son el mismo aeropuerto (" + o + ").\n"
                    + "No hace falta volar: 0 vuelos, 0 min, $0.");
            return;
        }

        ResultadoRuta r;
        String titulo;
        if (criterio == null) {
            r = BFS.menosEscalas(grafo, o, d);
            titulo = "RUTA CON MENOS ESCALAS (BFS)";
        } else if (criterio == Criterio.MINUTOS) {
            r = Dijkstra.rutaOptima(grafo, o, d, Criterio.MINUTOS);
            titulo = "RUTA MÁS RÁPIDA (Dijkstra por minutos)";
        } else {
            r = Dijkstra.rutaOptima(grafo, o, d, Criterio.COSTO);
            titulo = "RUTA MÁS BARATA (Dijkstra por costo)";
        }

        if (r == null) {     // caso especial 4.6
            txtResultado.setText(titulo + "\n\nNo existe ruta de " + o + " a " + d + ".");
            return;
        }

        StringBuilder sb = new StringBuilder(titulo).append("\n\n");
        sb.append("Secuencia: ").append(r.getSecuencia()).append("\n");
        sb.append("Vuelos:    ").append(r.getVuelos()).append(criterio == null ? "   ← criterio" : "").append("\n");
        sb.append("Minutos:   ").append(ResultadoRuta.formatoMinutos(r.getMinutos()))
          .append(criterio == Criterio.MINUTOS ? "   ← criterio" : "").append("\n");
        sb.append("Costo:     $").append(r.getCosto())
          .append(criterio == Criterio.COSTO ? "   ← criterio" : "").append("\n\nTramos:\n");
        for (Ruta t : r.getTramos()) sb.append("  ").append(t).append("\n");
        txtResultado.setText(sb.toString());

        mostrarTotales(r);
        mapa.resaltarRuta(r);
        animador.iniciar(r);
        btnPausa.setText("Pausar");
    }

    /** Llena las tres tarjetas (vuelos, duración, costo); con null las deja en "—". */
    private void mostrarTotales(ResultadoRuta r) {
        if (r == null) {
            lblVuelos.setText("—");
            lblTiempo.setText("—");
            lblCosto.setText("—");
            return;
        }
        lblVuelos.setText(String.valueOf(r.getVuelos()));
        lblTiempo.setText(r.getMinutos() / 60 + " h " + String.format("%02d", r.getMinutos() % 60));
        lblCosto.setText("$" + r.getCosto());
    }

    /** Intercambia origen y destino. */
    @FXML
    private void onIntercambiar() {
        String o = texto(cbOrigen), d = texto(cbDestino);
        cbOrigen.setValue(d.isEmpty() ? null : d);
        cbDestino.setValue(o.isEmpty() ? null : o);
        refrescarMarcas();
    }

    /** Muestra u oculta la imagen de fondo del continente. */
    @FXML
    private void onFondo() {
        mapa.setFondoVisible(chkFondo.isSelected());
    }

    // ======================= Consulta de aeropuerto (4.1) =======================

    @FXML
    private void onVerAeropuerto() {
        String o = validar(cbOrigen, "origen");
        if (o != null) mostrarAeropuerto(o);
    }

    private void mostrarAeropuerto(String codigo) {
        Aeropuerto a = grafo.getAeropuerto(codigo);
        List<Ruta> salientes = grafo.getRutasSalientes(codigo);
        StringBuilder sb = new StringBuilder("AEROPUERTO ").append(a.getCodigo()).append("\n");
        sb.append(a.getCiudad()).append(", ").append(a.getPais())
          .append("  ·  posición (").append(a.getX()).append(", ").append(a.getY()).append(")\n\n");
        sb.append("Rutas salientes (").append(salientes.size()).append("):\n");
        if (salientes.isEmpty()) sb.append("  (ninguna)\n");
        for (Ruta r : salientes) sb.append("  ").append(r).append("\n");
        txtResultado.setText(sb.toString());
    }

    // ======================= Alcanzables (4.4) =======================

    @FXML
    private void onAlcanzables() {
        String o = validar(cbOrigen, "origen");
        if (o == null) return;
        animador.detener();
        mapa.limpiarResaltado();

        mostrarTotales(null);
        Set<String> alc = DFS.alcanzables(grafo, o);
        List<String> no = DFS.noAlcanzables(grafo, alc);
        List<String> noLlegan = DFS.noAlcanzables(grafo, DFS.quienesLleganA(grafo, o));
        mapa.colorearAlcanzables(o, alc);

        alc.remove(o);   // el origen no se cuenta como destino
        txtResultado.setText("ALCANZABILIDAD DESDE " + o + " (DFS)\n\n"
                + "Alcanzables (" + alc.size() + "):\n" + envolver(alc) + "\n\n"
                + "NO alcanzables (" + no.size() + "):\n" + (no.isEmpty() ? "(ninguno)" : envolver(no)) + "\n\n"
                + "Aeropuertos desde los que NO se puede llegar a " + o + " (" + noLlegan.size() + "):\n"
                + (noLlegan.isEmpty() ? "(ninguno)" : envolver(noLlegan)));
    }

    /** Lista de códigos en renglones de 8 para que se lea bien. */
    private String envolver(Collection<String> codigos) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String c : codigos) {
            sb.append(c).append(++i % 8 == 0 ? "\n" : "  ");
        }
        return sb.toString().trim();
    }

    // ======================= Cancelar / restaurar (4.5) =======================

    @FXML
    private void onCancelarRuta() {
        String o = validar(cbOrigen, "origen");
        if (o == null) return;
        String d = validar(cbDestino, "destino");
        if (d == null) return;

        Ruta r = grafo.cancelarRuta(o, d);
        if (r == null) {
            txtResultado.setText("No existe una ruta directa activa de " + o + " a " + d + ".");
            return;
        }
        animador.detener();
        mostrarTotales(null);
        mapa.dibujar(grafo);
        refrescarMarcas();
        lblEstado.setText("Rutas activas: " + grafo.cantidadRutas() + "  ·  canceladas: " + grafo.getCanceladas().size());
        txtResultado.setText("Ruta cancelada: " + r + "\n\nLas siguientes consultas usan el grafo modificado.");
    }

    @FXML
    private void onRestaurar() {
        if (grafo == null) return;
        int n = grafo.getCanceladas().size();
        grafo.restaurarRutas();
        animador.detener();
        mapa.dibujar(grafo);
        refrescarMarcas();
        lblEstado.setText("Rutas activas: " + grafo.cantidadRutas() + "  ·  canceladas: 0");
        txtResultado.setText("Se restauraron " + n + " ruta(s).");
    }

    /** Rutas cuya cancelación deja algún aeropuerto inalcanzable desde el origen (pregunta 4). */
    @FXML
    private void onRutasCriticas() {
        String o = validar(cbOrigen, "origen");
        if (o == null) return;
        Map<Ruta, List<String>> crit = DFS.rutasCriticas(grafo, o);
        StringBuilder sb = new StringBuilder("RUTAS CRÍTICAS DESDE " + o + "\n");
        sb.append("(si se cancela una sola de ellas, algún aeropuerto deja de ser alcanzable)\n\n");
        if (crit.isEmpty()) sb.append("Ninguna: cancelar cualquier ruta individual no desconecta nada.");
        for (Map.Entry<Ruta, List<String>> e : crit.entrySet()) {
            sb.append(e.getKey().getOrigen()).append(" → ").append(e.getKey().getDestino())
              .append("   se pierde: ").append(String.join(", ", e.getValue())).append("\n");
        }
        txtResultado.setText(sb.toString());
    }

    // ======================= Animación =======================

    @FXML
    private void onPausa() {
        boolean pausado = animador.alternarPausa();
        btnPausa.setText(pausado ? "Reanudar" : "Pausar");
    }

    @FXML
    private void onReiniciar() {
        animador.reiniciar();
        btnPausa.setText("Pausar");
    }
}

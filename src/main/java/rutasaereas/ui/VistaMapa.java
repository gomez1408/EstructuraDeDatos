package rutasaereas.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.QuadCurve;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;
import rutasaereas.modelo.Aeropuerto;
import rutasaereas.modelo.Grafo;
import rutasaereas.modelo.ResultadoRuta;
import rutasaereas.modelo.Ruta;

/**
 * El mapa: un panel de 1000 x 1000 (las coordenadas x, y del archivo se usan tal cual).
 *
 * Se dibuja en capas (Group) para que unas cosas queden encima de otras:
 *   imagen de fondo -> rutas -> ruta resaltada -> estela -> aeropuertos -> avión
 *
 * Las rutas se dibujan como CURVAS (Bézier cuadrática) en vez de rectas: así la
 * ruta A->B y la ruta B->A no quedan una encima de la otra (cada una se curva
 * hacia su derecha). El avión sigue esa misma curva (ver puntoEnRuta).
 *
 * La imagen de fondo (silueta de América) es solo decoración: las coordenadas de
 * los aeropuertos SIEMPRE salen del archivo. Si se cargan otros archivos donde no
 * calce, se puede ocultar con setFondoVisible(false).
 */
public class VistaMapa extends Pane {

    public static final int TAM = 1000;

    // Paleta (también se usa en la leyenda del panel)
    static final Color COLOR_RUTA = Color.web("#7fb4e6", 0.28);
    static final Color COLOR_CANCELADA = Color.web("#ff5d73");
    static final Color COLOR_RESALTADA = Color.web("#ffb703");
    static final Color COLOR_AEROPUERTO = Color.web("#9be7ff");
    static final Color COLOR_ORIGEN = Color.web("#38bdf8");
    static final Color COLOR_DESTINO = Color.web("#f472b6");
    static final Color COLOR_ALCANZABLE = Color.web("#4ade80");
    static final Color COLOR_NO_ALCANZABLE = Color.web("#f87171");

    private final ImageView fondo = new ImageView();
    private final Group capaRutas = new Group();
    private final Group capaResaltado = new Group();
    private final Group capaAeropuertos = new Group();
    private final Group capaAvion = new Group();

    private final Map<String, Circle> circulos = new HashMap<>();
    private final Map<String, Circle> anillos = new HashMap<>();   // aro que "late" en origen/destino
    private final SVGPath avion = crearAvion();
    private final Polyline estela = new Polyline();

    private Grafo grafo;
    private Consumer<String> alHacerClic = c -> { };

    public VistaMapa() {
        setPrefSize(TAM, TAM);
        setMinSize(TAM, TAM);
        setMaxSize(TAM, TAM);
        getStyleClass().add("mapa");
        setClip(new Rectangle(TAM, TAM));

        // Imagen de fondo (está en resources/rutasaereas/ui/img/)
        var url = VistaMapa.class.getResource("img/fondo_america.png");
        if (url != null) {
            fondo.setImage(new Image(url.toExternalForm()));
            fondo.setFitWidth(TAM);
            fondo.setFitHeight(TAM);
            fondo.setSmooth(true);
        }

        // Estela que deja el avión
        estela.setStroke(Color.web("#ffffff", 0.75));
        estela.setStrokeWidth(2);
        estela.getStrokeDashArray().addAll(2.0, 6.0);
        estela.setStrokeLineCap(StrokeLineCap.ROUND);
        estela.setMouseTransparent(true);

        capaAvion.getChildren().addAll(estela, avion);
        avion.setVisible(false);
        getChildren().addAll(fondo, capaRutas, capaResaltado, capaAeropuertos, capaAvion);
    }

    public void setAlHacerClic(Consumer<String> accion) { this.alHacerClic = accion; }
    public SVGPath getAvion()   { return avion; }
    public Polyline getEstela() { return estela; }
    public void setFondoVisible(boolean v) { fondo.setVisible(v); }

    // ======================= Geometría de las curvas =======================

    /**
     * Punto de control de la curva A->B: el punto medio desplazado hacia la
     * derecha del sentido de vuelo (perpendicular al segmento).
     */
    private Point2D control(Aeropuerto a, Aeropuerto b) {
        double dx = b.getX() - a.getX(), dy = b.getY() - a.getY();
        double largo = Math.hypot(dx, dy);
        if (largo == 0) return new Point2D(a.getX(), a.getY());
        double desv = Math.min(45, largo * 0.12);
        // (-dy, dx)/largo es perpendicular al segmento
        return new Point2D((a.getX() + b.getX()) / 2 - dy / largo * desv,
                           (a.getY() + b.getY()) / 2 + dx / largo * desv);
    }

    /**
     * Punto de la curva de la ruta para t entre 0 y 1 (Bézier cuadrática):
     *   B(t) = (1-t)^2 * A  +  2(1-t)t * C  +  t^2 * B
     * Con t = 0 da el origen y con t = 1 el destino. Lo usa el animador.
     */
    public Point2D puntoEnRuta(Ruta r, double t) {
        Aeropuerto a = grafo.getAeropuerto(r.getOrigen());
        Aeropuerto b = grafo.getAeropuerto(r.getDestino());
        Point2D c = control(a, b);
        double u = 1 - t;
        return new Point2D(u * u * a.getX() + 2 * u * t * c.getX() + t * t * b.getX(),
                           u * u * a.getY() + 2 * u * t * c.getY() + t * t * b.getY());
    }

    /** Ángulo (grados) de la tangente de la curva en t, para orientar el avión. Derivada de B(t). */
    public double anguloEnRuta(Ruta r, double t) {
        Aeropuerto a = grafo.getAeropuerto(r.getOrigen());
        Aeropuerto b = grafo.getAeropuerto(r.getDestino());
        Point2D c = control(a, b);
        double dx = 2 * (1 - t) * (c.getX() - a.getX()) + 2 * t * (b.getX() - c.getX());
        double dy = 2 * (1 - t) * (c.getY() - a.getY()) + 2 * t * (b.getY() - c.getY());
        return Math.toDegrees(Math.atan2(dy, dx));
    }

    private QuadCurve curva(Ruta r) {
        Aeropuerto a = grafo.getAeropuerto(r.getOrigen());
        Aeropuerto b = grafo.getAeropuerto(r.getDestino());
        Point2D c = control(a, b);
        QuadCurve q = new QuadCurve(a.getX(), a.getY(), c.getX(), c.getY(), b.getX(), b.getY());
        q.setFill(null);
        q.setMouseTransparent(true);
        return q;
    }

    // ======================= Dibujo del grafo =======================

    /** Dibuja todo de nuevo: rutas activas, rutas canceladas y aeropuertos. */
    public void dibujar(Grafo g) {
        this.grafo = g;
        capaRutas.getChildren().clear();
        capaResaltado.getChildren().clear();
        capaAeropuertos.getChildren().clear();
        circulos.clear();
        anillos.clear();
        limpiarEstela();

        // Rutas activas: curvas finas y translúcidas
        for (Ruta r : g.getTodasLasRutas()) {
            QuadCurve q = curva(r);
            q.setStroke(COLOR_RUTA);
            q.setStrokeWidth(1.1);
            capaRutas.getChildren().add(q);
        }

        // Rutas canceladas: rojas, punteadas y con flecha
        for (Ruta r : g.getCanceladas()) {
            QuadCurve q = curva(r);
            q.setStroke(COLOR_CANCELADA);
            q.setStrokeWidth(2.2);
            q.getStrokeDashArray().addAll(9.0, 7.0);
            q.setEffect(new DropShadow(8, COLOR_CANCELADA));
            capaRutas.getChildren().addAll(q, flecha(r, COLOR_CANCELADA, 10));
        }

        // Aeropuertos: punto brillante + aro (oculto) + código
        Font fuente = Font.font("Segoe UI", FontWeight.BOLD, 12);
        for (Aeropuerto a : g.getAeropuertos()) {
            Circle anillo = new Circle(a.getX(), a.getY(), 14);
            anillo.setFill(null);
            anillo.setStrokeWidth(2);
            anillo.setVisible(false);
            anillo.setMouseTransparent(true);

            Circle c = new Circle(a.getX(), a.getY(), 5.5, COLOR_AEROPUERTO);
            c.setStroke(Color.web("#0b1726"));
            c.setStrokeWidth(1.5);
            c.setEffect(new DropShadow(10, COLOR_AEROPUERTO));
            c.setCursor(Cursor.HAND);
            Tooltip t = new Tooltip(a.getCodigo() + " · " + a.getCiudad() + ", " + a.getPais());
            t.setShowDelay(Duration.millis(150));
            Tooltip.install(c, t);
            c.setOnMouseClicked(e -> alHacerClic.accept(a.getCodigo()));
            c.setOnMouseEntered(e -> c.setEffect(new Glow(0.9)));
            c.setOnMouseExited(e -> c.setEffect(new DropShadow(10, (Color) c.getFill())));

            Text cod = new Text(a.getX() + 8, a.getY() - 7, a.getCodigo());
            cod.setFont(fuente);
            cod.setFill(Color.web("#e6f1ff"));
            cod.setEffect(new DropShadow(3, Color.web("#020814")));
            cod.setMouseTransparent(true);

            circulos.put(a.getCodigo(), c);
            anillos.put(a.getCodigo(), anillo);
            capaAeropuertos.getChildren().addAll(anillo, c, cod);
        }
        animarAnillos();
    }

    /** Los aros de origen/destino crecen y se desvanecen en bucle ("pulso"). */
    private void animarAnillos() {
        for (Circle anillo : anillos.values()) {
            ScaleTransition s = new ScaleTransition(Duration.seconds(1.4), anillo);
            s.setFromX(0.6); s.setFromY(0.6); s.setToX(1.6); s.setToY(1.6);
            FadeTransition f = new FadeTransition(Duration.seconds(1.4), anillo);
            f.setFromValue(0.9); f.setToValue(0.0);
            ParallelTransition p = new ParallelTransition(s, f);
            p.setCycleCount(Animation.INDEFINITE);
            p.play();
        }
    }

    // ======================= Resaltados =======================

    /** Pinta la ruta encontrada: curvas gruesas doradas con brillo y flechas. */
    public void resaltarRuta(ResultadoRuta res) {
        capaResaltado.getChildren().clear();
        if (res == null) return;
        for (Ruta r : res.getTramos()) {
            QuadCurve halo = curva(r);
            halo.setStroke(Color.web("#ffb703", 0.25));
            halo.setStrokeWidth(12);
            halo.setStrokeLineCap(StrokeLineCap.ROUND);

            QuadCurve q = curva(r);
            q.setStroke(COLOR_RESALTADA);
            q.setStrokeWidth(3.5);
            q.setStrokeLineCap(StrokeLineCap.ROUND);
            q.setEffect(new DropShadow(12, COLOR_RESALTADA));
            capaResaltado.getChildren().addAll(halo, q, flecha(r, COLOR_RESALTADA, 13));
        }
    }

    public void limpiarResaltado() {
        capaResaltado.getChildren().clear();
        limpiarEstela();
    }

    public void limpiarEstela() {
        estela.getPoints().clear();
    }

    /** Vuelve todos los aeropuertos a su color normal y marca origen/destino. */
    public void marcarOrigenDestino(String origen, String destino) {
        for (Map.Entry<String, Circle> e : circulos.entrySet()) {
            String cod = e.getKey();
            Color color = cod.equals(origen) ? COLOR_ORIGEN
                        : cod.equals(destino) ? COLOR_DESTINO : COLOR_AEROPUERTO;
            boolean marcado = cod.equals(origen) || cod.equals(destino);
            pintar(cod, color, marcado ? 8 : 5.5, marcado);
        }
    }

    /** Verde = alcanzable desde el origen, rojo = no alcanzable. */
    public void colorearAlcanzables(String origen, Set<String> alcanzables) {
        for (String cod : circulos.keySet()) {
            if (cod.equals(origen)) pintar(cod, COLOR_ORIGEN, 8, true);
            else pintar(cod, alcanzables.contains(cod) ? COLOR_ALCANZABLE : COLOR_NO_ALCANZABLE, 6.5, false);
        }
    }

    private void pintar(String cod, Color color, double radio, boolean conAnillo) {
        Circle c = circulos.get(cod);
        c.setFill(color);
        c.setRadius(radio);
        c.setEffect(new DropShadow(conAnillo ? 18 : 10, color));
        Circle anillo = anillos.get(cod);
        anillo.setStroke(color);
        anillo.setVisible(conAnillo);
    }

    // ======================= Ayudantes de dibujo =======================

    /** Triángulo sobre la mitad de la curva, apuntando en el sentido del vuelo. */
    private Polygon flecha(Ruta r, Color color, double tam) {
        Point2D p = puntoEnRuta(r, 0.5);
        Polygon f = new Polygon(tam, 0.0, -tam * 0.6, -tam * 0.6, -tam * 0.6, tam * 0.6);
        f.setFill(color);
        f.setLayoutX(p.getX());
        f.setLayoutY(p.getY());
        f.setRotate(anguloEnRuta(r, 0.5));
        f.setMouseTransparent(true);
        return f;
    }

    /** Silueta de avión (apunta a la derecha, ángulo 0) centrada en (0,0). */
    private static SVGPath crearAvion() {
        SVGPath p = new SVGPath();
        p.setContent("M 20 0 C 20 -2 17 -3 14 -3 L 4 -3 L -4 -16 L -8 -16 L -3 -3 "
                + "L -11 -3 L -15 -8 L -18 -8 L -15 0 L -18 8 L -15 8 L -11 3 "
                + "L -3 3 L -8 16 L -4 16 L 4 3 L 14 3 C 17 3 20 2 20 0 Z");
        p.setFill(Color.WHITE);
        p.setStroke(Color.web("#0b1726"));
        p.setStrokeWidth(1);
        p.setEffect(new DropShadow(14, Color.web("#7dd3fc")));
        p.setMouseTransparent(true);
        return p;
    }
}

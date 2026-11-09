package rutasaereas.algoritmos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import rutasaereas.modelo.Grafo;
import rutasaereas.modelo.Ruta;

// Recorridos en profundidad (DFS) sobre el grafo de rutas aéreas.
 
public class DFS {

    /** Aeropuertos alcanzables desde el origen (incluye al origen). O(n + m). */
    public static Set<String> alcanzables(Grafo g, String origen) {
        Set<String> visitados = new LinkedHashSet<>();
        Deque<String> pila = new ArrayDeque<>();
        pila.push(origen);

        while (!pila.isEmpty()) {
            String actual = pila.pop();
            if (!visitados.add(actual)) continue;   // ya visitado: se descarta
            for (Ruta r : g.getRutasSalientes(actual)) {
                if (!visitados.contains(r.getDestino())) {
                    pila.push(r.getDestino());
                }
            }
        }
        return visitados;
    }

    //Los aeropuertos que no están en el conjunto de alcanzables. O(n). 
    public static List<String> noAlcanzables(Grafo g, Set<String> alcanzables) {
        List<String> faltan = new ArrayList<>();
        for (String c : g.getCodigos()) if (!alcanzables.contains(c)) faltan.add(c);
        return faltan;
    }

    /**
     * Aeropuertos desde los que se puede llegar al destino. Se construye el
     * grafo INVERTIDO (cada ruta A->B pasa a B->A) y se hace DFS desde el
     * destino. O(n + m).
     */
    public static Set<String> quienesLleganA(Grafo g, String destino) {
        Map<String, List<String>> invertido = new HashMap<>();
        for (String c : g.getCodigos()) invertido.put(c, new ArrayList<>());
        for (Ruta r : g.getTodasLasRutas()) invertido.get(r.getDestino()).add(r.getOrigen());

        Set<String> visitados = new LinkedHashSet<>();
        Deque<String> pila = new ArrayDeque<>();
        pila.push(destino);

        while (!pila.isEmpty()) {
            String actual = pila.pop();
            if (!visitados.add(actual)) continue;
            for (String previo : invertido.get(actual)) {
                if (!visitados.contains(previo)) pila.push(previo);
            }
        }
        return visitados;
    }

    /**
     * Rutas cuya cancelación deja aeropuertos inalcanzables desde el origen.
     * Se prueba cancelar cada ruta (una a la vez), se repite el DFS y se
     * restaura. O(m * (n + m)).
     */
    public static Map<Ruta, List<String>> rutasCriticas(Grafo g, String origen) {
        Set<String> base = alcanzables(g, origen);
        Map<Ruta, List<String>> criticas = new LinkedHashMap<>();

        for (Ruta r : g.getTodasLasRutas()) {
            // Solo pueden ser críticas las rutas que salen de un aeropuerto alcanzable
            if (!base.contains(r.getOrigen())) continue;
            Ruta cancelada = g.cancelarRuta(r.getOrigen(), r.getDestino());
            Set<String> ahora = alcanzables(g, origen);
            g.restaurarRuta(cancelada);

            if (ahora.size() < base.size()) {
                List<String> perdidos = new ArrayList<>();
                for (String c : base) if (!ahora.contains(c)) perdidos.add(c);
                criticas.put(r, perdidos);
            }
        }
        return criticas;
    }
}
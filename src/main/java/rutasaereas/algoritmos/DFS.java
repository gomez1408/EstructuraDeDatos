/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rutasaereas.algoritmos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import rutasaereas.modelo.Grafo;
import rutasaereas.modelo.Ruta;
/**
 *
 * @author gomez
 */
public class DFS {
     /** Aeropuertos alcanzables desde el origen (incluye al origen). */
    public static Set<String> alcanzables(Grafo g, String origen) {
        Set<String> visitados = new LinkedHashSet<>();
        visitar(g, origen, visitados);
        return visitados;
    }

    private static void visitar(Grafo g, String actual, Set<String> visitados) {
        visitados.add(actual);
        for (Ruta r : g.getRutasSalientes(actual)) {
            if (!visitados.contains(r.getDestino())) {
                visitar(g, r.getDestino(), visitados);
            }
        }
    }

    /** Los aeropuertos que NO están en el conjunto de alcanzables. O(n). */
    public static List<String> noAlcanzables(Grafo g, Set<String> alcanzables) {
        List<String> faltan = new ArrayList<>();
        for (String c : g.getCodigos()) if (!alcanzables.contains(c)) faltan.add(c);
        return faltan;
    }

    
    public static Set<String> quienesLleganA(Grafo g, String destino) {
        Map<String, List<String>> invertido = new HashMap<>();
        for (String c : g.getCodigos()) invertido.put(c, new ArrayList<>());
        for (Ruta r : g.getTodasLasRutas()) invertido.get(r.getDestino()).add(r.getOrigen());

        Set<String> visitados = new LinkedHashSet<>();
        visitarInvertido(invertido, destino, visitados);
        return visitados;
    }

    private static void visitarInvertido(Map<String, List<String>> inv, String actual, Set<String> visitados) {
        visitados.add(actual);
        for (String previo : inv.get(actual)) {
            if (!visitados.contains(previo)) visitarInvertido(inv, previo, visitados);
        }
    }


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

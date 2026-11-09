package rutasaereas.algoritmos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import rutasaereas.modelo.Criterio;
import rutasaereas.modelo.Grafo;
import rutasaereas.modelo.ResultadoRuta;
import rutasaereas.modelo.Ruta;

public class Dijkstra {
     // Elemento de la cola de prioridad: un aeropuerto con su distancia tentativa.
    private record Entrada(String codigo, int distancia) { }

    // Devuelve la ruta óptima según el criterio, o null si no existe ninguna ruta. 
    public static ResultadoRuta rutaOptima(Grafo g, String origen, String destino, Criterio criterio) {
        Map<String, Integer> dist = new HashMap<>();
        Map<String, Ruta> anterior = new HashMap<>();
        Set<String> visitados = new HashSet<>();

        // La cola ordena por distancia: siempre sale primero la menor
        PriorityQueue<Entrada> cola =
                new PriorityQueue<>((a, b) -> Integer.compare(a.distancia(), b.distancia()));

        dist.put(origen, 0);
        cola.add(new Entrada(origen, 0));

        while (!cola.isEmpty()) {
            Entrada e = cola.poll();
            String actual = e.codigo();

            // Puede haber entradas viejas (con distancia peor) del mismo aeropuerto: se ignoran
            if (visitados.contains(actual)) continue;
            visitados.add(actual);

            if (actual.equals(destino)) {   // su distancia ya es definitiva
                return ResultadoRuta.reconstruir(anterior, origen, destino);
            }

            for (Ruta r : g.getRutasSalientes(actual)) {
                String vecino = r.getDestino();
                if (visitados.contains(vecino)) continue;
                int nueva = dist.get(actual) + r.getPeso(criterio);   // <- aquí entra el criterio
                if (nueva < dist.getOrDefault(vecino, Integer.MAX_VALUE)) {
                    dist.put(vecino, nueva);       // relajación
                    anterior.put(vecino, r);
                    cola.add(new Entrada(vecino, nueva));
                }
            }
        }
        return null;  // el destino nunca se alcanzó
    }
}

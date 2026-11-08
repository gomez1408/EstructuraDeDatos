/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rutasaereas.algoritmos;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import rutasaereas.modelo.Grafo;
import rutasaereas.modelo.ResultadoRuta;
import rutasaereas.modelo.Ruta;
/**
 *
 * @author gomez
 */
public class BFS {
     public static ResultadoRuta menosEscalas(Grafo g, String origen, String destino) {
        Queue<String> cola = new ArrayDeque<>();
        Set<String> visitados = new HashSet<>();
        Map<String, Ruta> anterior = new HashMap<>();   // por cuál ruta llegué a cada aeropuerto

        cola.add(origen);
        visitados.add(origen);

        while (!cola.isEmpty()) {
            String actual = cola.poll();
            for (Ruta r : g.getRutasSalientes(actual)) {
                String vecino = r.getDestino();
                if (!visitados.contains(vecino)) {
                    visitados.add(vecino);          // se marca al ENCOLAR para no meterlo dos veces
                    anterior.put(vecino, r);
                    if (vecino.equals(destino)) {   // primera vez que lo vemos = menos vuelos
                        return ResultadoRuta.reconstruir(anterior, origen, destino);
                    }
                    cola.add(vecino);
                }
            }
        }
        return null;  // la cola se vació sin encontrar el destino: no hay ruta
    }
}

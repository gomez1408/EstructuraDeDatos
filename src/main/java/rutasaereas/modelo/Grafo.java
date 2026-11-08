/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rutasaereas.modelo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 *
 * @author gomez
 */
public class Grafo {
    // LinkedHashMap conserva el orden del archivo (listados estables y predecibles)
    private final Map<String, Aeropuerto> aeropuertos = new LinkedHashMap<>();
    private final Map<String, List<Ruta>> adyacencia = new HashMap<>();

    // Rutas canceladas: salen del grafo pero se guardan aquí para poder
    // restaurarlas y para dibujarlas punteadas en el mapa. El archivo no se toca.
    private final List<Ruta> canceladas = new ArrayList<>();

    public void agregarAeropuerto(Aeropuerto a) {
        aeropuertos.put(a.getCodigo(), a);
        adyacencia.putIfAbsent(a.getCodigo(), new ArrayList<>());
    }

    /** Agrega la arista origen->destino. Devuelve false si algún código no existe. O(1). */
    public boolean agregarRuta(Ruta r) {
        if (!existe(r.getOrigen()) || !existe(r.getDestino())) return false;
        adyacencia.get(r.getOrigen()).add(r);
        return true;
    }

    // ======================= Consultas =======================

    /** O(1) promedio gracias al HashMap. */
    public boolean existe(String codigo) {
        return codigo != null && aeropuertos.containsKey(codigo);
    }

    public Aeropuerto getAeropuerto(String codigo) {
        return aeropuertos.get(codigo);
    }

    public Collection<Aeropuerto> getAeropuertos() {
        return aeropuertos.values();
    }

    public List<String> getCodigos() {
        return new ArrayList<>(aeropuertos.keySet());
    }

    /** Rutas salientes (activas) de un aeropuerto. Lista vacía si no existe. */
    public List<Ruta> getRutasSalientes(String codigo) {
        List<Ruta> lista = adyacencia.get(codigo);
        return lista == null ? new ArrayList<>() : lista;
    }

    public List<Ruta> getTodasLasRutas() {
        List<Ruta> todas = new ArrayList<>();
        for (String c : aeropuertos.keySet()) todas.addAll(adyacencia.get(c));
        return todas;
    }

    public int cantidadAeropuertos() { return aeropuertos.size(); }

    public int cantidadRutas() {
        int m = 0;
        for (List<Ruta> l : adyacencia.values()) m += l.size();
        return m;
    }

    public Ruta cancelarRuta(String origen, String destino) {
        List<Ruta> salientes = adyacencia.get(origen);
        if (salientes == null) return null;
        for (int i = 0; i < salientes.size(); i++) {
            Ruta r = salientes.get(i);
            if (r.getDestino().equals(destino)) {
                salientes.remove(i);
                canceladas.add(r);
                return r;
            }
        }
        return null;
    }

    /** Devuelve UNA ruta cancelada al grafo (lo usa el análisis de rutas críticas). */
    public void restaurarRuta(Ruta r) {
        if (canceladas.remove(r)) adyacencia.get(r.getOrigen()).add(r);
    }

    public void restaurarRutas() {
        for (Ruta r : canceladas) adyacencia.get(r.getOrigen()).add(r);
        canceladas.clear();
    }

    public List<Ruta> getCanceladas() {
        return canceladas;
    }
}

package rutasaereas.modelo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;


public class ResultadoRuta {
   private final List<Ruta> tramos;
    private final int minutos;
    private final int costo;

    public ResultadoRuta(List<Ruta> tramos) {
        this.tramos = tramos;
        int min = 0, cos = 0;
        for (Ruta r : tramos) {
            min += r.getMinutos();
            cos += r.getCosto();
        }
        this.minutos = min;
        this.costo = cos;
    }

    public static ResultadoRuta reconstruir(Map<String, Ruta> anterior, String origen, String destino) {
        Deque<Ruta> pila = new ArrayDeque<>();
        String actual = destino;
        while (!actual.equals(origen)) {
            Ruta r = anterior.get(actual);
            pila.push(r);
            actual = r.getOrigen();
        }
        List<Ruta> tramos = new ArrayList<>();
        while (!pila.isEmpty()) tramos.add(pila.pop());
        return new ResultadoRuta(tramos);
    }

    public List<Ruta> getTramos() { return tramos; }
    public int getVuelos()        { return tramos.size(); }
    public int getMinutos()       { return minutos; }
    public int getCosto()         { return costo; }

    //Secuencia de códigos, por ejemplo "SJO → MIA → JFK". 
    public String getSecuencia() {
        if (tramos.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(tramos.get(0).getOrigen());
        for (Ruta r : tramos) sb.append(" → ").append(r.getDestino());
        return sb.toString();
    }

    //Minutos en formato legible, por ejemplo "365 min (6 h 05 min)". 
    public static String formatoMinutos(int minutos) {
        return String.format("%d min (%d h %02d min)", minutos, minutos / 60, minutos % 60);
    }
}
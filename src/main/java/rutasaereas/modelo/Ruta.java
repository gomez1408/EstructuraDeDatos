/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rutasaereas.modelo;

/**
 *
 * @author gomez
 */
public class Ruta {
    
    private final String origen;
    private final String destino;
    private final int minutos;
    private final int costo;

    public Ruta(String origen, String destino, int minutos, int costo) {
        this.origen = origen;
        this.destino = destino;
        this.minutos = minutos;
        this.costo = costo;
    }

    public String getOrigen()  { return origen; }
    public String getDestino() { return destino; }
    public int getMinutos()    { return minutos; }
    public int getCosto()      { return costo; }

    public int getPeso(Criterio criterio) {
        return criterio == Criterio.MINUTOS ? minutos : costo;
    }

    @Override
    public String toString() {
        return origen + " -> " + destino + " | " + minutos + " min | $" + costo;
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rutasaereas.modelo;

/**
 *
 * @author gomez
 */
public class Aeropuerto {
   
        private final String codigo;
    private final int x;
    private final int y;
    private final String ciudad;
    private final String pais;

    public Aeropuerto(String codigo, int x, int y, String ciudad, String pais) {
        this.codigo = codigo;
        this.x = x;
        this.y = y;
        this.ciudad = ciudad;
        this.pais = pais;
    }

    public String getCodigo() { return codigo; }
    public int getX()         { return x; }
    public int getY()         { return y; }

    /** En el archivo los espacios vienen como "_"; aquí los devolvemos con espacios. */
    public String getCiudad() { return ciudad.replace('_', ' '); }
    public String getPais()   { return pais.replace('_', ' '); }

    @Override
    public String toString() {
        return codigo + " - " + getCiudad() + ", " + getPais() + " (" + x + ", " + y + ")";
    }
    
}

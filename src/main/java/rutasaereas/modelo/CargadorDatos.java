/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rutasaereas.modelo;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Scanner;
/**
 *
 * @author gomez
 */
public class CargadorDatos {
     public static Grafo cargar(File archAeropuertos, File archRutas) throws IOException {
        Grafo g = new Grafo();

        // 1) Vértices
        try (Scanner sc = new Scanner(archAeropuertos, "UTF-8")) {
            int n = sc.nextInt();
            for (int i = 0; i < n; i++) {
                String codigo = sc.next().toUpperCase();
                int x = sc.nextInt();
                int y = sc.nextInt();
                String ciudad = sc.next();
                String pais = sc.next();
                g.agregarAeropuerto(new Aeropuerto(codigo, x, y, ciudad, pais));
            }
        } catch (NoSuchElementException e) {   // incluye InputMismatchException
            throw new IOException("Formato inválido en " + archAeropuertos.getName(), e);
        }

        // 2) Aristas
        int ignoradas = 0;
        try (Scanner sc = new Scanner(archRutas, "UTF-8")) {
            int m = sc.nextInt();
            for (int i = 0; i < m; i++) {
                String origen = sc.next().toUpperCase();
                String destino = sc.next().toUpperCase();
                int minutos = sc.nextInt();
                int costo = sc.nextInt();
                if (!g.agregarRuta(new Ruta(origen, destino, minutos, costo))) ignoradas++;
            }
        } catch (NoSuchElementException e) {
            throw new IOException("Formato inválido en " + archRutas.getName(), e);
        }
        if (ignoradas > 0) {
            System.err.println("Aviso: " + ignoradas + " ruta(s) con códigos desconocidos fueron ignoradas.");
        }
        return g;
    }
    
}

# Rutas Aéreas — EIF207 Estructuras de Datos, Proyecto II

Planificador de vuelos entre aeropuertos de América. Cada aeropuerto (código IATA) es un
vértice de un **grafo dirigido y ponderado**; cada ruta es una arista con dos pesos:
duración (minutos) y costo (dólares).

**Integrantes:** 

Alisson Yulieth Cascante Chaves
Andrey Gomez 

## Tecnologías

| Elemento | Detalle |
|---|---|
| Lenguaje | Java 17 |
| Entorno | Apache NetBeans 17 |
| Biblioteca gráfica | JavaFX 21.0.5 (controles y FXML) |
| Construcción | Maven (`pom.xml`) |

No se usan bibliotecas de grafos: BFS, DFS y Dijkstra están escritos a mano.

## Cómo compilar y ejecutar

1. Instalar un **JDK 17 o superior** (por ejemplo Temurin 17). JavaFX no se instala aparte:
   Maven lo descarga la primera vez (se necesita internet).
2. **NetBeans:** abrir la carpeta del proyecto (la que contiene `pom.xml`), en
   *Properties → Build → Compile* elegir el JDK 17 como *Java Platform*, y presionar **Run**.
3. **Terminal:** dentro de la carpeta del proyecto, `mvn clean javafx:run`.

El programa busca `aeropuertos.txt` y `rutas.txt` en la carpeta desde donde se ejecuta.
Con el botón **«Cargar archivos…»** se pueden elegir otros archivos del mismo formato.

## Formato de los datos

- `aeropuertos.txt`: primera línea = cantidad; luego `codigo x y ciudad pais`
  (x, y = posición para una ventana de 1000×1000; los espacios de los nombres son `_`).
- `rutas.txt`: primera línea = cantidad; luego `origen destino minutos costo`.
  Cada línea es una ruta **dirigida**.

Se rechazan archivos con aeropuertos duplicados o con pesos negativos (Dijkstra exige
pesos no negativos). Las rutas con códigos desconocidos se ignoran.

## Funcionalidades

- Mapa con aeropuertos (código) y rutas; selección de origen y destino por lista o por clic.
- **Menos escalas** (BFS), **más rápida** y **más barata** (un mismo Dijkstra parametrizado por criterio).
- Panel con la secuencia, la cantidad de vuelos, los minutos y el costo.
- Animación del vuelo tramo por tramo, con pausa y reinicio.
- **Alcanzables** desde un origen (DFS), diferenciados por color.
- **Rutas críticas**: rutas cuya cancelación deja aeropuertos inalcanzables.
- **Cancelar ruta** (solo en memoria; el archivo no se toca) y **Restaurar todas**.
- Casos especiales: código inexistente, origen igual al destino y destino sin ruta.

## Estructura del código

```
src/main/java/rutasaereas/
├── App.java                  Arranque de la aplicación JavaFX
├── modelo/                   Aeropuerto, Ruta, Criterio, Grafo (lista de adyacencia),
│                             CargadorDatos, ResultadoRuta
├── algoritmos/               BFS, DFS (iterativo), Dijkstra
└── ui/                       MapaController, VistaMapa, AnimadorVuelo
src/main/resources/rutasaereas/ui/   mapa.fxml, estilos.css, imágenes
```

## Estructuras usadas

- `Map<String, List<Ruta>>` como lista de adyacencia (búsqueda de un aeropuerto en O(1) promedio).
- `ArrayDeque` como cola (BFS) y como pila (DFS iterativo).
- `PriorityQueue` en Dijkstra.
- `HashMap` / `HashSet` para distancias, rutas previas y visitados.

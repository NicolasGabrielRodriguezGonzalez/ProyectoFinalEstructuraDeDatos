package co.edu.uniquindio.poo.estructuras;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Directed weighted graph using an adjacency list stored in a HashMap.
 * Each vertex maps to the list of its outgoing edges.
 *
 * V = number of vertices, E = number of edges.
 * Complexity: agregarVertice O(1) average, agregarArista O(outdegree) because
 * parallel edges are merged, bfs and dfs O(V + E), hayCiclo O(V + E).
 */
public class Grafo<T> {

    /** Outgoing edge: destination vertex and weight. */
    public static class Arista<T> {
        private final T destino;
        private double peso;

        Arista(T destino, double peso) {
            this.destino = destino;
            this.peso = peso;
        }

        public T getDestino() {
            return destino;
        }

        public double getPeso() {
            return peso;
        }

        @Override
        public String toString() {
            return "-> " + destino + " (" + peso + ")";
        }
    }

    private enum Color { WHITE, GRAY, BLACK }

    private final Map<T, List<Arista<T>>> adyacencia = new HashMap<>();

    /** Adds a vertex. If it already exists, nothing changes. */
    public void agregarVertice(T vertice) {
        if (vertice == null) throw new IllegalArgumentException("The vertex cannot be null");
        adyacencia.putIfAbsent(vertice, new ArrayList<>());
    }

    /**
     * Adds a directed edge origen -> destino with the given weight.
     * If that edge already exists, its weight is accumulated (useful for total transferred amounts).
     */
    public void agregarArista(T origen, T destino, double peso) {
        validarExiste(origen);
        validarExiste(destino);
        List<Arista<T>> aristas = adyacencia.get(origen);
        for (Arista<T> a : aristas) {
            if (a.destino.equals(destino)) {
                a.peso += peso;
                return;
            }
        }
        aristas.add(new Arista<>(destino, peso));
    }

    public boolean contieneVertice(T vertice) {
        return adyacencia.containsKey(vertice);
    }

    public List<Arista<T>> getAristas(T vertice) {
        validarExiste(vertice);
        return Collections.unmodifiableList(adyacencia.get(vertice));
    }

    public Set<T> getVertices() {
        return Collections.unmodifiableSet(adyacencia.keySet());
    }

    /** Breadth-first search from inicio. Returns the visit order. Uses the Cola class as the queue. */
    public List<T> bfs(T inicio) {
        validarExiste(inicio);
        List<T> orden = new ArrayList<>();
        Set<T> visitados = new HashSet<>();
        Cola<T> cola = new Cola<>();

        visitados.add(inicio);
        cola.enqueue(inicio);
        while (!cola.isEmpty()) {
            T actual = cola.dequeue();
            orden.add(actual);
            for (Arista<T> a : adyacencia.get(actual)) {
                if (visitados.add(a.destino)) {   // add returns false if already visited
                    cola.enqueue(a.destino);
                }
            }
        }
        return orden;
    }

    /** Depth-first search from inicio (recursive). Returns the visit order. */
    public List<T> dfs(T inicio) {
        validarExiste(inicio);
        List<T> orden = new ArrayList<>();
        dfsRecursivo(inicio, new HashSet<>(), orden);
        return orden;
    }

    /** Detects whether the directed graph has a cycle, using three colors. */
    public boolean hayCiclo() {
        Map<T, Color> color = new HashMap<>();
        for (T v : adyacencia.keySet()) color.put(v, Color.WHITE);
        for (T v : adyacencia.keySet()) {
            if (color.get(v) == Color.WHITE && tieneCicloDesde(v, color)) return true;
        }
        return false;
    }

    // ---------------- helpers ----------------

    private void dfsRecursivo(T actual, Set<T> visitados, List<T> orden) {
        visitados.add(actual);
        orden.add(actual);
        for (Arista<T> a : adyacencia.get(actual)) {
            if (!visitados.contains(a.destino)) {
                dfsRecursivo(a.destino, visitados, orden);
            }
        }
    }

    /**
     * WHITE = not visited, GRAY = on the current DFS path, BLACK = finished.
     * An edge into a GRAY vertex is a back edge, which means a cycle.
     */
    private boolean tieneCicloDesde(T actual, Map<T, Color> color) {
        color.put(actual, Color.GRAY);
        for (Arista<T> a : adyacencia.get(actual)) {
            Color c = color.get(a.destino);
            if (c == Color.GRAY) return true;
            if (c == Color.WHITE && tieneCicloDesde(a.destino, color)) return true;
        }
        color.put(actual, Color.BLACK);
        return false;
    }

    private void validarExiste(T vertice) {
        if (!adyacencia.containsKey(vertice)) {
            throw new IllegalArgumentException("The vertex does not exist: " + vertice);
        }
    }
}
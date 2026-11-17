package co.edu.uniquindio.poo.servicios;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import co.edu.uniquindio.poo.estructuras.Grafo;
import co.edu.uniquindio.poo.modelo.Transaccion;

/**
 * PERSONA 3: análisis de patrones de gasto con grafos dirigidos.
 *  - grafoTransferencias: client -> client, weight = accumulated amount.
 *  - grafoMultimonedero:  wallet -> wallet (internal moves only), weight = accumulated amount.
 *  - grafoCategorias:     previous spending category -> next spending category of the same client,
 *                         weight = amount of the next spending.
 */
public class ServicioAnalisis {

    public static final String CAT_COMIDA = "Comida";
    public static final String CAT_SERVICIOS = "Servicios";
    public static final String CAT_AHORRO = "Ahorro";
    public static final String CAT_TRANSFERENCIA = "Transferencia";
    public static final String CAT_RETIRO = "Retiro";
    public static final String CAT_INGRESO = "Ingreso";

    /** Keywords matched as whole tokens against the transaction description (accent-insensitive). */
    private static final Map<String, List<String>> PALABRAS_CLAVE = new LinkedHashMap<>();
    static {
        PALABRAS_CLAVE.put(CAT_COMIDA, List.of("comida", "restaurante", "mercado", "almuerzo", "cena", "cafe"));
        PALABRAS_CLAVE.put(CAT_SERVICIOS, List.of("servicio", "servicios", "luz", "agua", "internet", "energia", "arriendo"));
        PALABRAS_CLAVE.put(CAT_AHORRO, List.of("ahorro", "ahorros"));
    }

    /** One registered spending: the transaction, its category and the category before it (for the edge). */
    private record Gasto(Transaccion transaccion, String categoria, String desde) { }

    private final Grafo<String> grafoTransferencias = new Grafo<>();
    private final Grafo<String> grafoMultimonedero = new Grafo<>();
    private final Grafo<String> grafoCategorias = new Grafo<>();

    private final Map<String, String> categoriasExplicitas = new HashMap<>();
    private final Map<String, String> ultimaCategoriaPorCliente = new HashMap<>();
    private final Map<String, Double> gastoPorCategoria = new HashMap<>();
    private final Map<String, Gasto> gastos = new HashMap<>();

    // ===================================================================
    // Contract used by ServicioTransacciones (Persona 1). Do not change the signature.
    // ===================================================================

    /** Called after every successful transaction. */
    public void registrar(Transaccion transaccion) {
        aplicar(transaccion, 1);
    }

    /** Called when a transaction is reverted: subtracts its effect from the graphs and totals. */
    public void revertir(Transaccion transaccion) {
        aplicar(transaccion, -1);
    }

    /** Assigns an explicit category to a transaction, reclassifying it if it was already counted. */
    public void asignarCategoria(String idTransaccion, String categoria) {
        if (categoria == null || categoria.isBlank()) {
            throw new OperacionInvalidaException("The category cannot be empty");
        }
        categoriasExplicitas.put(idTransaccion, categoria.trim());
        Gasto anterior = gastos.get(idTransaccion);
        if (anterior != null) {
            quitarGasto(idTransaccion);
            registrarGasto(anterior.transaccion());
        }
    }

    // ===================================================================
    // Queries for PanelReportes
    // ===================================================================

    public Optional<Map.Entry<String, Double>> categoriaConMayorGasto() {
        return mayor(gastoPorCategoria);
    }

    public Map<String, Double> getGastoPorCategoria() {
        return Collections.unmodifiableMap(gastoPorCategoria);
    }

    /** Client with the largest total sent (sum of outgoing edge weights). */
    public Optional<Map.Entry<String, Double>> clienteQueMasTransfiere() {
        return mayor(totalesSalientes(grafoTransferencias));
    }

    /** Client with the largest total received (sum of incoming edge weights). */
    public Optional<Map.Entry<String, Double>> clienteQueMasRecibe() {
        return mayor(totalesEntrantes(grafoTransferencias));
    }

    /** Clients reachable from idCliente through transfers (BFS). */
    public List<String> conexionesDeCliente(String idCliente) {
        List<String> resultado = new ArrayList<>();
        if (grafoTransferencias.contieneVertice(idCliente)) {
            resultado = grafoTransferencias.bfs(idCliente);
        }
        return resultado;
    }

    /** Same reachability as above, visited in depth-first order (DFS). */
    public List<String> recorridoDfsCliente(String idCliente) {
        List<String> resultado = new ArrayList<>();
        if (grafoTransferencias.contieneVertice(idCliente)) {
            resultado = grafoTransferencias.dfs(idCliente);
        }
        return resultado;
    }

    /** true if money goes around in a loop between clients (A -> B -> ... -> A). */
    public boolean hayCiclosEnTransferencias() {
        return grafoTransferencias.hayCiclo();
    }

    /** Wallets reachable from idMonedero through internal moves (BFS). */
    public List<String> monederosConectados(String idMonedero) {
        List<String> resultado = new ArrayList<>();
        if (grafoMultimonedero.contieneVertice(idMonedero)) {
            resultado = grafoMultimonedero.bfs(idMonedero);
        }
        return resultado;
    }

    // ===================================================================
    // Private logic
    // ===================================================================

    private void aplicar(Transaccion t, int signo) {
        switch (t.getTipo()) {
            case DEPOSITO -> { }   // income: does not affect the spending graphs
            case RETIRO -> actualizarGasto(t, signo);
            case TRANSFERENCIA -> {
                if (t.esInterna()) {
                    sumarPeso(grafoMultimonedero, t.getOrigen().getId(), t.getDestino().getId(),
                            signo * t.getMonto());
                } else {
                    sumarPeso(grafoTransferencias, t.getOrigen().getDuenio().getId(),
                            t.getDestino().getDuenio().getId(), signo * t.getMonto());
                    actualizarGasto(t, signo);
                }
            }
        }
    }

    private void actualizarGasto(Transaccion t, int signo) {
        if (signo > 0) registrarGasto(t);
        else quitarGasto(t.getId());
    }

    private void registrarGasto(Transaccion t) {
        String categoria = categoriasExplicitas.getOrDefault(t.getId(), inferirCategoria(t));
        String cliente = t.getCliente().getId();
        String desde = ultimaCategoriaPorCliente.get(cliente);

        if (desde != null && !desde.equals(categoria)) {
            sumarPeso(grafoCategorias, desde, categoria, t.getMonto());
        }
        ultimaCategoriaPorCliente.put(cliente, categoria);
        gastoPorCategoria.merge(categoria, t.getMonto(), Double::sum);
        gastos.put(t.getId(), new Gasto(t, categoria, desde));
    }

    private void quitarGasto(String idTransaccion) {
        Gasto g = gastos.remove(idTransaccion);
        if (g == null) return;
        double monto = g.transaccion().getMonto();
        gastoPorCategoria.merge(g.categoria(), -monto, Double::sum);
        if (g.desde() != null && !g.desde().equals(g.categoria())) {
            sumarPeso(grafoCategorias, g.desde(), g.categoria(), -monto);
        }
    }

    /** Explicit category first; otherwise keyword in the description; otherwise by type. */
    private static String inferirCategoria(Transaccion t) {
        Set<String> tokens = tokens(t.descripcion());
        for (Map.Entry<String, List<String>> entrada : PALABRAS_CLAVE.entrySet()) {
            for (String palabra : entrada.getValue()) {
                if (tokens.contains(palabra)) return entrada.getKey();
            }
        }
        return switch (t.getTipo()) {
            case RETIRO -> CAT_RETIRO;
            case TRANSFERENCIA -> CAT_TRANSFERENCIA;
            case DEPOSITO -> CAT_INGRESO;
        };
    }

    /** Lowercase, accent-free words of a text. "Depósito" -> "deposito". */
    private static Set<String> tokens(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return new HashSet<>(Arrays.asList(sinTildes.toLowerCase().split("[^a-z]+")));
    }

    /** Adds the vertices if needed and accumulates the weight on the edge. */
    private static void sumarPeso(Grafo<String> g, String origen, String destino, double peso) {
        g.agregarVertice(origen);
        g.agregarVertice(destino);
        g.agregarArista(origen, destino, peso);
    }

    private static Optional<Map.Entry<String, Double>> mayor(Map<String, Double> valores) {
        return valores.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .max(Map.Entry.comparingByValue());
    }

    private static Map<String, Double> totalesSalientes(Grafo<String> g) {
        Map<String, Double> total = new HashMap<>();
        for (String v : g.getVertices()) {
            double suma = 0;
            for (Grafo.Arista<String> a : g.getAristas(v)) suma += a.getPeso();
            total.put(v, suma);
        }
        return total;
    }

    private static Map<String, Double> totalesEntrantes(Grafo<String> g) {
        Map<String, Double> total = new HashMap<>();
        for (String v : g.getVertices()) {
            for (Grafo.Arista<String> a : g.getAristas(v)) {
                total.merge(a.getDestino(), a.getPeso(), Double::sum);
            }
        }
        return total;
    }

    /** One line per category transition with positive weight, e.g. "Comida -> Servicios ($120,000.00)". */
    public List<String> transicionesCategorias() {
        List<String> lineas = new ArrayList<>();
        for (String origen : grafoCategorias.getVertices()) {
            for (Grafo.Arista<String> a : grafoCategorias.getAristas(origen)) {
                if (a.getPeso() > 0) {
                    lineas.add(origen + " -> " + a.getDestino()
                            + " ($" + String.format("%,.2f", a.getPeso()) + ")");
                }
            }
        }
        return lineas;
    }
}
package co.edu.uniquindio.poo.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import co.edu.uniquindio.poo.modelo.Cliente;
import co.edu.uniquindio.poo.modelo.Monedero;
import co.edu.uniquindio.poo.modelo.TipoTransaccion;
import co.edu.uniquindio.poo.modelo.Transaccion;
import co.edu.uniquindio.poo.servicios.OperacionInvalidaException;
import co.edu.uniquindio.poo.servicios.SistemaMonedero;

/**
 * PERSONA 3: spending reports, reclassification and graph connections.
 * Talks only to SistemaMonedero.
 */
public class PanelReportes extends BorderPane implements PanelCliente {

    private static final List<String> CATEGORIAS =
            List.of("Comida", "Servicios", "Ahorro", "Transferencia", "Retiro", "Ingreso", "Otros");
    private static final int MAX_GASTOS_RECIENTES = 10;

    /** One row of the category table. */
    private record FilaCategoria(String categoria, double total) { }

    private final SistemaMonedero sistema;
    private final Runnable alCambiarDatos;
    private Cliente cliente;

    private final Label lblTopCategoria = new Label("-");
    private final Label lblTopCategoriaMonto = new Label();
    private final Label lblTopEnvia = new Label("-");
    private final Label lblTopEnviaMonto = new Label();
    private final Label lblTopRecibe = new Label("-");
    private final Label lblTopRecibeMonto = new Label();

    private final Label lblClienteActual = new Label();
    private final TableView<FilaCategoria> tablaCategorias = new TableView<>();
    private final ComboBox<Transaccion> cmbGasto = new ComboBox<>();
    private final ComboBox<String> cmbNuevaCategoria = new ComboBox<>();
    private final TextArea txtConexiones = new TextArea();
    private final Label lblMensaje = new Label();

    public PanelReportes(SistemaMonedero sistema, Runnable alCambiarDatos) {
        this.sistema = sistema;
        this.alCambiarDatos = alCambiarDatos;
        setPadding(new Insets(12));
        setTop(crearResumen());

        ScrollPane scroll = new ScrollPane(crearContenido());
        scroll.setFitToWidth(true);
        setCenter(scroll);

        setBottom(lblMensaje);
        BorderPane.setMargin(lblMensaje, new Insets(8, 0, 0, 0));

        refrescarVista();
    }

    @Override
    public void mostrarCliente(Cliente c) {
        this.cliente = c;
        refrescarVista();
    }

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    private VBox crearResumen() {
        HBox tarjetas = new HBox(12,
                tarjetaResumen("Categoría de mayor gasto", lblTopCategoria, lblTopCategoriaMonto),
                tarjetaResumen("Cliente que más transfiere", lblTopEnvia, lblTopEnviaMonto),
                tarjetaResumen("Cliente que más recibe", lblTopRecibe, lblTopRecibeMonto));
        return new VBox(8, titulo("Resumen global (todos los clientes)"), tarjetas);
    }

    private VBox crearContenido() {
        tablaCategorias.getColumns().add(columnaCategoria("Categoría", 220, FilaCategoria::categoria));
        tablaCategorias.getColumns().add(columnaCategoria("Gasto acumulado", 180, f -> dinero(f.total())));
        tablaCategorias.setPlaceholder(new Label("Sin gastos registrados"));
        tablaCategorias.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaCategorias.setPrefHeight(180);

        cmbGasto.setConverter(conversorGasto());
        cmbGasto.setPrefWidth(420);
        cmbGasto.setPromptText("Gasto reciente");

        cmbNuevaCategoria.getItems().setAll(CATEGORIAS);
        cmbNuevaCategoria.setEditable(true);
        cmbNuevaCategoria.setValue("Otros");
        cmbNuevaCategoria.setPrefWidth(180);

        Button btnReclasificar = new Button("Reclasificar");
        btnReclasificar.getStyleClass().add("success");
        btnReclasificar.setOnAction(e -> reclasificar());

        HBox filaGasto = new HBox(8, new Label("Gasto:"), cmbGasto);
        HBox filaCategoria = new HBox(8, new Label("Nueva categoría:"), cmbNuevaCategoria, btnReclasificar);

        txtConexiones.setEditable(false);
        txtConexiones.setPrefRowCount(14);

        Button btnRefrescar = new Button("Refrescar reportes");
        btnRefrescar.getStyleClass().add("accent");
        btnRefrescar.setOnAction(e -> refrescarVista());

        return new VBox(10,
                lblClienteActual,
                titulo("Gasto acumulado por categoría (todos los clientes)"),
                tablaCategorias,
                titulo("Reclasificar un gasto reciente"),
                filaGasto,
                filaCategoria,
                new Separator(),
                titulo("Conexiones de los grafos"),
                txtConexiones,
                btnRefrescar);
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void reclasificar() {
        Transaccion t = cmbGasto.getValue();
        try {
            if (t == null) throw new OperacionInvalidaException("Elija un gasto reciente");
            String categoria = cmbNuevaCategoria.getEditor().getText().trim();
            sistema.asignarCategoria(t.getId(), categoria);
            mensaje("Gasto " + t.getId() + " reclasificado como " + categoria, false);
        } catch (OperacionInvalidaException ex) {
            mensaje(ex.getMessage(), true);
            return;
        }
        refrescarVista();
        alCambiarDatos.run();
    }

    // ------------------------------------------------------------------
    // Refresh
    // ------------------------------------------------------------------

    private void refrescarVista() {
        lblClienteActual.setText(cliente == null
                ? "Elija un cliente para ver sus gastos recientes y sus conexiones"
                : "Cliente seleccionado: " + cliente);
        cargarResumen();
        cargarCategorias();
        cargarGastosRecientes();
        cargarConexiones();
    }

    private void cargarResumen() {
        mostrarTop(sistema.categoriaConMayorGasto(), lblTopCategoria, lblTopCategoriaMonto, false);
        mostrarTop(sistema.clienteQueMasTransfiere(), lblTopEnvia, lblTopEnviaMonto, true);
        mostrarTop(sistema.clienteQueMasRecibe(), lblTopRecibe, lblTopRecibeMonto, true);
    }

    private void cargarCategorias() {
        List<FilaCategoria> filas = new ArrayList<>();
        for (Map.Entry<String, Double> e : sistema.gastoPorCategoria().entrySet()) {
            if (e.getValue() > 0) filas.add(new FilaCategoria(e.getKey(), e.getValue()));
        }
        filas.sort(Comparator.comparingDouble(FilaCategoria::total).reversed());
        tablaCategorias.getItems().setAll(filas);
    }

    /** Most recent outgoing spendings of the selected client (not reverted, not internal). */
    private void cargarGastosRecientes() {
        cmbGasto.getItems().clear();
        if (cliente == null) return;

        List<Transaccion> gastos = new ArrayList<>();
        for (Transaccion t : sistema.getHistorialCompleto(cliente.getId())) {
            if (esGastoDelCliente(t)) gastos.add(t);
        }
        Collections.reverse(gastos);
        cmbGasto.getItems().setAll(gastos.subList(0, Math.min(MAX_GASTOS_RECIENTES, gastos.size())));
        if (!cmbGasto.getItems().isEmpty()) cmbGasto.setValue(cmbGasto.getItems().get(0));
    }

    private void cargarConexiones() {
        if (cliente == null) {
            txtConexiones.setText("Elija un cliente para ver sus conexiones.");
            return;
        }
        String id = cliente.getId();
        StringBuilder sb = new StringBuilder();

        sb.append("1) Transferencias entre clientes (grafo dirigido)\n");
        sb.append("   BFS: ").append(textoLista(nombresClientes(sistema.conexionesDe(id)))).append("\n");
        sb.append("   DFS: ").append(textoLista(nombresClientes(sistema.recorridoDfsDe(id)))).append("\n");
        sb.append("   ¿Hay ciclos de dinero entre clientes? ")
                .append(sistema.hayCiclosEnTransferencias() ? "SÍ" : "NO").append("\n\n");

        sb.append("2) Multimonedero (monedero principal, movimientos internos)\n");
        Monedero principal = cliente.getMonederoPrincipal();
        List<String> conectados = sistema.monederosConectados(id, principal.getId());
        sb.append("   Conectados desde ").append(principal.getNombre()).append(": ")
                .append(textoLista(etiquetasMonederos(conectados))).append("\n\n");

        sb.append("3) Transiciones entre categorías de gasto (todos los clientes)\n");
        List<String> transiciones = sistema.transicionesCategorias();
        if (transiciones.isEmpty()) sb.append("   Sin transiciones registradas\n");
        for (String linea : transiciones) sb.append("   ").append(linea).append("\n");

        txtConexiones.setText(sb.toString());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private boolean esGastoDelCliente(Transaccion t) {
        boolean salida = t.getOrigen() != null && t.getOrigen().getDuenio().equals(cliente);
        return salida && !t.isRevertida() && !t.esInterna() && t.getTipo() != TipoTransaccion.DEPOSITO;
    }

    private void mostrarTop(Optional<Map.Entry<String, Double>> top, Label lblNombre, Label lblMonto,
                            boolean esCliente) {
        if (top.isEmpty()) {
            lblNombre.setText("-");
            lblMonto.setText("Sin datos");
            return;
        }
        String clave = top.get().getKey();
        lblNombre.setText(esCliente ? nombreCliente(clave) : clave);
        lblMonto.setText(dinero(top.get().getValue()));
    }

    private String nombreCliente(String id) {
        Cliente c = sistema.buscarCliente(id);
        return c == null ? id : c.getNombre();
    }

    private List<String> nombresClientes(List<String> ids) {
        List<String> nombres = new ArrayList<>();
        for (String id : ids) nombres.add(nombreCliente(id));
        return nombres;
    }

    /** Maps wallet ids to "Client/Wallet" labels. */
    private List<String> etiquetasMonederos(List<String> ids) {
        Map<String, String> mapa = new HashMap<>();
        for (Cliente c : sistema.listarClientes()) {
            for (Monedero m : c.getMonederos()) {
                mapa.put(m.getId(), c.getNombre() + "/" + m.getNombre());
            }
        }
        List<String> etiquetas = new ArrayList<>();
        for (String id : ids) etiquetas.add(mapa.getOrDefault(id, id));
        return etiquetas;
    }

    private static String textoLista(List<String> elementos) {
        return elementos.isEmpty() ? "(sin conexiones)" : String.join(", ", elementos);
    }

    private void mensaje(String texto, boolean error) {
        lblMensaje.setText(texto);
        lblMensaje.setStyle(error ? "-fx-text-fill: #b00020;" : "-fx-text-fill: #1b6e20;");
    }

    private static VBox tarjetaResumen(String etiqueta, Label valor, Label monto) {
        Label lblEtiqueta = new Label(etiqueta);
        lblEtiqueta.getStyleClass().add("etiqueta-tarjeta");
        valor.getStyleClass().add("cifra-grande");
        monto.getStyleClass().add("cifra-mediana");

        VBox caja = new VBox(4, lblEtiqueta, valor, monto);
        caja.getStyleClass().add("tarjeta");
        caja.setPadding(new Insets(12));
        caja.setMinWidth(220);
        return caja;
    }

    private static Label titulo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("titulo-seccion");
        return l;
    }

    private static String dinero(double valor) {
        return "$" + String.format("%,.2f", valor);
    }

    private static TableColumn<FilaCategoria, String> columnaCategoria(String nombre, double ancho,
                                                                       Function<FilaCategoria, String> valor) {
        TableColumn<FilaCategoria, String> col = new TableColumn<>(nombre);
        col.setPrefWidth(ancho);
        col.setCellValueFactory(c -> new SimpleStringProperty(valor.apply(c.getValue())));
        return col;
    }

    private static StringConverter<Transaccion> conversorGasto() {
        return new StringConverter<>() {
            @Override
            public String toString(Transaccion t) {
                return t == null ? "" : t.getId() + " · " + t.descripcion() + " · " + dinero(t.getMonto());
            }

            @Override
            public Transaccion fromString(String texto) {
                return null;
            }
        };
    }
}
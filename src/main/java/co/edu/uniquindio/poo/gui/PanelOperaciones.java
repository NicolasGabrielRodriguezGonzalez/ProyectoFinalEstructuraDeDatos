package co.edu.uniquindio.poo.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import co.edu.uniquindio.poo.estructuras.ListaDoble;
import co.edu.uniquindio.poo.modelo.Cliente;
import co.edu.uniquindio.poo.modelo.Monedero;
import co.edu.uniquindio.poo.modelo.Transaccion;
import co.edu.uniquindio.poo.servicios.OperacionInvalidaException;
import co.edu.uniquindio.poo.servicios.ServicioTransacciones;
import co.edu.uniquindio.poo.servicios.SistemaMonedero;

import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/**
 * PERSONA 1: pestaña de operaciones.
 * Depósitos, retiros, transferencias (a otro cliente o entre monederos propios),
 * crear monederos, deshacer la última transacción, verificar integridad e historial.
 */
public class PanelOperaciones extends BorderPane implements PanelCliente {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");

    private final SistemaMonedero sistema;
    private final Runnable alCambiarDatos;
    private Cliente cliente;

    private final ComboBox<Monedero> cmbMonedero = new ComboBox<>();
    private final Label lblSaldo = new Label();
    private final TextField txtMonto = new TextField();
    private final ComboBox<Cliente> cmbClienteDestino = new ComboBox<>();
    private final ComboBox<Monedero> cmbMonederoDestino = new ComboBox<>();
    private final TextField txtNuevoMonedero = new TextField();
    private final CheckBox chkTodos = new CheckBox("Ver todos los monederos");
    private final TableView<Transaccion> tabla = new TableView<>();
    private final Label lblMensaje = new Label();

    public PanelOperaciones(SistemaMonedero sistema, Runnable alCambiarDatos) {
        this.sistema = sistema;
        this.alCambiarDatos = alCambiarDatos;
        setPadding(new Insets(12));
        setLeft(crearFormulario());
        setCenter(crearHistorial());
        setBottom(lblMensaje);
        BorderPane.setMargin(lblMensaje, new Insets(8, 0, 0, 0));
    }

    @Override
    public void mostrarCliente(Cliente c) {
        this.cliente = c;
        Monedero antes = cmbMonedero.getValue();
        cmbMonedero.getItems().clear();
        if (c != null) {
            for (Monedero m : c.getMonederos()) cmbMonedero.getItems().add(m);
            cmbMonedero.setValue(antes != null && antes.getDuenio() == c ? antes : c.getMonederoPrincipal());
        }
        Cliente destinoAntes = cmbClienteDestino.getValue();
        cmbClienteDestino.getItems().clear();
        for (Cliente otro : sistema.listarClientes()) cmbClienteDestino.getItems().add(otro);
        if (destinoAntes != null) cmbClienteDestino.setValue(destinoAntes);
        actualizarVista();
    }

    // ------------------------------------------------------------------
    // Construcción de la vista
    // ------------------------------------------------------------------

    private VBox crearFormulario() {
        cmbMonedero.setMaxWidth(Double.MAX_VALUE);
        cmbMonedero.valueProperty().addListener((o, a, b) -> actualizarVista());
        lblSaldo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        txtMonto.setPromptText("Monto, ej. 50000");

        Button btnDepositar = new Button("Depositar");
        btnDepositar.setOnAction(e -> ejecutar(() -> {
            Transaccion t = sistema.depositar(cliente.getId(), monederoActual().getId(), leerMonto());
            return "Depósito hecho: " + t;
        }));
        Button btnRetirar = new Button("Retirar");
        btnRetirar.setOnAction(e -> ejecutar(() -> {
            Transaccion t = sistema.retirar(cliente.getId(), monederoActual().getId(), leerMonto());
            return "Retiro hecho: " + t;
        }));
        HBox filaBotones = new HBox(8, btnDepositar, btnRetirar);

        cmbClienteDestino.setPromptText("Cliente destino");
        cmbClienteDestino.setMaxWidth(Double.MAX_VALUE);
        cmbClienteDestino.valueProperty().addListener((o, a, nuevo) -> {
            cmbMonederoDestino.getItems().clear();
            if (nuevo != null) {
                for (Monedero m : nuevo.getMonederos()) cmbMonederoDestino.getItems().add(m);
                cmbMonederoDestino.setValue(nuevo.getMonederoPrincipal());
            }
        });
        cmbMonederoDestino.setPromptText("Monedero destino");
        cmbMonederoDestino.setMaxWidth(Double.MAX_VALUE);
        Button btnTransferir = new Button("Transferir");
        btnTransferir.setOnAction(e -> ejecutar(() -> {
            Cliente dest = cmbClienteDestino.getValue();
            Monedero mDest = cmbMonederoDestino.getValue();
            if (dest == null || mDest == null) throw new OperacionInvalidaException("Elija el cliente y el monedero de destino");
            Transaccion t = sistema.transferir(cliente.getId(), monederoActual().getId(),
                    dest.getId(), mDest.getId(), leerMonto());
            return "Transferencia hecha: " + t;
        }));

        txtNuevoMonedero.setPromptText("Nombre, ej. Ahorros");
        Button btnCrear = new Button("Crear monedero");
        btnCrear.setOnAction(e -> ejecutar(() -> {
            Monedero m = sistema.crearMonedero(cliente.getId(), txtNuevoMonedero.getText());
            txtNuevoMonedero.clear();
            return "Monedero creado: " + m;
        }));

        Button btnDeshacer = new Button("Deshacer última transacción");
        btnDeshacer.setMaxWidth(Double.MAX_VALUE);
        btnDeshacer.setOnAction(e -> ejecutar(() -> "Revertida: " + sistema.revertirUltima(cliente.getId())));

        Button btnVerificar = new Button("Verificar integridad");
        btnVerificar.setMaxWidth(Double.MAX_VALUE);
        btnVerificar.setOnAction(e -> verificar());

        GridPane transferencia = new GridPane();
        transferencia.setHgap(6);
        transferencia.setVgap(6);
        transferencia.addRow(0, new Label("Para:"), cmbClienteDestino);
        transferencia.addRow(1, new Label("Monedero:"), cmbMonederoDestino);

        VBox form = new VBox(8,
                titulo("Monedero"), cmbMonedero, lblSaldo,
                new Separator(),
                titulo("Monto"), txtMonto, filaBotones,
                new Separator(),
                titulo("Transferir"), transferencia, btnTransferir,
                new Separator(),
                titulo("Nuevo monedero"), new HBox(6, txtNuevoMonedero, btnCrear),
                new Separator(),
                btnDeshacer, btnVerificar);
        form.setPrefWidth(320);
        form.setPadding(new Insets(0, 12, 0, 0));
        return form;
    }

    private VBox crearHistorial() {
        tabla.getColumns().add(columna("Id", 50, Transaccion::getId));
        tabla.getColumns().add(columna("Fecha", 100, t -> t.getFecha().format(FECHA)));
        tabla.getColumns().add(columna("Tipo", 110, t -> t.getTipo().toString()));
        tabla.getColumns().add(columna("Detalle", 300, Transaccion::descripcion));
        tabla.getColumns().add(columna("Monto", 100, t -> dinero(t.getMonto())));
        tabla.getColumns().add(columna("Comisión", 80, t -> dinero(t.getComision())));
        tabla.getColumns().add(columna("Efecto", 100, t -> {
            Monedero m = monederoActual();
            return chkTodos.isSelected() || m == null ? "" : dinero(t.efectoSobre(m));
        }));
        tabla.getColumns().add(columna("Estado", 90, t -> t.isRevertida() ? "Revertida" : "OK"));
        tabla.setPlaceholder(new Label("Sin transacciones"));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        chkTodos.setOnAction(e -> actualizarVista());
        HBox barra = new HBox(12, titulo("Historial (más reciente arriba)"), chkTodos);
        barra.setAlignment(Pos.CENTER_LEFT);

        VBox caja = new VBox(8, barra, tabla);
        VBox.setVgrow(tabla, javafx.scene.layout.Priority.ALWAYS);
        return caja;
    }

    // ------------------------------------------------------------------
    // Lógica de la vista
    // ------------------------------------------------------------------

    private void actualizarVista() {
        Monedero m = monederoActual();
        lblSaldo.setText(m == null ? "$ -" : "Saldo: " + dinero(m.getSaldo()));

        tabla.getItems().clear();
        if (cliente == null) return;
        ListaDoble<Transaccion> historial = chkTodos.isSelected()
                ? sistema.getHistorialCompleto(cliente.getId())
                : (m == null ? new ListaDoble<>() : m.getHistorial());
        // La lista doble se recorre al revés para mostrar primero la más reciente.
        for (Transaccion t : historial.alReves()) tabla.getItems().add(t);
        if (chkTodos.isSelected()) {
            FXCollections.sort(tabla.getItems(), (a, b) -> b.getFecha().compareTo(a.getFecha()));
        }
        tabla.refresh();
    }

    private void verificar() {
        if (cliente == null) return;
        StringBuilder sb = new StringBuilder();
        boolean todoBien = true;
        for (ServicioTransacciones.ResultadoVerificacion r : sistema.verificarIntegridad(cliente.getId())) {
            sb.append(r).append("\n");
            todoBien &= r.esValido();
        }
        Alert a = new Alert(todoBien ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        a.setTitle("Verificación de integridad");
        a.setHeaderText(todoBien ? "Todos los monederos están correctos" : "Se encontraron problemas");
        TextArea area = new TextArea(sb.toString());
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(8);
        a.getDialogPane().setContent(area);
        a.showAndWait();
    }

    /** Ejecuta una operación, muestra el resultado o el error y refresca toda la ventana. */
    private void ejecutar(java.util.function.Supplier<String> operacion) {
        if (cliente == null) {
            mensaje("Primero elija un cliente", true);
            return;
        }
        try {
            String resultado = operacion.get();
            txtMonto.clear();
            alCambiarDatos.run();
            mensaje(resultado, false);
        } catch (OperacionInvalidaException ex) {
            mensaje(ex.getMessage(), true);
        }
    }

    private double leerMonto() {
        String texto = txtMonto.getText().replace("$", "").replace(" ", "").replace(",", "");
        if (texto.isEmpty()) throw new OperacionInvalidaException("Escriba un monto");
        try {
            return Double.parseDouble(texto);
        } catch (NumberFormatException e) {
            throw new OperacionInvalidaException("El monto debe ser un número, por ejemplo 50000");
        }
    }

    private Monedero monederoActual() {
        Monedero m = cmbMonedero.getValue();
        if (m == null && cliente != null) return cliente.getMonederoPrincipal();
        return m;
    }

    private void mensaje(String texto, boolean error) {
        lblMensaje.setText(texto);
        lblMensaje.setStyle(error ? "-fx-text-fill: #b00020;" : "-fx-text-fill: #1b6e20;");
    }

    private static Label titulo(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-font-weight: bold;");
        return l;
    }

    private static String dinero(double v) {
        return "$" + String.format("%,.2f", v);
    }

    private static TableColumn<Transaccion, String> columna(String nombre, double ancho,
                                                            Function<Transaccion, String> valor) {
        TableColumn<Transaccion, String> col = new TableColumn<>(nombre);
        col.setPrefWidth(ancho);
        col.setCellValueFactory(c -> new SimpleStringProperty(valor.apply(c.getValue())));
        return col;
    }
}

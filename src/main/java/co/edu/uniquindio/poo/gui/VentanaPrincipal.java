package co.edu.uniquindio.poo.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import co.edu.uniquindio.poo.modelo.Cliente;
import co.edu.uniquindio.poo.servicios.OperacionInvalidaException;
import co.edu.uniquindio.poo.servicios.SistemaMonedero;

/**
 * PERSONA 1: ventana principal.
 * Arriba: buscador y selector de cliente, con su saldo total, puntos y rango.
 * En el centro: un TabPane con una pestaña por persona.
 *
 * Para agregar una pestaña nueva: crear una clase que implemente PanelCliente
 * y añadirla en el método crearPestanias().
 */
public class VentanaPrincipal {

    private final SistemaMonedero sistema;

    private final TextField txtBuscar = new TextField();
    private final ComboBox<Cliente> cmbCliente = new ComboBox<>();
    private final ObservableList<Cliente> clientesVisibles = FXCollections.observableArrayList();
    private final Label lblSaldoTotal = new Label();
    private final Label lblPuntos = new Label();
    private final Label lblRango = new Label();

    private PanelCliente[] paneles;

    public VentanaPrincipal(SistemaMonedero sistema) {
        this.sistema = sistema;
    }

    public void mostrar(Stage stage) {
        BorderPane raiz = new BorderPane();
        raiz.setTop(crearCabecera());
        raiz.setCenter(crearPestanias());

        filtrarClientes();
        if (!clientesVisibles.isEmpty()) cmbCliente.getSelectionModel().selectFirst();

        stage.setTitle("Monedero Virtual con Puntos");
        stage.setScene(new Scene(raiz, 1100, 680));
        stage.show();
    }

    /** Cliente elegido en el selector (puede ser null). */
    public Cliente getClienteActual() {
        return cmbCliente.getValue();
    }

    /** Vuelve a pintar la cabecera y todas las pestañas con el cliente actual. */
    public void refrescar() {
        Cliente c = getClienteActual();
        if (c == null) {
            lblSaldoTotal.setText("Saldo total: -");
            lblPuntos.setText("Puntos: -");
            lblRango.setText("Rango: -");
        } else {
            lblSaldoTotal.setText("Saldo total: $" + String.format("%,.2f", c.getSaldoTotal()));
            lblPuntos.setText("Puntos: " + c.getPuntosDisponibles() + " disponibles / "
                    + c.getPuntosHistoricos() + " históricos");
            lblRango.setText("Rango: " + c.getRango());
        }
        for (PanelCliente p : paneles) p.mostrarCliente(c);
    }

    // ------------------------------------------------------------------

    private VBox crearCabecera() {
        txtBuscar.setPromptText("Buscar por id o nombre");
        txtBuscar.textProperty().addListener((obs, viejo, nuevo) -> filtrarClientes());

        cmbCliente.setItems(clientesVisibles);
        cmbCliente.setPrefWidth(260);
        cmbCliente.setPromptText("Elija un cliente");
        cmbCliente.valueProperty().addListener((obs, viejo, nuevo) -> refrescar());

        Button btnNuevo = new Button("Nuevo cliente");
        btnNuevo.setOnAction(e -> dialogoNuevoCliente());

        HBox fila1 = new HBox(8, new Label("Cliente:"), txtBuscar, cmbCliente, btnNuevo);
        fila1.setAlignment(Pos.CENTER_LEFT);

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        lblSaldoTotal.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        HBox fila2 = new HBox(24, lblSaldoTotal, lblPuntos, lblRango, espacio);
        fila2.setAlignment(Pos.CENTER_LEFT);

        VBox cabecera = new VBox(8, fila1, fila2);
        cabecera.setPadding(new Insets(12));
        cabecera.setStyle("-fx-background-color: -fx-control-inner-background-alt; "
                + "-fx-border-color: transparent transparent #c8c8c8 transparent;");
        return cabecera;
    }

    private TabPane crearPestanias() {
        Runnable alCambiar = this::refrescar;
        PanelOperaciones operaciones = new PanelOperaciones(sistema, alCambiar);
        PanelPuntos puntos = new PanelPuntos(sistema, alCambiar);              // Persona 2
        PanelProgramadas programadas = new PanelProgramadas(sistema, alCambiar); // Persona 3
        PanelReportes reportes = new PanelReportes(sistema, alCambiar);        // Persona 3
        paneles = new PanelCliente[] {operaciones, puntos, programadas, reportes};

        TabPane tabs = new TabPane(
                new Tab("Operaciones", operaciones),
                new Tab("Puntos y rangos", puntos),
                new Tab("Programadas y alertas", programadas),
                new Tab("Reportes", reportes));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        return tabs;
    }

    private void filtrarClientes() {
        Cliente seleccionado = cmbCliente.getValue();
        clientesVisibles.clear();
        for (Cliente c : sistema.buscarClientes(txtBuscar.getText())) clientesVisibles.add(c);
        if (seleccionado != null && clientesVisibles.contains(seleccionado)) {
            cmbCliente.setValue(seleccionado);
        } else if (clientesVisibles.size() == 1) {
            cmbCliente.setValue(clientesVisibles.get(0));
        }
    }

    private void dialogoNuevoCliente() {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Nuevo cliente");
        TextField txtId = new TextField();
        TextField txtNombre = new TextField();
        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(8);
        g.addRow(0, new Label("Id / cédula:"), txtId);
        g.addRow(1, new Label("Nombre:"), txtNombre);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        d.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b -> {
            try {
                Cliente c = sistema.registrarCliente(txtId.getText(), txtNombre.getText());
                txtBuscar.clear();
                filtrarClientes();
                cmbCliente.setValue(c);
            } catch (OperacionInvalidaException ex) {
                new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
            }
        });
    }
}

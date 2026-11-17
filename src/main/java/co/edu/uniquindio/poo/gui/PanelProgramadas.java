package co.edu.uniquindio.poo.gui;

import co.edu.uniquindio.poo.modelo.*;
import co.edu.uniquindio.poo.modelo.TransaccionProgramada.Periodicidad;
import co.edu.uniquindio.poo.servicios.OperacionInvalidaException;
import co.edu.uniquindio.poo.servicios.SistemaMonedero;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/**
 * PERSONA 3: scheduled transactions, simulated clock and alert history.
 * Talks only to SistemaMonedero.
 */
public class PanelProgramadas extends BorderPane implements PanelCliente {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final SistemaMonedero sistema;
    private final Runnable alCambiarDatos;
    private Cliente cliente;

    private final Label lblFechaSimulada = new Label();
    private final Label lblAviso = new Label();
    private final ComboBox<TipoTransaccion> cmbTipo = new ComboBox<>();
    private final ComboBox<Monedero> cmbMonederoPropio = new ComboBox<>();
    private final ComboBox<Cliente> cmbClienteDestino = new ComboBox<>();
    private final ComboBox<Monedero> cmbMonederoDestino = new ComboBox<>();
    private final TextField txtMonto = new TextField();
    private final TextField txtCategoria = new TextField();
    private final DatePicker dpFecha = new DatePicker();
    private final TextField txtDias = new TextField();
    private final ComboBox<Periodicidad> cmbPeriodicidad = new ComboBox<>();
    private final TableView<TransaccionProgramada> tabla = new TableView<>();
    private final ListView<String> listaAlertas = new ListView<>();
    private final TextArea txtInforme = new TextArea();
    private final Label lblMensaje = new Label();

    public PanelProgramadas(SistemaMonedero sistema, Runnable alCambiarDatos) {
        this.sistema = sistema;
        this.alCambiarDatos = alCambiarDatos;
        setPadding(new Insets(12));
        setLeft(crearFormulario());

        ScrollPane scroll = new ScrollPane(crearCentro());
        scroll.setFitToWidth(true);
        setCenter(scroll);

        setBottom(lblMensaje);
        BorderPane.setMargin(lblMensaje, new Insets(8, 0, 0, 0));

        actualizarCamposDestino();
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

    private VBox crearFormulario() {
        cmbTipo.getItems().setAll(TipoTransaccion.values());
        cmbTipo.setConverter(conversorTipo());
        cmbTipo.setValue(TipoTransaccion.DEPOSITO);
        cmbTipo.setMaxWidth(Double.MAX_VALUE);
        cmbTipo.valueProperty().addListener((obs, viejo, nuevo) -> actualizarCamposDestino());

        cmbMonederoPropio.setConverter(conversorMonedero());
        cmbMonederoPropio.setMaxWidth(Double.MAX_VALUE);
        cmbMonederoPropio.setPromptText("Su monedero");

        cmbClienteDestino.setConverter(conversorCliente());
        cmbClienteDestino.setMaxWidth(Double.MAX_VALUE);
        cmbClienteDestino.setPromptText("Cliente destino");
        cmbClienteDestino.valueProperty().addListener((obs, viejo, nuevo) -> {
            cmbMonederoDestino.getItems().clear();
            if (nuevo != null) {
                for (Monedero m : nuevo.getMonederos()) cmbMonederoDestino.getItems().add(m);
                cmbMonederoDestino.setValue(nuevo.getMonederoPrincipal());
            }
        });

        cmbMonederoDestino.setConverter(conversorMonedero());
        cmbMonederoDestino.setMaxWidth(Double.MAX_VALUE);
        cmbMonederoDestino.setPromptText("Monedero destino");

        txtMonto.setPromptText("Ej. 50000");
        txtCategoria.setPromptText("Opcional, ej. Servicios");
        dpFecha.setMaxWidth(Double.MAX_VALUE);
        txtDias.setPromptText("Ej. 7 (opcional)");

        cmbPeriodicidad.getItems().setAll(Periodicidad.values());
        cmbPeriodicidad.setConverter(conversorPeriodicidad());
        cmbPeriodicidad.setValue(Periodicidad.UNICA);
        cmbPeriodicidad.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.addRow(0, new Label("Tipo:"), cmbTipo);
        grid.addRow(1, new Label("Mi monedero:"), cmbMonederoPropio);
        grid.addRow(2, new Label("Cliente destino:"), cmbClienteDestino);
        grid.addRow(3, new Label("Monedero destino:"), cmbMonederoDestino);
        grid.addRow(4, new Label("Monto:"), txtMonto);
        grid.addRow(5, new Label("Categoría:"), txtCategoria);
        grid.addRow(6, new Label("Fecha:"), dpFecha);
        grid.addRow(7, new Label("o días desde hoy:"), txtDias);
        grid.addRow(8, new Label("Periodicidad:"), cmbPeriodicidad);

        Button btnAgendar = new Button("Agendar transacción");
        btnAgendar.getStyleClass().add("success");
        btnAgendar.setMaxWidth(Double.MAX_VALUE);
        btnAgendar.setOnAction(e -> agendar());

        VBox form = new VBox(10, titulo("Agendar transacción"), grid, btnAgendar);
        form.setPrefWidth(400);
        form.setPadding(new Insets(0, 12, 0, 0));
        return form;
    }

    private VBox crearCentro() {
        Button btnAvanzar1 = new Button("Avanzar 1 día");
        btnAvanzar1.getStyleClass().add("accent");
        btnAvanzar1.setOnAction(e -> avanzar(1));

        Button btnAvanzar7 = new Button("Avanzar 7 días");
        btnAvanzar7.getStyleClass().add("accent");
        btnAvanzar7.setOnAction(e -> avanzar(7));

        HBox barraReloj = new HBox(10, lblFechaSimulada, btnAvanzar1, btnAvanzar7);
        barraReloj.setAlignment(Pos.CENTER_LEFT);

        lblAviso.setStyle("-fx-text-fill: #b00020; -fx-font-weight: bold;");
        lblAviso.setWrapText(true);
        VBox tarjetaAviso = new VBox(lblAviso);
        tarjetaAviso.getStyleClass().add("tarjeta");
        tarjetaAviso.setPadding(new Insets(8));
        tarjetaAviso.visibleProperty().bind(lblAviso.textProperty().isNotEmpty());
        tarjetaAviso.managedProperty().bind(tarjetaAviso.visibleProperty());

        tabla.getColumns().add(columna("Id", 50, TransaccionProgramada::getId));
        tabla.getColumns().add(columna("Próxima fecha", 100, p -> p.getFechaEjecucion().format(FECHA)));
        tabla.getColumns().add(columna("Periodicidad", 90, p -> textoPeriodicidad(p.getPeriodicidad())));
        tabla.getColumns().add(columna("Detalle", 300, TransaccionProgramada::describir));
        tabla.getColumns().add(columna("Categoría", 100, TransaccionProgramada::getCategoria));
        tabla.getColumns().add(columna("Monto", 100, p -> dinero(p.getMonto())));
        tabla.setPlaceholder(new Label("No hay transacciones programadas pendientes"));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPrefHeight(200);

        Button btnCancelar = new Button("Cancelar seleccionada");
        btnCancelar.getStyleClass().add("danger");
        btnCancelar.setOnAction(e -> cancelarSeleccionada());

        txtInforme.setEditable(false);
        txtInforme.setPrefRowCount(4);
        txtInforme.setPromptText("Aquí aparecerá lo que se ejecutó al avanzar el reloj");

        listaAlertas.setPrefHeight(160);
        listaAlertas.setPlaceholder(new Label("Sin alertas"));

        return new VBox(10,
                barraReloj,
                tarjetaAviso,
                titulo("Programadas pendientes"),
                tabla,
                btnCancelar,
                new Separator(),
                titulo("Alertas (últimas 15, la más reciente arriba)"),
                listaAlertas,
                titulo("Último avance del reloj"),
                txtInforme);
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void agendar() {
        if (cliente == null) {
            mensaje("Primero elija un cliente", true);
            return;
        }
        try {
            TipoTransaccion tipo = cmbTipo.getValue();
            Monedero propio = cmbMonederoPropio.getValue();
            if (tipo == null) throw new OperacionInvalidaException("Elija el tipo de transacción");
            if (propio == null) throw new OperacionInvalidaException("Elija su monedero");

            String idOrigen = null;
            String idClienteDestino = null;
            String idDestino = null;

            if (tipo == TipoTransaccion.DEPOSITO) {
                idDestino = propio.getId();          // the deposit goes into one of my wallets
            } else if (tipo == TipoTransaccion.RETIRO) {
                idOrigen = propio.getId();
            } else {
                Cliente destino = cmbClienteDestino.getValue();
                Monedero monederoDestino = cmbMonederoDestino.getValue();
                if (destino == null || monederoDestino == null) {
                    throw new OperacionInvalidaException("Elija el cliente y el monedero de destino");
                }
                idOrigen = propio.getId();
                idClienteDestino = destino.getId();
                idDestino = monederoDestino.getId();
            }

            TransaccionProgramada p = sistema.programar(cliente.getId(), tipo, idOrigen, idClienteDestino,
                    idDestino, leerMonto(), txtCategoria.getText(), leerFecha(), cmbPeriodicidad.getValue());

            txtMonto.clear();
            txtDias.clear();
            mensaje("Programada " + p.getId() + ": " + p.describir()
                    + " el " + p.getFechaEjecucion().format(FECHA), false);
            refrescarVista();
        } catch (OperacionInvalidaException ex) {
            mensaje(ex.getMessage(), true);
        }
    }

    private void cancelarSeleccionada() {
        TransaccionProgramada seleccionada = tabla.getSelectionModel().getSelectedItem();
        if (cliente == null || seleccionada == null) {
            mensaje("Seleccione una transacción programada de la tabla", true);
            return;
        }
        boolean ok = sistema.cancelarProgramada(cliente.getId(), seleccionada.getId());
        mensaje(ok ? "Cancelada " + seleccionada.getId() : "No se pudo cancelar " + seleccionada.getId(), !ok);
        refrescarVista();
    }

    /** The clock is global: advancing it executes due transactions of every client. */
    private void avanzar(int dias) {
        try {
            List<String> informe = sistema.avanzarDias(dias);
            long fallidas = informe.stream().filter(linea -> linea.startsWith("No se pudo")).count();
            long ejecutadas = informe.size() - fallidas;

            txtInforme.setText(informe.isEmpty()
                    ? "No hubo transacciones vencidas en este período."
                    : String.join("\n", informe));
            mensaje("Reloj avanzado " + dias + " día(s). Ejecutadas: " + ejecutadas
                    + " · Fallidas: " + fallidas, fallidas > 0);
        } catch (OperacionInvalidaException ex) {
            mensaje(ex.getMessage(), true);
        }
        alCambiarDatos.run();   // refreshes the balance in the header and the other tabs
        refrescarVista();
    }

    // ------------------------------------------------------------------
    // Refresh
    // ------------------------------------------------------------------

    private void refrescarVista() {
        LocalDate hoy = sistema.getFechaSimulada();
        lblFechaSimulada.setText("Fecha simulada: " + hoy.format(FECHA));
        if (dpFecha.getValue() == null || dpFecha.getValue().isBefore(hoy)) dpFecha.setValue(hoy);

        cargarMonederos();
        cargarClientesDestino();
        cargarTabla();
        cargarAlertas();
        actualizarAviso();
    }

    private void cargarMonederos() {
        Monedero antes = cmbMonederoPropio.getValue();
        cmbMonederoPropio.getItems().clear();
        if (cliente == null) return;
        for (Monedero m : cliente.getMonederos()) cmbMonederoPropio.getItems().add(m);
        cmbMonederoPropio.setValue(antes != null && antes.getDuenio().equals(cliente)
                ? antes : cliente.getMonederoPrincipal());
    }

    private void cargarClientesDestino() {
        Cliente antes = cmbClienteDestino.getValue();
        cmbClienteDestino.getItems().clear();
        for (Cliente c : sistema.listarClientes()) cmbClienteDestino.getItems().add(c);
        if (antes != null) cmbClienteDestino.setValue(antes);
    }

    private void cargarTabla() {
        tabla.getItems().clear();
        if (cliente != null) tabla.getItems().addAll(sistema.listarProgramadas(cliente.getId()));
    }

    /** Circular history from the system, shown newest first. */
    private void cargarAlertas() {
        listaAlertas.getItems().clear();
        if (cliente == null) return;
        List<Notificacion> historial = sistema.getNotificaciones(cliente.getId());
        for (int i = historial.size() - 1; i >= 0; i--) {
            Notificacion n = historial.get(i);
            listaAlertas.getItems().add(n.getFecha().format(FECHA_HORA) + "   " + n);
        }
    }

    private void actualizarAviso() {
        if (cliente != null && cliente.getSaldoTotal() < SistemaMonedero.UMBRAL_SALDO_BAJO) {
            lblAviso.setText("⚠ Saldo bajo: " + dinero(cliente.getSaldoTotal())
                    + " (umbral " + dinero(SistemaMonedero.UMBRAL_SALDO_BAJO) + ")");
        } else {
            lblAviso.setText("");
        }
    }

    /** Destination fields are only used for transfers. */
    private void actualizarCamposDestino() {
        boolean transferencia = cmbTipo.getValue() == TipoTransaccion.TRANSFERENCIA;
        cmbClienteDestino.setDisable(!transferencia);
        cmbMonederoDestino.setDisable(!transferencia);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private double leerMonto() {
        String texto = txtMonto.getText().replace("$", "").replace(" ", "").replace(",", "");
        if (texto.isEmpty()) throw new OperacionInvalidaException("Escriba un monto");
        try {
            return Double.parseDouble(texto);
        } catch (NumberFormatException e) {
            throw new OperacionInvalidaException("El monto debe ser un número, por ejemplo 50000");
        }
    }

    /** Days from the simulated date take precedence over the date picker when filled in. */
    private LocalDate leerFecha() {
        String dias = txtDias.getText().trim();
        if (!dias.isEmpty()) {
            try {
                return sistema.getFechaSimulada().plusDays(Integer.parseInt(dias));
            } catch (NumberFormatException e) {
                throw new OperacionInvalidaException("Los días deben ser un número entero, por ejemplo 7");
            }
        }
        if (dpFecha.getValue() == null) {
            throw new OperacionInvalidaException("Elija una fecha o escriba los días desde hoy");
        }
        return dpFecha.getValue();
    }

    private void mensaje(String texto, boolean error) {
        lblMensaje.setText(texto);
        lblMensaje.setStyle(error ? "-fx-text-fill: #b00020;" : "-fx-text-fill: #1b6e20;");
    }

    private static Label titulo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("titulo-seccion");
        return l;
    }

    private static String dinero(double valor) {
        return "$" + String.format("%,.2f", valor);
    }

    private static String textoPeriodicidad(Periodicidad p) {
        return switch (p) {
            case UNICA -> "Una vez";
            case DIARIA -> "Diaria";
            case SEMANAL -> "Semanal";
            case MENSUAL -> "Mensual";
        };
    }

    private static TableColumn<TransaccionProgramada, String> columna(String nombre, double ancho,
                                                                      Function<TransaccionProgramada, String> valor) {
        TableColumn<TransaccionProgramada, String> col = new TableColumn<>(nombre);
        col.setPrefWidth(ancho);
        col.setCellValueFactory(c -> new SimpleStringProperty(valor.apply(c.getValue())));
        return col;
    }

    private static StringConverter<TipoTransaccion> conversorTipo() {
        return new StringConverter<>() {
            @Override
            public String toString(TipoTransaccion t) {
                if (t == null) return "";
                return switch (t) {
                    case DEPOSITO -> "Depósito";
                    case RETIRO -> "Retiro";
                    case TRANSFERENCIA -> "Transferencia";
                };
            }

            @Override
            public TipoTransaccion fromString(String texto) {
                return null;
            }
        };
    }

    private static StringConverter<Periodicidad> conversorPeriodicidad() {
        return new StringConverter<>() {
            @Override
            public String toString(Periodicidad p) {
                return p == null ? "" : textoPeriodicidad(p);
            }

            @Override
            public Periodicidad fromString(String texto) {
                return null;
            }
        };
    }

    private static StringConverter<Monedero> conversorMonedero() {
        return new StringConverter<>() {
            @Override
            public String toString(Monedero m) {
                return m == null ? "" : m.getNombre() + " (" + dinero(m.getSaldo()) + ")";
            }

            @Override
            public Monedero fromString(String texto) {
                return null;
            }
        };
    }

    private static StringConverter<Cliente> conversorCliente() {
        return new StringConverter<>() {
            @Override
            public String toString(Cliente c) {
                return c == null ? "" : c.getId() + " - " + c.getNombre();
            }

            @Override
            public Cliente fromString(String texto) {
                return null;
            }
        };
    }
}
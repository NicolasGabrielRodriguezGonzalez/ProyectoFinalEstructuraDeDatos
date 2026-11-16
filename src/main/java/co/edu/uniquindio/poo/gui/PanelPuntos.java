package co.edu.uniquindio.poo.gui;

import co.edu.uniquindio.poo.estructuras.ArbolPuntosABB;
import co.edu.uniquindio.poo.modelo.Cliente;
import co.edu.uniquindio.poo.modelo.Rango;
import co.edu.uniquindio.poo.servicios.ServicioPuntos;
import co.edu.uniquindio.poo.servicios.ServicioPuntos.Beneficio;
import co.edu.uniquindio.poo.servicios.SistemaMonedero;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/**
 * PERSONA 2: pestaña de puntos, rango, canje de beneficios y ranking.
 */
public class PanelPuntos extends VBox implements PanelCliente {

    private final SistemaMonedero sistema;
    private final Runnable alCambiarDatos;
    private Cliente clienteActual;

    private final Label lblCliente = new Label();
    private final Label lblRango = new Label();
    private final Label lblHistoricos = new Label();
    private final Label lblDisponibles = new Label();
    private final Label lblSiguiente = new Label();
    private final Label lblDescuento = new Label();
    private final Label lblRetiros = new Label();

    private final Button btnDescuento = new Button("Canjear 100 pts: 10% menos comisión en transferencias");
    private final Button btnRetiros = new Button("Canjear 500 pts: un mes sin cargos por retiros");
    private final Button btnBono = new Button("Canjear 1000 pts: bono de 50 en el monedero");

    private final ListView<String> listaRanking = new ListView<>();
    private final ListView<String> listaHistorial = new ListView<>();

    public PanelPuntos(SistemaMonedero sistema, Runnable alCambiarDatos) {
        this.sistema = sistema;
        this.alCambiarDatos = alCambiarDatos;

        setPadding(new Insets(16));
        setSpacing(10);

        lblCliente.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        lblRango.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        btnDescuento.setOnAction(e -> canjear(Beneficio.DESCUENTO_TRANSFERENCIAS));
        btnRetiros.setOnAction(e -> canjear(Beneficio.RETIROS_GRATIS));
        btnBono.setOnAction(e -> canjear(Beneficio.BONO_SALDO));

        VBox colRanking = new VBox(6, new Label("Ranking (top 10)"), listaRanking);
        VBox colHistorial = new VBox(6, new Label("Historial de puntos"), listaHistorial);
        HBox.setHgrow(colRanking, Priority.ALWAYS);
        HBox.setHgrow(colHistorial, Priority.ALWAYS);
        HBox listas = new HBox(12, colRanking, colHistorial);
        VBox.setVgrow(listas, Priority.ALWAYS);

        getChildren().addAll(
                lblCliente, lblRango, lblHistoricos, lblDisponibles, lblSiguiente,
                new Label("Beneficios activos"), lblDescuento, lblRetiros,
                new Label("Canjear puntos"), btnDescuento, btnRetiros, btnBono,
                listas
        );

        refrescar();
    }

    @Override
    public void mostrarCliente(Cliente cliente) {
        this.clienteActual = cliente;
        refrescar();
    }

    private void refrescar() {
        ServicioPuntos sp = sistema.getPuntos();

        if (clienteActual == null) {
            lblCliente.setText("Sin cliente seleccionado");
            lblRango.setText("");
            lblHistoricos.setText("");
            lblDisponibles.setText("");
            lblSiguiente.setText("");
            lblDescuento.setText("");
            lblRetiros.setText("");
            btnDescuento.setDisable(true);
            btnRetiros.setDisable(true);
            btnBono.setDisable(true);
            listaRanking.getItems().clear();
            listaHistorial.getItems().clear();
            return;
        }

        lblCliente.setText("Cliente: " + clienteActual);
        lblRango.setText("Rango: " + clienteActual.getRango());
        lblHistoricos.setText("Puntos históricos (definen el rango): " + clienteActual.getPuntosHistoricos());
        lblDisponibles.setText("Puntos disponibles (para canjear): " + clienteActual.getPuntosDisponibles());
        lblSiguiente.setText(textoSiguienteRango());

        lblDescuento.setText(sp.tieneDescuentoTransferencias(clienteActual)
                ? "✔ 10% menos de comisión en transferencias"
                : "✘ Sin descuento en transferencias");

        LocalDate hasta = sp.getRetirosGratisHasta(clienteActual);
        lblRetiros.setText(sp.tieneRetirosGratis(clienteActual)
                ? "✔ Retiros sin cargo hasta " + hasta
                : "✘ Sin retiros gratis");

        btnDescuento.setDisable(sp.tieneDescuentoTransferencias(clienteActual)
                || clienteActual.getPuntosDisponibles() < Beneficio.DESCUENTO_TRANSFERENCIAS.getCosto());
        btnRetiros.setDisable(clienteActual.getPuntosDisponibles() < Beneficio.RETIROS_GRATIS.getCosto());
        btnBono.setDisable(clienteActual.getPuntosDisponibles() < Beneficio.BONO_SALDO.getCosto());

        listaRanking.getItems().clear();
        List<ArbolPuntosABB.Entrada> top = sp.getRanking(10);
        for (int i = 0; i < top.size(); i++) {
            ArbolPuntosABB.Entrada e = top.get(i);
            Cliente c = sistema.buscarCliente(e.idCliente());
            String nombre = c == null ? e.idCliente() : c.getNombre();
            listaRanking.getItems().add((i + 1) + ". " + nombre + " — " + e.puntos() + " pts");
        }

        listaHistorial.getItems().setAll(sp.getHistorialPuntos(clienteActual));
    }

    private String textoSiguienteRango() {
        Rango[] todos = Rango.values();
        int indice = clienteActual.getRango().ordinal();
        if (indice == todos.length - 1) return "Ya tienes el rango más alto.";
        Rango siguiente = todos[indice + 1];
        int faltan = siguiente.getMinimo() - clienteActual.getPuntosHistoricos();
        return "Te faltan " + faltan + " puntos para " + siguiente;
    }

    private void canjear(Beneficio beneficio) {
        if (clienteActual == null) return;

        boolean ok = sistema.getPuntos().canjear(clienteActual, beneficio);

        Alert alerta = new Alert(
                ok ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING,
                ok ? "Canje realizado: " + beneficio.getDescripcion()
                        : "No tienes puntos suficientes para este beneficio.",
                ButtonType.OK);
        alerta.setHeaderText(null);
        alerta.showAndWait();

        refrescar();
        alCambiarDatos.run();
    }
}
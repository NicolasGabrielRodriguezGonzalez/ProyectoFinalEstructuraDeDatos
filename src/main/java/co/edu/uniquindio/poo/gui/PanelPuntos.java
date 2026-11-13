package co.edu.uniquindio.poo.gui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import co.edu.uniquindio.poo.modelo.Cliente;
import co.edu.uniquindio.poo.servicios.SistemaMonedero;

/**
 * PERSONA 2: esta pestaña es suya. Reemplacen el contenido como quieran,
 * pero mantengan el constructor (sistema, alCambiarDatos) y mostrarCliente(...),
 * que es lo que usa VentanaPrincipal.
 *
 *  - sistema: la fachada; llamen solo a sus métodos (y a getPuntos(), getAlertas(), getAnalisis()).
 *  - alCambiarDatos.run(): llámenlo después de modificar algo (por ejemplo un canje)
 *    para que la cabecera y las demás pestañas se actualicen.
 */
public class PanelPuntos extends VBox implements PanelCliente {

    private final SistemaMonedero sistema;
    private final Runnable alCambiarDatos;
    private final Label lblCliente = new Label();

    public PanelPuntos(SistemaMonedero sistema, Runnable alCambiarDatos) {
        this.sistema = sistema;
        this.alCambiarDatos = alCambiarDatos;
        setPadding(new Insets(16));
        setSpacing(8);
        getChildren().addAll(new Label("Pendiente (Persona 2): Puntos, rango, canje de puntos y beneficios activos del cliente."), lblCliente);
    }

    @Override
    public void mostrarCliente(Cliente cliente) {
        lblCliente.setText(cliente == null ? "Sin cliente seleccionado" : "Cliente: " + cliente);
    }
}

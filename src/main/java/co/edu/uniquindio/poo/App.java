package co.edu.uniquindio.poo;

import javafx.application.Application;
import javafx.stage.Stage;
import co.edu.uniquindio.poo.gui.VentanaPrincipal;
import co.edu.uniquindio.poo.servicios.DatosEjemplo;
import co.edu.uniquindio.poo.servicios.SistemaMonedero;

/**
 * Punto de entrada: mvn javafx:run
 * Crea el sistema, carga los datos de ejemplo y abre la ventana principal.
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        SistemaMonedero sistema = new SistemaMonedero();
        DatosEjemplo.cargar(sistema);
        new VentanaPrincipal(sistema).mostrar(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

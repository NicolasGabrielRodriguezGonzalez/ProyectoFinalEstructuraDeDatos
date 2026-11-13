package co.edu.uniquindio.poo;

/**
 * Clase para ejecutar con el botón ▶ de IntelliJ.
 * Como no extiende Application, Java no exige los módulos de JavaFX al arrancar
 * y se evita el error "JavaFX runtime components are missing".
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}

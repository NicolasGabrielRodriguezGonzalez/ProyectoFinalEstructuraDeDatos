package co.edu.uniquindio.poo;

import co.edu.uniquindio.poo.modelo.Cliente;
import co.edu.uniquindio.poo.modelo.Monedero;
import co.edu.uniquindio.poo.modelo.Transaccion;
import co.edu.uniquindio.poo.servicios.OperacionInvalidaException;
import co.edu.uniquindio.poo.servicios.ServicioTransacciones;
import co.edu.uniquindio.poo.servicios.SistemaMonedero;

/**
 * Prueba rápida del núcleo por consola, sin interfaz gráfica.
 * Ejecutar: mvn compile exec:java -Dexec.mainClass=co.edu.uniquindio.poo.DemoConsola
 * (o con el botón "Run" del IDE sobre esta clase).
 */
public class DemoConsola {

    public static void main(String[] args) {
        SistemaMonedero s = new SistemaMonedero();
        Cliente ana = s.registrarCliente("1", "Ana");
        Cliente beto = s.registrarCliente("2", "Beto");
        Monedero anaPrincipal = ana.getMonederoPrincipal();
        Monedero anaAhorros = s.crearMonedero("1", "Ahorros");

        s.depositar("1", 100_000);
        s.retirar("1", anaPrincipal.getId(), 20_000);
        s.transferir("1", "2", 30_000);
        s.transferir("1", anaPrincipal.getId(), "1", anaAhorros.getId(), 10_000);

        System.out.println("\nMonederos de Ana: " + ana.getMonederos());
        System.out.println("Saldo de Beto: " + beto.getSaldoTotal());
        System.out.println("Historial principal de Ana: " + anaPrincipal.getHistorial());
        System.out.println("Pila de reversión de Ana: " + ana.getPilaReversion());

        Transaccion revertida = s.revertirUltima("1");
        System.out.println("\nRevertida: " + revertida);
        System.out.println("Monederos de Ana: " + ana.getMonederos());

        try {
            s.retirar("1", anaPrincipal.getId(), 1_000_000);
        } catch (OperacionInvalidaException e) {
            System.out.println("\nError esperado: " + e.getMessage());
        }

        System.out.println("\nVerificación de integridad:");
        for (ServicioTransacciones.ResultadoVerificacion r : s.verificarIntegridadSistema()) {
            System.out.println(r);
        }
    }
}

package co.edu.uniquindio.poo.servicios;

import co.edu.uniquindio.poo.modelo.Cliente;
import co.edu.uniquindio.poo.modelo.TipoTransaccion;
import co.edu.uniquindio.poo.modelo.Transaccion;

/**
 * PERSONA 2: puntos, rangos y beneficios.
 *
 * Estos métodos son el CONTRATO que usa ServicioTransacciones (Persona 1).
 * No cambien los nombres ni los parámetros; solo llenen el cuerpo.
 * Hoy están "vacíos" para que el proyecto compile y el núcleo funcione.
 */
public class ServicioPuntos {

    /** Se llama después de cada transacción exitosa. Debe sumar puntos y actualizar el rango. */
    public void registrar(Cliente cliente, TipoTransaccion tipo, double monto) {
        // TODO Persona 2: 1, 2 o 3 puntos por cada 100 según el tipo; actualizar TablaHash, AVL y rango.
        System.out.println("[puntos] registrar " + tipo + " $" + monto + " para " + cliente.getId());
    }

    /** Se llama al deshacer una transacción. Debe restar los puntos que esa transacción dio. */
    public void revertir(Transaccion transaccion) {
        // TODO Persona 2
        System.out.println("[puntos] revertir " + transaccion.getId());
    }

    /**
     * Comisión que se cobra en un retiro o una transferencia.
     * Debe aplicar el 10% de descuento si el cliente tiene ese beneficio.
     * Por ahora devuelve 0 (sin comisión) hasta confirmar con el profesor.
     */
    public double calcularComision(Cliente cliente, double monto) {
        // TODO Persona 2
        return 0;
    }

    /** true si el cliente tiene activo el beneficio "mes sin cargos por retiros". */
    public boolean tieneRetirosGratis(Cliente cliente) {
        // TODO Persona 2
        return false;
    }
}

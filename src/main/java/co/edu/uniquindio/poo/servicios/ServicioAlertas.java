package co.edu.uniquindio.poo.servicios;

import co.edu.uniquindio.poo.modelo.Cliente;
import co.edu.uniquindio.poo.modelo.Notificacion;

/**
 * PERSONA 3: alertas y notificaciones (lista circular).
 * Contrato que usa ServicioTransacciones (Persona 1). No cambien la firma.
 */
public class ServicioAlertas {

    public void notificar(Cliente cliente, Notificacion notificacion) {
        // TODO Persona 3: guardar en la ListaCircular de alertas del cliente.
        System.out.println("[alertas] " + cliente.getId() + ": " + notificacion);
    }
}

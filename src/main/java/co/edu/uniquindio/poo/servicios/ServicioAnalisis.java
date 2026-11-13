package co.edu.uniquindio.poo.servicios;

import co.edu.uniquindio.poo.modelo.Transaccion;

/**
 * PERSONA 3: análisis de patrones con grafos.
 * Contrato que usa ServicioTransacciones (Persona 1). No cambien la firma.
 */
public class ServicioAnalisis {

    /** Se llama después de cada transacción exitosa para alimentar los grafos. */
    public void registrar(Transaccion transaccion) {
        // TODO Persona 3: agregar vértices/aristas a los grafos de gastos y transferencias.
        System.out.println("[analisis] registrar " + transaccion.getId());
    }
}

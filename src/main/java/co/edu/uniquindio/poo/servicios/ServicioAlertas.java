package co.edu.uniquindio.poo.servicios;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.edu.uniquindio.poo.estructuras.Cola;
import co.edu.uniquindio.poo.estructuras.ListaCircular;
import co.edu.uniquindio.poo.modelo.Cliente;
import co.edu.uniquindio.poo.modelo.Notificacion;
import co.edu.uniquindio.poo.modelo.TransaccionProgramada;

/**
 * PERSONA 3: alertas y notificaciones.
 * - Cola<Notificacion>: outbox of pending notifications (FIFO).
 * - ListaCircular<Notificacion> per client: last CAPACIDAD_HISTORIAL alerts, oldest overwritten.
 */
public class ServicioAlertas {

    public static final int CAPACIDAD_HISTORIAL = 15;

    private final Cola<Notificacion> pendientes = new Cola<>();
    private final Map<String, ListaCircular<Notificacion>> historialPorCliente = new HashMap<>();

    /** Contract used by ServicioTransacciones (Persona 1). Do not change the signature. */
    public void notificar(Cliente cliente, Notificacion notificacion) {
        if (cliente == null) throw new IllegalArgumentException("The client is required");
        if (notificacion == null) throw new IllegalArgumentException("The notification is required");
        historialPorCliente
                .computeIfAbsent(cliente.getId(), k -> new ListaCircular<>(CAPACIDAD_HISTORIAL))
                .add(notificacion);
        pendientes.enqueue(notificacion);
    }

    public void notificarInformativa(Cliente cliente, String mensaje) {
        notificar(cliente, new Notificacion(Notificacion.Tipo.INFORMATIVA, mensaje));
    }

    /** Returns true if an alert was generated (saldo total below umbral). */
    public boolean alertaSaldoBajo(Cliente cliente, double saldoTotal, double umbral) {
        if (saldoTotal >= umbral) return false;
        notificar(cliente, new Notificacion(Notificacion.Tipo.SALDO_BAJO,
                "Saldo bajo: $" + String.format("%,.2f", saldoTotal)));
        return true;
    }

    public void recordatorioProgramada(TransaccionProgramada programada) {
        notificar(programada.getCliente(), new Notificacion(Notificacion.Tipo.RECORDATORIO,
                "Recordatorio " + programada.getId() + ": " + programada.describir()
                        + " el " + programada.getFechaEjecucion()));
    }

    /** Simulates sending: drains the outbox and returns the messages that were "sent". */
    public List<Notificacion> procesarPendientes() {
        List<Notificacion> enviadas = new ArrayList<>();
        while (!pendientes.isEmpty()) {
            enviadas.add(pendientes.dequeue());
        }
        return enviadas;
    }

    public int cantidadPendientes() {
        return pendientes.size();
    }

    /** History of the client, from oldest to newest (at most CAPACIDAD_HISTORIAL items). */
    public List<Notificacion> getNotificaciones(Cliente cliente) {
        List<Notificacion> lista = new ArrayList<>();
        ListaCircular<Notificacion> historial = historialPorCliente.get(cliente.getId());
        if (historial != null) {
            for (Notificacion n : historial) lista.add(n);
        }
        return lista;
    }
}
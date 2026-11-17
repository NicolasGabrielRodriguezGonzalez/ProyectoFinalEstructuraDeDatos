package co.edu.uniquindio.poo.servicios;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.edu.uniquindio.poo.estructuras.ColaPrioridad;
import co.edu.uniquindio.poo.modelo.Cliente;
import co.edu.uniquindio.poo.modelo.Monedero;
import co.edu.uniquindio.poo.modelo.TipoTransaccion;
import co.edu.uniquindio.poo.modelo.Transaccion;
import co.edu.uniquindio.poo.modelo.TransaccionProgramada;
import co.edu.uniquindio.poo.modelo.TransaccionProgramada.Periodicidad;

/**
 * PERSONA 3: scheduled transactions (single or recurring).
 * A min-heap (ColaPrioridad) ordered by execution date, plus a simulated clock.
 */
public class ServicioProgramadas {

    private static final Comparator<TransaccionProgramada> POR_FECHA =
            Comparator.comparing(TransaccionProgramada::getFechaEjecucion)
                    .thenComparing(TransaccionProgramada::getId);

    private final ServicioTransacciones transacciones;
    private final ServicioAlertas alertas;
    private final ColaPrioridad<TransaccionProgramada> cola = new ColaPrioridad<>(POR_FECHA);
    private final Map<String, TransaccionProgramada> activasPorId = new HashMap<>();
    private LocalDate fechaSimulada = LocalDate.now();

    public ServicioProgramadas(ServicioTransacciones transacciones, ServicioAlertas alertas) {
        this.transacciones = transacciones;
        this.alertas = alertas;
    }

    /** Schedules a transaction. Monederos must belong to the client for origin, and to the client for deposits. */
    public TransaccionProgramada programar(Cliente cliente, TipoTransaccion tipo,
                                           Monedero origen, Monedero destino, double monto,
                                           String categoria, LocalDate fecha, Periodicidad periodicidad) {
        validar(cliente, tipo, origen, destino, monto, fecha, periodicidad);
        String cat = (categoria == null || categoria.isBlank()) ? "Otros" : categoria.trim();
        TransaccionProgramada p = new TransaccionProgramada(cliente, tipo, origen, destino,
                monto, cat, fecha, periodicidad);
        cola.insert(p);
        activasPorId.put(p.getId(), p);
        return p;
    }

    /** Pending (active) scheduled transactions of a client, sorted by date. */
    public List<TransaccionProgramada> pendientesDe(Cliente cliente) {
        List<TransaccionProgramada> lista = new ArrayList<>();
        for (TransaccionProgramada p : cola) {
            if (p.isActiva() && p.getCliente().equals(cliente)) lista.add(p);
        }
        lista.sort(POR_FECHA);
        return lista;
    }

    /** Cancels an active scheduled transaction owned by the client. */
    public boolean cancelar(Cliente cliente, String idProgramada) {
        TransaccionProgramada p = activasPorId.get(idProgramada);
        if (p == null || !p.getCliente().equals(cliente)) return false;
        p.cancelar();
        activasPorId.remove(idProgramada);
        return true;
    }

    /**
     * Simulated clock: advances `dias` days and executes every due transaction in date order.
     * Recurring ones are re-enqueued with their next date; one-time ones are closed.
     * Returns a log line per execution.
     */
    public List<String> avanzarDias(int dias) {
        if (dias < 0) throw new OperacionInvalidaException("The clock cannot go backwards");
        fechaSimulada = fechaSimulada.plusDays(dias);
        List<String> informe = new ArrayList<>();

        while (!cola.isEmpty() && !cola.peek().getFechaEjecucion().isAfter(fechaSimulada)) {
            TransaccionProgramada p = cola.poll();
            if (!p.isActiva()) continue;          // cancelled: discarded lazily
            informe.add(ejecutar(p));

            if (p.esRecurrente()) {
                p.avanzarFecha();
                cola.insert(p);                   // next occurrence
            } else {
                p.cancelar();                     // one-time: finished
                activasPorId.remove(p.getId());
            }
        }
        return informe;
    }

    /** Sends a reminder for each active scheduled transaction due within the next `diasAnticipacion` days. */
    public int generarRecordatorios(int diasAnticipacion) {
        LocalDate limite = fechaSimulada.plusDays(diasAnticipacion);
        int enviados = 0;
        for (TransaccionProgramada p : cola) {
            LocalDate f = p.getFechaEjecucion();
            if (p.isActiva() && !f.isBefore(fechaSimulada) && !f.isAfter(limite)) {
                alertas.recordatorioProgramada(p);
                enviados++;
            }
        }
        return enviados;
    }

    public LocalDate getFechaSimulada() {
        return fechaSimulada;
    }

    // ---------------- private ----------------

    private String ejecutar(TransaccionProgramada p) {
        try {
            Transaccion t = switch (p.getTipo()) {
                case DEPOSITO -> transacciones.depositar(p.getMonederoDestino(), p.getMonto());
                case RETIRO -> transacciones.retirar(p.getMonederoOrigen(), p.getMonto());
                case TRANSFERENCIA -> transacciones.transferir(p.getMonederoOrigen(),
                        p.getMonederoDestino(), p.getMonto());
            };
            String msg = "Ejecutada " + p.getId() + " el " + fechaSimulada + ": " + t.getId();
            alertas.notificarInformativa(p.getCliente(), msg);
            return msg;
        } catch (OperacionInvalidaException ex) {
            String msg = "No se pudo ejecutar " + p.getId() + ": " + ex.getMessage();
            alertas.notificarInformativa(p.getCliente(), msg);
            return msg;
        }
    }

    private void validar(Cliente cliente, TipoTransaccion tipo, Monedero origen, Monedero destino,
                         double monto, LocalDate fecha, Periodicidad periodicidad) {
        if (cliente == null) throw new OperacionInvalidaException("The client is required");
        if (tipo == null || periodicidad == null) {
            throw new OperacionInvalidaException("The type and the periodicity are required");
        }
        if (!(monto > 0)) throw new OperacionInvalidaException("The amount must be greater than 0");
        if (fecha == null || fecha.isBefore(fechaSimulada)) {
            throw new OperacionInvalidaException("The date must be " + fechaSimulada + " or later");
        }
        switch (tipo) {
            case DEPOSITO -> requerir(destino != null && destino.getDuenio().equals(cliente),
                    "The deposit destination must be a wallet of the client");
            case RETIRO -> requerir(origen != null && origen.getDuenio().equals(cliente),
                    "The withdrawal origin must be a wallet of the client");
            case TRANSFERENCIA -> {
                requerir(origen != null && origen.getDuenio().equals(cliente),
                        "The transfer origin must be a wallet of the client");
                requerir(destino != null && destino != origen,
                        "The transfer destination must be another wallet");
            }
        }
    }

    private static void requerir(boolean condicion, String mensaje) {
        if (!condicion) throw new OperacionInvalidaException(mensaje);
    }
}
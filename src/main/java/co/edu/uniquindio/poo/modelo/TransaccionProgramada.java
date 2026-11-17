package co.edu.uniquindio.poo.modelo;

import java.time.LocalDate;

/**
 * A money operation scheduled for a future date. It can run once or repeat.
 * Owner and balance validation is done in ServicioProgramadas / ServicioTransacciones.
 */
public class TransaccionProgramada {

    public enum Periodicidad { UNICA, DIARIA, SEMANAL, MENSUAL }

    private static int contador = 0;

    private final String id;
    private final Cliente cliente;
    private final TipoTransaccion tipo;
    private final Monedero monederoOrigen;   // null for DEPOSITO
    private final Monedero monederoDestino;  // null for RETIRO
    private final double monto;
    private final String categoria;
    private final Periodicidad periodicidad;
    private LocalDate fechaEjecucion;
    private boolean activa = true;

    public TransaccionProgramada(Cliente cliente, TipoTransaccion tipo, Monedero monederoOrigen,
                                 Monedero monederoDestino, double monto, String categoria,
                                 LocalDate fechaEjecucion, Periodicidad periodicidad) {
        this.id = "P" + (++contador);
        this.cliente = cliente;
        this.tipo = tipo;
        this.monederoOrigen = monederoOrigen;
        this.monederoDestino = monederoDestino;
        this.monto = monto;
        this.categoria = categoria;
        this.fechaEjecucion = fechaEjecucion;
        this.periodicidad = periodicidad;
    }

    public boolean esRecurrente() {
        return periodicidad != Periodicidad.UNICA;
    }

    /** Moves the execution date to the next occurrence. */
    public void avanzarFecha() {
        fechaEjecucion = switch (periodicidad) {
            case DIARIA -> fechaEjecucion.plusDays(1);
            case SEMANAL -> fechaEjecucion.plusWeeks(1);
            case MENSUAL -> fechaEjecucion.plusMonths(1);
            case UNICA -> fechaEjecucion;
        };
    }

    public void cancelar() {
        this.activa = false;
    }

    public boolean isActiva() { return activa; }
    public String getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public TipoTransaccion getTipo() { return tipo; }
    public Monedero getMonederoOrigen() { return monederoOrigen; }
    public Monedero getMonederoDestino() { return monederoDestino; }
    public double getMonto() { return monto; }
    public String getCategoria() { return categoria; }
    public LocalDate getFechaEjecucion() { return fechaEjecucion; }
    public Periodicidad getPeriodicidad() { return periodicidad; }

    public String describir() {
        return switch (tipo) {
            case DEPOSITO -> "Depósito de $" + monto + " a " + monederoDestino.getNombre();
            case RETIRO -> "Retiro de $" + monto + " de " + monederoOrigen.getNombre();
            case TRANSFERENCIA -> "Transferencia de $" + monto + " a "
                    + monederoDestino.getDuenio().getNombre() + "/" + monederoDestino.getNombre();
        };
    }

    @Override
    public String toString() {
        return id + " [" + periodicidad + "] " + describir() + " (" + fechaEjecucion + ")"
                + (activa ? "" : " [CANCELADA]");
    }
}
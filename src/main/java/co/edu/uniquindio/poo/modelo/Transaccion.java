package co.edu.uniquindio.poo.modelo;

import java.time.LocalDateTime;

/**
 * Una transacción de dinero. Una transferencia es UN solo objeto que queda
 * guardado en el historial del monedero de origen y en el del destino;
 * {@link #efectoSobre(Monedero)} dice cuánto cambió el saldo de cada uno.
 */
public class Transaccion {

    private static int contador = 0;

    private final String id;
    private final TipoTransaccion tipo;
    private final double monto;
    private final double comision;
    private final LocalDateTime fecha;
    /** Monedero del que sale el dinero (null en un depósito). */
    private final Monedero origen;
    /** Monedero al que entra el dinero (null en un retiro). */
    private final Monedero destino;
    private boolean revertida;

    public Transaccion(TipoTransaccion tipo, double monto, double comision, Monedero origen, Monedero destino) {
        this.id = "T" + (++contador);
        this.tipo = tipo;
        this.monto = monto;
        this.comision = comision;
        this.origen = origen;
        this.destino = destino;
        this.fecha = LocalDateTime.now();
    }

    /**
     * Cuánto cambió esta transacción el saldo del monedero dado.
     * Una transacción revertida ya no tiene efecto.
     */
    public double efectoSobre(Monedero m) {
        if (revertida) return 0;
        double efecto = 0;
        if (m == destino) efecto += monto;
        if (m == origen) efecto -= monto + comision;
        return efecto;
    }

    /** Cliente que hizo la transacción (el dueño del origen, o del destino si es depósito). */
    public Cliente getCliente() {
        return origen != null ? origen.getDuenio() : destino.getDuenio();
    }

    /** Transferencia entre dos monederos del mismo cliente: no da puntos ni cobra comisión. */
    public boolean esInterna() {
        return origen != null && destino != null && origen.getDuenio() == destino.getDuenio();
    }

    public String getId() { return id; }
    public TipoTransaccion getTipo() { return tipo; }
    public double getMonto() { return monto; }
    public double getComision() { return comision; }
    public LocalDateTime getFecha() { return fecha; }
    public Monedero getOrigen() { return origen; }
    public Monedero getDestino() { return destino; }
    public boolean isRevertida() { return revertida; }

    public void marcarRevertida() {
        this.revertida = true;
    }

    public String descripcion() {
        switch (tipo) {
            case DEPOSITO:
                return "Depósito a " + destino.getNombre();
            case RETIRO:
                return "Retiro de " + origen.getNombre();
            default:
                return "Transferencia " + origen.getDuenio().getNombre() + "/" + origen.getNombre()
                        + " -> " + destino.getDuenio().getNombre() + "/" + destino.getNombre();
        }
    }

    @Override
    public String toString() {
        return id + " " + tipo + " $" + String.format("%.2f", monto)
                + (comision > 0 ? " (comisión $" + String.format("%.2f", comision) + ")" : "")
                + (revertida ? " [REVERTIDA]" : "");
    }
}

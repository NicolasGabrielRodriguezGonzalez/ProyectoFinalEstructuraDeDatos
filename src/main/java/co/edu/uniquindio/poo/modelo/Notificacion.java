package co.edu.uniquindio.poo.modelo;

import java.time.LocalDateTime;

/**
 * Mensaje que el sistema genera para un cliente (saldo bajo, recordatorios, etc.).
 * La Persona 3 puede agregarle campos si los necesita.
 */
public class Notificacion {

    public enum Tipo { SALDO_BAJO, RECORDATORIO, INFORMATIVA }

    private final Tipo tipo;
    private final String mensaje;
    private final LocalDateTime fecha;

    public Notificacion(Tipo tipo, String mensaje) {
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.fecha = LocalDateTime.now();
    }

    public Tipo getTipo() { return tipo; }
    public String getMensaje() { return mensaje; }
    public LocalDateTime getFecha() { return fecha; }

    @Override
    public String toString() {
        return "[" + tipo + "] " + mensaje;
    }
}

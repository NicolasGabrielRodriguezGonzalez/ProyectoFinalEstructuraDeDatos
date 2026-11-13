package co.edu.uniquindio.poo.modelo;

import co.edu.uniquindio.poo.estructuras.ListaDoble;

/**
 * Un monedero de un cliente (por ejemplo "Principal", "Ahorros", "Viajes").
 * Cada monedero tiene su saldo y su propio historial en una lista doble.
 */
public class Monedero {

    private static int contador = 0;

    private final String id;
    private final String nombre;
    private final Cliente duenio;
    private double saldo;
    private final ListaDoble<Transaccion> historial = new ListaDoble<>();

    public Monedero(String nombre, Cliente duenio) {
        this.id = "M" + (++contador);
        this.nombre = nombre;
        this.duenio = duenio;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public Cliente getDuenio() { return duenio; }
    public double getSaldo() { return saldo; }
    public ListaDoble<Transaccion> getHistorial() { return historial; }

    /** Solo lo usa ServicioTransacciones. */
    public void aplicar(Transaccion t) {
        saldo += t.efectoSobre(this);
    }

    /** Solo lo usa ServicioTransacciones al revertir: quita el efecto antes de marcarla revertida. */
    public void quitarEfecto(Transaccion t) {
        saldo -= t.efectoSobre(this);
    }

    public void registrarEnHistorial(Transaccion t) {
        historial.agregarAlFinal(t);
    }

    @Override
    public String toString() {
        return nombre + " (" + id + ") $" + String.format("%,.2f", saldo);
    }
}

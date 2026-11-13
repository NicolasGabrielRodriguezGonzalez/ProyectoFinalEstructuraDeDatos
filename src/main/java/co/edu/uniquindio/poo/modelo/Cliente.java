package co.edu.uniquindio.poo.modelo;

import co.edu.uniquindio.poo.estructuras.ListaDoble;
import co.edu.uniquindio.poo.estructuras.Pila;

/**
 * Cliente del sistema. Puede tener varios monederos (multimonedero).
 * Guarda dos contadores de puntos: los históricos (para el rango, nunca bajan
 * al canjear) y los disponibles (para canjear). Los maneja la Persona 2.
 * También tiene su pila de reversión: lo último que hizo es lo primero que se deshace.
 */
public class Cliente {

    private final String id;
    private final String nombre;
    private final ListaDoble<Monedero> monederos = new ListaDoble<>();
    /** Transacciones que este cliente puede deshacer; la última hecha queda en el tope. */
    private final Pila<Transaccion> pilaReversion = new Pila<>();

    private int puntosHistoricos;
    private int puntosDisponibles;
    private Rango rango = Rango.BRONCE;

    public Cliente(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
        monederos.agregarAlFinal(new Monedero("Principal", this));
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public ListaDoble<Monedero> getMonederos() { return monederos; }
    public Pila<Transaccion> getPilaReversion() { return pilaReversion; }

    public Monedero getMonederoPrincipal() {
        return monederos.obtenerPrimero();
    }

    public Monedero buscarMonedero(String idMonedero) {
        for (Monedero m : monederos) {
            if (m.getId().equals(idMonedero)) return m;
        }
        return null;
    }

    public Monedero agregarMonedero(String nombre) {
        Monedero m = new Monedero(nombre, this);
        monederos.agregarAlFinal(m);
        return m;
    }

    /** Suma de los saldos de todos sus monederos. */
    public double getSaldoTotal() {
        double total = 0;
        for (Monedero m : monederos) total += m.getSaldo();
        return total;
    }

    // ---- Puntos y rango (los actualiza ServicioPuntos, Persona 2) ----
    public int getPuntosHistoricos() { return puntosHistoricos; }
    public void setPuntosHistoricos(int p) { this.puntosHistoricos = p; }
    public int getPuntosDisponibles() { return puntosDisponibles; }
    public void setPuntosDisponibles(int p) { this.puntosDisponibles = p; }
    public Rango getRango() { return rango; }
    public void setRango(Rango rango) { this.rango = rango; }

    @Override
    public boolean equals(Object o) {
        return o instanceof Cliente && ((Cliente) o).id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " - " + nombre;
    }
}

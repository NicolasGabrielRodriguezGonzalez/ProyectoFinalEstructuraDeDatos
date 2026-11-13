package co.edu.uniquindio.poo.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Pila (LIFO) genérica con nodos enlazados, implementada a mano.
 * Se usa para revertir transacciones: la última que entra es la primera que se deshace.
 *
 * Complejidades: push, pop, peek, estaVacia, getTamanio : O(1)
 */
public class Pila<T> implements Iterable<T> {

    private static class Nodo<T> {
        final T dato;
        Nodo<T> siguiente;

        Nodo(T dato, Nodo<T> siguiente) {
            this.dato = dato;
            this.siguiente = siguiente;
        }
    }

    private Nodo<T> tope;
    private int tamanio;

    public void push(T dato) {
        tope = new Nodo<>(dato, tope);
        tamanio++;
    }

    public T pop() {
        if (estaVacia()) throw new NoSuchElementException("La pila está vacía");
        T dato = tope.dato;
        tope = tope.siguiente;
        tamanio--;
        return dato;
    }

    public T peek() {
        if (estaVacia()) throw new NoSuchElementException("La pila está vacía");
        return tope.dato;
    }

    public boolean estaVacia() {
        return tope == null;
    }

    public int getTamanio() {
        return tamanio;
    }

    /** Recorre desde el tope hacia el fondo. */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Nodo<T> actual = tope;

            @Override
            public boolean hasNext() { return actual != null; }

            @Override
            public T next() {
                if (actual == null) throw new NoSuchElementException();
                T dato = actual.dato;
                actual = actual.siguiente;
                return dato;
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("tope -> [");
        for (Nodo<T> n = tope; n != null; n = n.siguiente) {
            sb.append(n.dato);
            if (n.siguiente != null) sb.append(", ");
        }
        return sb.append("]").toString();
    }
}

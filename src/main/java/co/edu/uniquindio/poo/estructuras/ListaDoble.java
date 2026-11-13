package co.edu.uniquindio.poo.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Lista doblemente enlazada genérica, implementada a mano.
 * Se usa para el historial de transacciones de cada monedero y para los
 * monederos de cada cliente.
 *
 * Complejidades:
 *   agregarAlInicio / agregarAlFinal / eliminarPrimero / eliminarUltimo : O(1)
 *   obtener(i) / eliminar(dato) / contiene : O(n)
 *   recorrido hacia adelante o hacia atrás : O(n)
 */
public class ListaDoble<T> implements Iterable<T> {

    /** Nodo de la lista. Es público para que la verificación recursiva pueda recorrer nodo por nodo. */
    public static class Nodo<T> {
        private T dato;
        private Nodo<T> anterior;
        private Nodo<T> siguiente;

        Nodo(T dato) {
            this.dato = dato;
        }

        public T getDato() { return dato; }
        public Nodo<T> getAnterior() { return anterior; }
        public Nodo<T> getSiguiente() { return siguiente; }
    }

    private Nodo<T> cabeza;
    private Nodo<T> cola;
    private int tamanio;

    public boolean estaVacia() {
        return tamanio == 0;
    }

    public int getTamanio() {
        return tamanio;
    }

    public Nodo<T> getCabeza() {
        return cabeza;
    }

    public Nodo<T> getCola() {
        return cola;
    }

    public void agregarAlInicio(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (estaVacia()) {
            cabeza = cola = nuevo;
        } else {
            nuevo.siguiente = cabeza;
            cabeza.anterior = nuevo;
            cabeza = nuevo;
        }
        tamanio++;
    }

    public void agregarAlFinal(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (estaVacia()) {
            cabeza = cola = nuevo;
        } else {
            cola.siguiente = nuevo;
            nuevo.anterior = cola;
            cola = nuevo;
        }
        tamanio++;
    }

    public T obtenerPrimero() {
        if (estaVacia()) throw new NoSuchElementException("La lista está vacía");
        return cabeza.dato;
    }

    public T obtenerUltimo() {
        if (estaVacia()) throw new NoSuchElementException("La lista está vacía");
        return cola.dato;
    }

    public T obtener(int indice) {
        return nodoEn(indice).dato;
    }

    public T eliminarPrimero() {
        if (estaVacia()) throw new NoSuchElementException("La lista está vacía");
        Nodo<T> n = cabeza;
        desenlazar(n);
        return n.dato;
    }

    public T eliminarUltimo() {
        if (estaVacia()) throw new NoSuchElementException("La lista está vacía");
        Nodo<T> n = cola;
        desenlazar(n);
        return n.dato;
    }

    /** Elimina la primera aparición del dato (comparando con equals). Devuelve true si lo encontró. */
    public boolean eliminar(T dato) {
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            if (iguales(n.dato, dato)) {
                desenlazar(n);
                return true;
            }
        }
        return false;
    }

    public boolean contiene(T dato) {
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            if (iguales(n.dato, dato)) return true;
        }
        return false;
    }

    public void limpiar() {
        cabeza = cola = null;
        tamanio = 0;
    }

    /** Iterador de atrás hacia adelante (del más reciente al más viejo en el historial). */
    public Iterable<T> alReves() {
        return () -> new Iterator<T>() {
            private Nodo<T> actual = cola;

            @Override
            public boolean hasNext() { return actual != null; }

            @Override
            public T next() {
                if (actual == null) throw new NoSuchElementException();
                T dato = actual.dato;
                actual = actual.anterior;
                return dato;
            }
        };
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Nodo<T> actual = cabeza;

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
        StringBuilder sb = new StringBuilder("[");
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            sb.append(n.dato);
            if (n.siguiente != null) sb.append(" <-> ");
        }
        return sb.append("]").toString();
    }

    // ---------------- auxiliares ----------------

    private Nodo<T> nodoEn(int indice) {
        if (indice < 0 || indice >= tamanio) {
            throw new IndexOutOfBoundsException("Índice " + indice + ", tamaño " + tamanio);
        }
        // Se recorre desde el extremo más cercano.
        if (indice < tamanio / 2) {
            Nodo<T> n = cabeza;
            for (int i = 0; i < indice; i++) n = n.siguiente;
            return n;
        }
        Nodo<T> n = cola;
        for (int i = tamanio - 1; i > indice; i--) n = n.anterior;
        return n;
    }

    private void desenlazar(Nodo<T> n) {
        if (n.anterior == null) cabeza = n.siguiente;
        else n.anterior.siguiente = n.siguiente;

        if (n.siguiente == null) cola = n.anterior;
        else n.siguiente.anterior = n.anterior;

        n.anterior = n.siguiente = null;
        tamanio--;
    }

    private static boolean iguales(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }
}

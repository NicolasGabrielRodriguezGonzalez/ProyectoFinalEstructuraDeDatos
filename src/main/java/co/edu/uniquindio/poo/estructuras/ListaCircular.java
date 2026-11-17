package co.edu.uniquindio.poo.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Fixed-capacity circular buffer. When it is full, adding a new element
 * overwrites the oldest one. Logical index 0 is the oldest element.
 *
 * Complexity: add O(1), get(i) O(1), iteration O(n), space O(capacity).
 */
public class ListaCircular<T> implements Iterable<T> {

    private final Object[] buffer;
    private int head;   // physical index of the oldest element
    private int size;   // number of stored elements

    public ListaCircular(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be greater than 0");
        this.buffer = new Object[capacity];
    }

    /** Adds an element as the newest one. If the buffer is full, overwrites the oldest. */
    public void add(T element) {
        int tail = (head + size) % buffer.length;   // next free slot, or the oldest slot when full
        buffer[tail] = element;
        if (size == buffer.length) {
            head = (head + 1) % buffer.length;      // the oldest was overwritten: head moves forward
        } else {
            size++;
        }
    }

    /** Returns the element at logical position index (0 = oldest). */
    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index " + index + ", size " + size);
        }
        return (T) buffer[(head + index) % buffer.length];
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return buffer.length;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isFull() {
        return size == buffer.length;
    }

    public void clear() {
        for (int i = 0; i < buffer.length; i++) buffer[i] = null;
        head = 0;
        size = 0;
    }

    /** Iterates from oldest to newest. */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int served = 0;

            @Override
            public boolean hasNext() {
                return served < size;
            }

            @Override
            public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                return get(served++);
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            sb.append(get(i));
            if (i < size - 1) sb.append(", ");
        }
        return sb.append("]").toString();
    }
}
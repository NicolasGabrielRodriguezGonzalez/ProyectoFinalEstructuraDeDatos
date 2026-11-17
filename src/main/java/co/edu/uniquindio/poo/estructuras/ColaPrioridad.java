package co.edu.uniquindio.poo.estructuras;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Generic min-heap (priority queue) backed by a dynamic array.
 * The smallest element, according to the comparator, is always at index 0.
 * Parent of i: (i - 1) / 2. Children of i: 2i + 1 and 2i + 2.
 *
 * Complexity: insert O(log n), poll O(log n), peek O(1), size O(1).
 * Iteration visits elements in heap (array) order, NOT sorted order.
 */
public class ColaPrioridad<T> implements Iterable<T> {

    private static final int INITIAL_CAPACITY = 16;

    private Object[] heap;
    private int size;
    private final Comparator<? super T> comparator;

    public ColaPrioridad(Comparator<? super T> comparator) {
        this.comparator = comparator;
        this.heap = new Object[INITIAL_CAPACITY];
    }

    /** Inserts an element at the end of the array, then moves it up until the heap property holds. */
    public void insert(T element) {
        if (element == null) throw new IllegalArgumentException("Null elements are not allowed");
        ensureCapacity();
        heap[size] = element;
        siftUp(size);
        size++;
    }

    /** Returns the smallest element without removing it. */
    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) throw new NoSuchElementException("The priority queue is empty");
        return (T) heap[0];
    }

    /** Removes and returns the smallest element. */
    @SuppressWarnings("unchecked")
    public T poll() {
        if (isEmpty()) throw new NoSuchElementException("The priority queue is empty");
        T min = (T) heap[0];
        size--;
        heap[0] = heap[size];   // move the last element to the root
        heap[size] = null;      // avoid keeping a reference (memory leak)
        if (!isEmpty()) siftDown(0);
        return min;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    // ---------------- helpers ----------------

    /** Moves the element at index up while it is smaller than its parent. */
    private void siftUp(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;
            if (compare(index, parent) >= 0) break;
            swap(index, parent);
            index = parent;
        }
    }

    /** Moves the element at index down while it is larger than its smallest child. */
    private void siftDown(int index) {
        while (true) {
            int left = 2 * index + 1;
            int right = left + 1;
            int smallest = index;
            if (left < size && compare(left, smallest) < 0) smallest = left;
            if (right < size && compare(right, smallest) < 0) smallest = right;
            if (smallest == index) break;
            swap(index, smallest);
            index = smallest;
        }
    }

    @SuppressWarnings("unchecked")
    private int compare(int i, int j) {
        return comparator.compare((T) heap[i], (T) heap[j]);
    }

    private void swap(int i, int j) {
        Object tmp = heap[i];
        heap[i] = heap[j];
        heap[j] = tmp;
    }

    private void ensureCapacity() {
        if (size == heap.length) {
            heap = Arrays.copyOf(heap, heap.length * 2);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public T next() {
                if (index >= size) throw new NoSuchElementException();
                return (T) heap[index++];
            }
        };
    }
}
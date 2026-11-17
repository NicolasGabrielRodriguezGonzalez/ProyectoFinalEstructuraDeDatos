package co.edu.uniquindio.poo.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * FIFO queue implemented with a singly linked list.
 * front = next node to dequeue, rear = last node enqueued.
 *
 * Complexity: enqueue O(1), dequeue O(1), peek O(1), size O(1),
 * iteration O(n) time and O(1) extra space.
 */
public class Cola<T> implements Iterable<T> {

    private static class Node<T> {
        private final T data;
        private Node<T> next;

        Node(T data) {
            this.data = data;
        }
    }

    private Node<T> front;
    private Node<T> rear;
    private int size;

    /** Adds an element at the rear of the queue. */
    public void enqueue(T data) {
        Node<T> newNode = new Node<>(data);
        if (isEmpty()) {
            front = newNode;          // first element: front and rear point to the same node
        } else {
            rear.next = newNode;      // link the current last node to the new one
        }
        rear = newNode;
        size++;
    }

    /** Removes and returns the element at the front of the queue. */
    public T dequeue() {
        if (isEmpty()) throw new NoSuchElementException("The queue is empty");
        T data = front.data;
        front = front.next;
        if (front == null) {
            rear = null;              // the queue became empty: reset rear too
        }
        size--;
        return data;
    }

    /** Returns the front element without removing it. */
    public T peek() {
        if (isEmpty()) throw new NoSuchElementException("The queue is empty");
        return front.data;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public void clear() {
        front = null;
        rear = null;
        size = 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node<T> current = front;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public T next() {
                if (current == null) throw new NoSuchElementException();
                T data = current.data;
                current = current.next;
                return data;
            }
        };
    }
}
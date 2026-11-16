package co.edu.uniquindio.poo.estructuras;

import java.util.ArrayList;
import java.util.List;

/** AVL: árbol balanceado de clientes por puntos históricos. Sirve para consultar por rango. */
public class ArbolRangosAVL {

    private static class Nodo {
        int puntos;
        List<String> ids = new ArrayList<>();
        int altura = 1;
        Nodo izq, der;

        Nodo(int puntos, String id) {
            this.puntos = puntos;
            this.ids.add(id);
        }
    }

    private Nodo raiz;

    private int altura(Nodo n) { return n == null ? 0 : n.altura; }

    private void actualizar(Nodo n) {
        n.altura = 1 + Math.max(altura(n.izq), altura(n.der));
    }

    private int balance(Nodo n) { return n == null ? 0 : altura(n.izq) - altura(n.der); }

    private Nodo rotarDerecha(Nodo y) {
        Nodo x = y.izq;
        y.izq = x.der;
        x.der = y;
        actualizar(y);
        actualizar(x);
        return x;
    }

    private Nodo rotarIzquierda(Nodo x) {
        Nodo y = x.der;
        x.der = y.izq;
        y.izq = x;
        actualizar(x);
        actualizar(y);
        return y;
    }

    private Nodo balancear(Nodo n) {
        actualizar(n);
        int b = balance(n);
        if (b > 1) {
            if (balance(n.izq) < 0) n.izq = rotarIzquierda(n.izq);
            return rotarDerecha(n);
        }
        if (b < -1) {
            if (balance(n.der) > 0) n.der = rotarDerecha(n.der);
            return rotarIzquierda(n);
        }
        return n;
    }

    public void insertar(int puntos, String idCliente) {
        raiz = insertar(raiz, puntos, idCliente);
    }

    private Nodo insertar(Nodo n, int p, String id) {
        if (n == null) return new Nodo(p, id);
        if (p < n.puntos) n.izq = insertar(n.izq, p, id);
        else if (p > n.puntos) n.der = insertar(n.der, p, id);
        else {
            if (!n.ids.contains(id)) n.ids.add(id);
            return n;
        }
        return balancear(n);
    }

    public void eliminar(int puntos, String idCliente) {
        raiz = eliminar(raiz, puntos, idCliente);
    }

    private Nodo eliminar(Nodo n, int p, String id) {
        if (n == null) return null;
        if (p < n.puntos) {
            n.izq = eliminar(n.izq, p, id);
        } else if (p > n.puntos) {
            n.der = eliminar(n.der, p, id);
        } else {
            n.ids.remove(id);
            if (!n.ids.isEmpty()) return n;
            return quitarClave(n, p);
        }
        return balancear(n);
    }

    private Nodo quitarClave(Nodo n, int p) {
        if (n == null) return null;
        if (p < n.puntos) {
            n.izq = quitarClave(n.izq, p);
        } else if (p > n.puntos) {
            n.der = quitarClave(n.der, p);
        } else {
            if (n.izq == null) return n.der;
            if (n.der == null) return n.izq;
            Nodo sucesor = n.der;
            while (sucesor.izq != null) sucesor = sucesor.izq;
            n.puntos = sucesor.puntos;
            n.ids = sucesor.ids;
            n.der = quitarClave(n.der, sucesor.puntos);
        }
        return balancear(n);
    }

    /** Clientes cuyos puntos están entre min y max (ambos incluidos). */
    public List<String> clientesEnRango(int min, int max) {
        List<String> lista = new ArrayList<>();
        buscar(raiz, min, max, lista);
        return lista;
    }

    private void buscar(Nodo n, int min, int max, List<String> lista) {
        if (n == null) return;
        if (min < n.puntos) buscar(n.izq, min, max, lista);
        if (n.puntos >= min && n.puntos <= max) lista.addAll(n.ids);
        if (max > n.puntos) buscar(n.der, min, max, lista);
    }

    public int altura() { return altura(raiz); }
}
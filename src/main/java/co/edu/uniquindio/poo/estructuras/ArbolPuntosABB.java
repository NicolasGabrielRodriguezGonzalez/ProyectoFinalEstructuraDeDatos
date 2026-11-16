package co.edu.uniquindio.poo.estructuras;

import java.util.ArrayList;
import java.util.List;

/** ABB: cada nodo es una cantidad de puntos y guarda los clientes que la tienen. */
public class ArbolPuntosABB {

    public record Entrada(String idCliente, int puntos) {}

    private static class Nodo {
        int puntos;
        List<String> ids = new ArrayList<>();
        Nodo izq, der;

        Nodo(int puntos, String id) {
            this.puntos = puntos;
            this.ids.add(id);
        }
    }

    private Nodo raiz;

    public void insertar(int puntos, String idCliente) {
        raiz = insertar(raiz, puntos, idCliente);
    }

    private Nodo insertar(Nodo n, int p, String id) {
        if (n == null) return new Nodo(p, id);
        if (p < n.puntos) n.izq = insertar(n.izq, p, id);
        else if (p > n.puntos) n.der = insertar(n.der, p, id);
        else if (!n.ids.contains(id)) n.ids.add(id);
        return n;
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
            if (n.ids.isEmpty()) return quitarClave(n, p);
        }
        return n;
    }

    /** Borra el nodo completo con esos puntos. */
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
        return n;
    }

    /** Ranking de mayor a menor puntos. */
    public List<Entrada> ranking() {
        List<Entrada> lista = new ArrayList<>();
        recorrerDescendente(raiz, lista);
        return lista;
    }

    private void recorrerDescendente(Nodo n, List<Entrada> lista) {
        if (n == null) return;
        recorrerDescendente(n.der, lista);
        for (String id : n.ids) lista.add(new Entrada(id, n.puntos));
        recorrerDescendente(n.izq, lista);
    }

    /** Los primeros N del ranking. */
    public List<Entrada> top(int n) {
        List<Entrada> todos = ranking();
        return todos.subList(0, Math.min(n, todos.size()));
    }
}
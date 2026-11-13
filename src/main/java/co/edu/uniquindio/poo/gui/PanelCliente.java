package co.edu.uniquindio.poo.gui;

import co.edu.uniquindio.poo.modelo.Cliente;

/**
 * Toda pestaña de la ventana principal implementa esta interfaz.
 * La ventana llama a mostrarCliente(...) cada vez que se elige otro cliente
 * o cuando cambian los datos (después de una operación), para que la pestaña se refresque.
 * El cliente puede ser null si todavía no hay ninguno seleccionado.
 */
public interface PanelCliente {
    void mostrarCliente(Cliente cliente);
}

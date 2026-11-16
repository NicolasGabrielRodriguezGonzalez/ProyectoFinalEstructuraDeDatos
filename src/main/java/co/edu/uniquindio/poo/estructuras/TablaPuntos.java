package co.edu.uniquindio.poo.estructuras;

import co.edu.uniquindio.poo.modelo.TipoTransaccion;
import co.edu.uniquindio.poo.servicios.ReglasPuntos;
import java.util.HashMap;

public class TablaPuntos {

    private HashMap<String, Integer> puntos = new HashMap<>();

    public void sumar(String idCliente, int cantidad) {
        puntos.put(idCliente, obtener(idCliente) + cantidad);
    }

    public void restar(String idCliente, int cantidad) {
        puntos.put(idCliente, Math.max(0, obtener(idCliente) - cantidad));
    }

    public int obtener(String idCliente) {
        return puntos.getOrDefault(idCliente, 0);
    }

    public int registrar(String idCliente, TipoTransaccion tipo, double monto) {
        int ganados = ReglasPuntos.calcularPuntos(tipo, monto);
        sumar(idCliente, ganados);
        return ganados;
    }
}
package co.edu.uniquindio.poo.servicios;

import co.edu.uniquindio.poo.modelo.Rango;
import co.edu.uniquindio.poo.modelo.TipoTransaccion;

public class ReglasPuntos {

    // Puntos por una transacción: 1, 2 o 3 por cada 100
    public static int calcularPuntos(TipoTransaccion tipo, double monto) {
        int bloques = (int) (monto / 100);

        switch (tipo) {
            case DEPOSITO:      return bloques * 1;
            case RETIRO:        return bloques * 2;
            case TRANSFERENCIA: return bloques * 3;
            default:            return 0;
        }
    }

    // El rango ya lo calcula el enum Rango
    public static Rango calcularRango(int puntosHistoricos) {
        return Rango.segunPuntos(puntosHistoricos);
    }
}
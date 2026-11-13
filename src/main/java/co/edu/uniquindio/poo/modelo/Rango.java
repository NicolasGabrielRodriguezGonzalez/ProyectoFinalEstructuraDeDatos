package co.edu.uniquindio.poo.modelo;

/**
 * Rango del cliente según sus puntos históricos.
 * La Persona 2 decide cuándo se recalcula (ServicioPuntos); aquí solo están los límites.
 */
public enum Rango {
    BRONCE(0, 500),
    PLATA(501, 1000),
    ORO(1001, 5000),
    PLATINO(5001, Integer.MAX_VALUE);

    private final int minimo;
    private final int maximo;

    Rango(int minimo, int maximo) {
        this.minimo = minimo;
        this.maximo = maximo;
    }

    public int getMinimo() { return minimo; }
    public int getMaximo() { return maximo; }

    public static Rango segunPuntos(int puntosHistoricos) {
        for (Rango r : values()) {
            if (puntosHistoricos >= r.minimo && puntosHistoricos <= r.maximo) return r;
        }
        return BRONCE;
    }
}

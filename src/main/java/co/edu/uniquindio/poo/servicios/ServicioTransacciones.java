package co.edu.uniquindio.poo.servicios;

import java.util.Map;

import co.edu.uniquindio.poo.estructuras.ListaDoble;
import co.edu.uniquindio.poo.estructuras.Pila;
import co.edu.uniquindio.poo.modelo.Cliente;
import co.edu.uniquindio.poo.modelo.Monedero;
import co.edu.uniquindio.poo.modelo.Notificacion;
import co.edu.uniquindio.poo.modelo.TipoTransaccion;
import co.edu.uniquindio.poo.modelo.Transaccion;

/**
 * PERSONA 1: el dinero. Depósitos, retiros, transferencias, reversión
 * (con la pila de cada cliente) y verificación de integridad (recursiva).
 *
 * Cada operación exitosa:
 *   1. modifica los saldos,
 *   2. queda en el historial (ListaDoble) de cada monedero involucrado,
 *   3. queda en la pila de reversión del cliente que la hizo,
 *   4. avisa a puntos (P2), análisis (P3) y, si aplica, alertas (P3).
 */
public class ServicioTransacciones {

    /** Por debajo de este saldo se envía una alerta de saldo bajo después de un retiro o transferencia. */
    public static final double UMBRAL_SALDO_BAJO = 10_000;

    private final Map<String, Cliente> clientes;
    private final ServicioPuntos puntos;
    private final ServicioAlertas alertas;
    private final ServicioAnalisis analisis;

    public ServicioTransacciones(Map<String, Cliente> clientes, ServicioPuntos puntos,
                                 ServicioAlertas alertas, ServicioAnalisis analisis) {
        this.clientes = clientes;
        this.puntos = puntos;
        this.alertas = alertas;
        this.analisis = analisis;
    }

    // ===================================================================
    // Operaciones
    // ===================================================================

    public Transaccion depositar(Monedero destino, double monto) {
        validarMonto(monto);
        validarMonederoActivo(destino);
        Transaccion t = new Transaccion(TipoTransaccion.DEPOSITO, monto, 0, null, destino);
        destino.aplicar(t);
        destino.registrarEnHistorial(t);
        terminar(t, destino.getDuenio());
        return t;
    }

    public Transaccion retirar(Monedero origen, double monto) {
        validarMonto(monto);
        validarMonederoActivo(origen);
        Cliente cliente = origen.getDuenio();
        double comision = puntos.tieneRetirosGratis(cliente) ? 0 : puntos.calcularComision(cliente, monto);
        validarSaldo(origen, monto + comision);

        Transaccion t = new Transaccion(TipoTransaccion.RETIRO, monto, comision, origen, null);
        origen.aplicar(t);
        origen.registrarEnHistorial(t);
        terminar(t, cliente);
        revisarSaldoBajo(origen);
        return t;
    }

    /**
     * Transferencia entre dos monederos. Sirve tanto entre clientes distintos
     * como entre dos monederos del mismo cliente (multimonedero).
     */
    public Transaccion transferir(Monedero origen, Monedero destino, double monto) {
        validarMonto(monto);
        validarMonederoActivo(origen);
        validarMonederoActivo(destino);
        if (origen == destino) {
            throw new OperacionInvalidaException("El monedero de origen y el de destino son el mismo");
        }
        Cliente cliente = origen.getDuenio();
        // Entre monederos del mismo cliente no se cobra comisión.
        double comision = origen.getDuenio() == destino.getDuenio() ? 0 : puntos.calcularComision(cliente, monto);
        validarSaldo(origen, monto + comision);

        Transaccion t = new Transaccion(TipoTransaccion.TRANSFERENCIA, monto, comision, origen, destino);
        origen.aplicar(t);
        destino.aplicar(t);
        origen.registrarEnHistorial(t);
        destino.registrarEnHistorial(t);
        terminar(t, cliente);
        revisarSaldoBajo(origen);
        return t;
    }

    /**
     * Deshace la última transacción del cliente (pop de su pila).
     * Si el dinero ya no está (por ejemplo, se depositó y luego se gastó),
     * no se puede revertir y la transacción vuelve a la pila.
     */
    public Transaccion revertirUltima(Cliente cliente) {
        Pila<Transaccion> pila = cliente.getPilaReversion();
        if (pila.estaVacia()) {
            throw new OperacionInvalidaException("No hay transacciones para revertir");
        }
        Transaccion t = pila.pop();
        // Para revertir, el destino debe tener todavía el dinero que recibió.
        Monedero destino = t.getDestino();
        if (destino != null && destino.getSaldo() < t.getMonto()) {
            pila.push(t);
            throw new OperacionInvalidaException("No se puede revertir " + t.getId()
                    + ": el monedero " + destino.getNombre() + " ya no tiene $" + formato(t.getMonto()));
        }
        if (t.getOrigen() != null) t.getOrigen().quitarEfecto(t);
        if (destino != null) destino.quitarEfecto(t);
        t.marcarRevertida();
        if (!t.esInterna()) puntos.revertir(t);
        return t;
    }

    // ===================================================================
    // Verificación de integridad (recursiva)
    // ===================================================================

    /** Resultado de verificar un monedero. */
    public static class ResultadoVerificacion {
        private final Monedero monedero;
        private final double saldoGuardado;
        private final double saldoCalculado;
        private final ListaDoble<String> errores;

        ResultadoVerificacion(Monedero monedero, double saldoCalculado, ListaDoble<String> errores) {
            this.monedero = monedero;
            this.saldoGuardado = monedero.getSaldo();
            this.saldoCalculado = saldoCalculado;
            this.errores = errores;
        }

        public Monedero getMonedero() { return monedero; }
        public double getSaldoGuardado() { return saldoGuardado; }
        public double getSaldoCalculado() { return saldoCalculado; }
        public ListaDoble<String> getErrores() { return errores; }
        public boolean esValido() { return errores.estaVacia(); }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(monedero.getDuenio().getNombre()).append(" / ").append(monedero.getNombre())
              .append(": guardado $").append(formato(saldoGuardado))
              .append(", calculado $").append(formato(saldoCalculado))
              .append(esValido() ? "  OK" : "  CON ERRORES");
            for (String e : errores) sb.append("\n   - ").append(e);
            return sb.toString();
        }
    }

    public ResultadoVerificacion verificarIntegridad(Monedero m) {
        ListaDoble<String> errores = new ListaDoble<>();
        ListaDoble.Nodo<Transaccion> primero = m.getHistorial().getCabeza();

        double calculado = calcularSaldo(primero, m);
        validarTransacciones(primero, m, 0, errores);

        if (Math.abs(calculado - m.getSaldo()) > 0.001) {
            errores.agregarAlFinal("El saldo guardado ($" + formato(m.getSaldo())
                    + ") no coincide con el recalculado ($" + formato(calculado) + ")");
        }
        return new ResultadoVerificacion(m, calculado, errores);
    }

    /** Verifica todos los monederos de un cliente. */
    public ListaDoble<ResultadoVerificacion> verificarIntegridad(Cliente c) {
        ListaDoble<ResultadoVerificacion> resultados = new ListaDoble<>();
        for (Monedero m : c.getMonederos()) resultados.agregarAlFinal(verificarIntegridad(m));
        return resultados;
    }

    /**
     * Recursión 1: recalcula el saldo sumando el efecto de cada nodo.
     *   caso base:       nodo == null            -> 0
     *   caso recursivo:  efecto(nodo) + saldo(nodo.siguiente)
     * Complejidad O(n) en tiempo y O(n) en la pila de llamadas.
     */
    double calcularSaldo(ListaDoble.Nodo<Transaccion> nodo, Monedero m) {
        if (nodo == null) return 0;
        return nodo.getDato().efectoSobre(m) + calcularSaldo(nodo.getSiguiente(), m);
    }

    /**
     * Recursión 2: recorre el historial en orden cronológico llevando el saldo
     * acumulado, y valida cada transacción (monto > 0, cuentas existentes,
     * el monedero participa en ella, saldo nunca negativo).
     */
    void validarTransacciones(ListaDoble.Nodo<Transaccion> nodo, Monedero m,
                              double acumulado, ListaDoble<String> errores) {
        if (nodo == null) return;
        Transaccion t = nodo.getDato();

        if (t.getMonto() <= 0) {
            errores.agregarAlFinal(t.getId() + ": monto no positivo");
        }
        if (t.getOrigen() != m && t.getDestino() != m) {
            errores.agregarAlFinal(t.getId() + ": está en el historial pero no involucra a este monedero");
        }
        if (t.getOrigen() != null && !existe(t.getOrigen())) {
            errores.agregarAlFinal(t.getId() + ": el monedero de origen no existe en el sistema");
        }
        if (t.getDestino() != null && !existe(t.getDestino())) {
            errores.agregarAlFinal(t.getId() + ": el monedero de destino no existe en el sistema");
        }
        double nuevo = acumulado + t.efectoSobre(m);
        if (nuevo < -0.001) {
            errores.agregarAlFinal(t.getId() + ": el saldo queda negativo ($" + formato(nuevo) + ")");
        }
        validarTransacciones(nodo.getSiguiente(), m, nuevo, errores);
    }

    // ===================================================================
    // Auxiliares
    // ===================================================================

    private void terminar(Transaccion t, Cliente quienLaHizo) {
        quienLaHizo.getPilaReversion().push(t);
        // Mover dinero entre sus propios monederos no da puntos (si no, se podrían "fabricar").
        if (!t.esInterna()) puntos.registrar(quienLaHizo, t.getTipo(), t.getMonto());
        analisis.registrar(t);
    }

    private void revisarSaldoBajo(Monedero m) {
        if (m.getSaldo() < UMBRAL_SALDO_BAJO) {
            alertas.notificar(m.getDuenio(), new Notificacion(Notificacion.Tipo.SALDO_BAJO,
                    "Saldo bajo en " + m.getNombre() + ": $" + formato(m.getSaldo())));
        }
    }

    private boolean existe(Monedero m) {
        Cliente c = clientes.get(m.getDuenio().getId());
        return c != null && c.getMonederos().contiene(m);
    }

    private void validarMonederoActivo(Monedero m) {
        if (m == null) throw new OperacionInvalidaException("El monedero no existe");
        if (!existe(m)) throw new OperacionInvalidaException("El monedero " + m.getId() + " no está registrado");
    }

    private static void validarMonto(double monto) {
        if (!(monto > 0)) throw new OperacionInvalidaException("El monto debe ser mayor que 0");
    }

    private static void validarSaldo(Monedero m, double necesario) {
        if (m.getSaldo() < necesario) {
            throw new OperacionInvalidaException("Saldo insuficiente en " + m.getNombre()
                    + ": tiene $" + formato(m.getSaldo()) + " y necesita $" + formato(necesario));
        }
    }

    static String formato(double valor) {
        return String.format("%,.2f", valor);
    }
}

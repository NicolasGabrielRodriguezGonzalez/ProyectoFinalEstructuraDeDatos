package co.edu.uniquindio.poo.servicios;

import co.edu.uniquindio.poo.estructuras.ArbolPuntosABB;
import co.edu.uniquindio.poo.estructuras.ArbolRangosAVL;
import co.edu.uniquindio.poo.estructuras.TablaPuntos;
import co.edu.uniquindio.poo.modelo.Cliente;
import co.edu.uniquindio.poo.modelo.Monedero;
import co.edu.uniquindio.poo.modelo.Rango;
import co.edu.uniquindio.poo.modelo.TipoTransaccion;
import co.edu.uniquindio.poo.modelo.Transaccion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PERSONA 2: puntos, rangos y beneficios.
 * Usa: tabla hash (puntos por cliente), ABB (ranking) y AVL (clientes por rango).
 */
public class ServicioPuntos {

    /** Beneficios que se pueden canjear con puntos disponibles. */
    public enum Beneficio {
        DESCUENTO_TRANSFERENCIAS(100, "10% menos de comisión en transferencias"),
        RETIROS_GRATIS(500, "Un mes sin cargos por retiros"),
        BONO_SALDO(1000, "Bono de 50 unidades en el monedero principal");

        private final int costo;
        private final String descripcion;

        Beneficio(int costo, String descripcion) {
            this.costo = costo;
            this.descripcion = descripcion;
        }

        public int getCosto() { return costo; }
        public String getDescripcion() { return descripcion; }
    }

    // Comisión base (AJUSTAR con el grupo o el profesor)
    private static final double TASA_BASE = 0.01;
    private static final double DESCUENTO_BENEFICIO = 0.10;
    private static final double MONTO_BONO = 50;

    private final TablaPuntos tabla = new TablaPuntos();
    private final ArbolPuntosABB ranking = new ArbolPuntosABB();
    private final ArbolRangosAVL arbolRangos = new ArbolRangosAVL();

    private final Set<String> conDescuentoTransferencias = new HashSet<>();
    private final HashMap<String, LocalDate> retirosGratisHasta = new HashMap<>();
    private final HashMap<String, List<String>> historialPuntos = new HashMap<>();

    /** Se llama después de cada transacción exitosa. Suma puntos y actualiza el rango. */
    public void registrar(Cliente cliente, TipoTransaccion tipo, double monto) {
        int ganados = ReglasPuntos.calcularPuntos(tipo, monto);
        if (ganados <= 0) return;

        int antes = cliente.getPuntosHistoricos();
        int despues = antes + ganados;

        tabla.sumar(cliente.getId(), ganados);
        cliente.setPuntosHistoricos(despues);
        cliente.setPuntosDisponibles(cliente.getPuntosDisponibles() + ganados);

        actualizarArboles(cliente, antes, despues);
        cliente.setRango(ReglasPuntos.calcularRango(despues));
        anotar(cliente, tipo + " $" + monto + " -> +" + ganados + " puntos");
    }

    /** Se llama al deshacer una transacción. Resta los puntos que esa transacción dio. */
    public void revertir(Transaccion transaccion) {
        if (transaccion.esInterna()) return; // las internas no dan puntos

        Cliente cliente = transaccion.getCliente();
        int puntos = ReglasPuntos.calcularPuntos(transaccion.getTipo(), transaccion.getMonto());
        if (puntos <= 0) return;

        int antes = cliente.getPuntosHistoricos();
        int despues = Math.max(0, antes - puntos);

        tabla.restar(cliente.getId(), puntos);
        cliente.setPuntosHistoricos(despues);
        cliente.setPuntosDisponibles(Math.max(0, cliente.getPuntosDisponibles() - puntos));

        actualizarArboles(cliente, antes, despues);
        cliente.setRango(ReglasPuntos.calcularRango(despues));
        anotar(cliente, "Reversión " + transaccion.getId() + " -> -" + puntos + " puntos");
    }

    /** Comisión de un retiro o transferencia, con descuento por rango y por beneficio canjeado. */
    public double calcularComision(Cliente cliente, double monto) {
        double tasa = TASA_BASE * (1 - descuentoPorRango(cliente.getRango()));
        if (conDescuentoTransferencias.contains(cliente.getId())) {
            tasa *= (1 - DESCUENTO_BENEFICIO);
        }
        return monto * tasa;
    }

    /** true si el cliente tiene activo "mes sin cargos por retiros". */
    public boolean tieneRetirosGratis(Cliente cliente) {
        LocalDate hasta = retirosGratisHasta.get(cliente.getId());
        return hasta != null && !LocalDate.now().isAfter(hasta);
    }

    /** Canjea puntos disponibles por un beneficio. Devuelve false si no alcanzan. */
    public boolean canjear(Cliente cliente, Beneficio beneficio) {
        if (cliente.getPuntosDisponibles() < beneficio.getCosto()) return false;

        cliente.setPuntosDisponibles(cliente.getPuntosDisponibles() - beneficio.getCosto());

        switch (beneficio) {
            case DESCUENTO_TRANSFERENCIAS:
                conDescuentoTransferencias.add(cliente.getId());
                break;
            case RETIROS_GRATIS:
                LocalDate hoy = LocalDate.now();
                LocalDate actual = retirosGratisHasta.get(cliente.getId());
                LocalDate base = (actual != null && actual.isAfter(hoy)) ? actual : hoy;
                retirosGratisHasta.put(cliente.getId(), base.plusMonths(1));
                break;
            case BONO_SALDO:
                Monedero principal = cliente.getMonederoPrincipal();
                Transaccion bono = new Transaccion(TipoTransaccion.DEPOSITO, MONTO_BONO, 0, null, principal);
                principal.aplicar(bono);
                principal.registrarEnHistorial(bono);
                break;
        }
        anotar(cliente, "Canje: " + beneficio.getDescripcion() + " -> -" + beneficio.getCosto() + " puntos");
        return true;
    }

    // ---- Consultas para la pantalla ----

    public boolean tieneDescuentoTransferencias(Cliente cliente) {
        return conDescuentoTransferencias.contains(cliente.getId());
    }

    public LocalDate getRetirosGratisHasta(Cliente cliente) {
        return retirosGratisHasta.get(cliente.getId());
    }

    public List<ArbolPuntosABB.Entrada> getRanking(int n) {
        return ranking.top(n);
    }

    public List<String> getClientesDeRango(Rango rango) {
        return arbolRangos.clientesEnRango(rango.getMinimo(), rango.getMaximo());
    }

    public List<String> getHistorialPuntos(Cliente cliente) {
        return historialPuntos.getOrDefault(cliente.getId(), new ArrayList<>());
    }

    // ---- Privados ----

    private void actualizarArboles(Cliente cliente, int antes, int despues) {
        ranking.eliminar(antes, cliente.getId());
        ranking.insertar(despues, cliente.getId());
        arbolRangos.eliminar(antes, cliente.getId());
        arbolRangos.insertar(despues, cliente.getId());
    }

    private double descuentoPorRango(Rango rango) {
        switch (rango) {
            case PLATA:   return 0.05;
            case ORO:     return 0.10;
            case PLATINO: return 0.15;
            default:      return 0.0;
        }
    }

    private void anotar(Cliente cliente, String texto) {
        historialPuntos.computeIfAbsent(cliente.getId(), k -> new ArrayList<>()).add(texto);
    }
}
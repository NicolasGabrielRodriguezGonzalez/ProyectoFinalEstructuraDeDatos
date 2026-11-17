package co.edu.uniquindio.poo.servicios;

import java.util.LinkedHashMap;
import java.util.Map;

import co.edu.uniquindio.poo.estructuras.ListaDoble;
import co.edu.uniquindio.poo.modelo.Cliente;
import co.edu.uniquindio.poo.modelo.Monedero;
import co.edu.uniquindio.poo.modelo.Transaccion;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import co.edu.uniquindio.poo.modelo.Notificacion;
import co.edu.uniquindio.poo.modelo.TipoTransaccion;
import co.edu.uniquindio.poo.modelo.TransaccionProgramada;
import co.edu.uniquindio.poo.modelo.TransaccionProgramada.Periodicidad;

/**
 * Fachada única del sistema. La GUI solo habla con esta clase.
 *
 * Los clientes se guardan en un HashMap (búsqueda por id en O(1) promedio).
 * Se usa LinkedHashMap, que es un HashMap que además recuerda el orden de
 * registro, para que la lista de clientes salga siempre en el mismo orden.
 *
 * Personas 2 y 3: sus servicios se obtienen con getPuntos(), getAlertas(),
 * getAnalisis(). Si necesitan un método nuevo para su pestaña, agréguenlo aquí
 * como un método que delega en su servicio.
 */
public class SistemaMonedero {

    private final Map<String, Cliente> clientes = new LinkedHashMap<>();

    private final ServicioPuntos puntos = new ServicioPuntos();
    private final ServicioAlertas alertas = new ServicioAlertas();
    private final ServicioAnalisis analisis = new ServicioAnalisis();
    private final ServicioTransacciones transacciones =
            new ServicioTransacciones(clientes, puntos, alertas, analisis);
    private final ServicioProgramadas programadas = new ServicioProgramadas(transacciones, alertas);
    public static final double UMBRAL_SALDO_BAJO = ServicioTransacciones.UMBRAL_SALDO_BAJO;

    // ===================================================================
    // Clientes
    // ===================================================================

    public Cliente registrarCliente(String id, String nombre) {
        if (id == null || id.isBlank()) throw new OperacionInvalidaException("El id es obligatorio");
        if (nombre == null || nombre.isBlank()) throw new OperacionInvalidaException("El nombre es obligatorio");
        id = id.trim();
        if (clientes.containsKey(id)) throw new OperacionInvalidaException("Ya existe un cliente con id " + id);
        Cliente c = new Cliente(id, nombre.trim());
        clientes.put(id, c);
        return c;
    }

    /** Búsqueda por id: O(1) promedio. Devuelve null si no existe. */
    public Cliente buscarCliente(String id) {
        return id == null ? null : clientes.get(id.trim());
    }

    /** Búsqueda por id exacto o por parte del nombre (sin importar mayúsculas): O(n). */
    public ListaDoble<Cliente> buscarClientes(String texto) {
        ListaDoble<Cliente> encontrados = new ListaDoble<>();
        String t = texto == null ? "" : texto.trim().toLowerCase();
        for (Cliente c : clientes.values()) {
            if (t.isEmpty() || c.getId().equalsIgnoreCase(t) || c.getNombre().toLowerCase().contains(t)) {
                encontrados.agregarAlFinal(c);
            }
        }
        return encontrados;
    }

    public ListaDoble<Cliente> listarClientes() {
        return buscarClientes("");
    }

    public int cantidadClientes() {
        return clientes.size();
    }

    // ===================================================================
    // Monederos (multimonedero)
    // ===================================================================

    public Monedero crearMonedero(String idCliente, String nombre) {
        Cliente c = clienteObligatorio(idCliente);
        if (nombre == null || nombre.isBlank()) throw new OperacionInvalidaException("El nombre del monedero es obligatorio");
        for (Monedero m : c.getMonederos()) {
            if (m.getNombre().equalsIgnoreCase(nombre.trim())) {
                throw new OperacionInvalidaException(c.getNombre() + " ya tiene un monedero llamado " + m.getNombre());
            }
        }
        return c.agregarMonedero(nombre.trim());
    }

    public Monedero buscarMonedero(String idCliente, String idMonedero) {
        Cliente c = clienteObligatorio(idCliente);
        Monedero m = c.buscarMonedero(idMonedero);
        if (m == null) throw new OperacionInvalidaException(c.getNombre() + " no tiene el monedero " + idMonedero);
        return m;
    }

    // ===================================================================
    // Transacciones (contrato que usan Personas 2 y 3)
    // ===================================================================

    public Transaccion depositar(String idCliente, String idMonedero, double monto) {
        return transacciones.depositar(buscarMonedero(idCliente, idMonedero), monto);
    }

    /** Depósito al monedero principal. */
    public Transaccion depositar(String idCliente, double monto) {
        return transacciones.depositar(clienteObligatorio(idCliente).getMonederoPrincipal(), monto);
    }

    public Transaccion retirar(String idCliente, String idMonedero, double monto) {
        return transacciones.retirar(buscarMonedero(idCliente, idMonedero), monto);
    }

    public Transaccion transferir(String idClienteOrigen, String idMonederoOrigen,
                                  String idClienteDestino, String idMonederoDestino, double monto) {
        return transacciones.transferir(buscarMonedero(idClienteOrigen, idMonederoOrigen),
                buscarMonedero(idClienteDestino, idMonederoDestino), monto);
    }

    /** Transferencia entre los monederos principales de dos clientes. */
    public Transaccion transferir(String idClienteOrigen, String idClienteDestino, double monto) {
        return transacciones.transferir(clienteObligatorio(idClienteOrigen).getMonederoPrincipal(),
                clienteObligatorio(idClienteDestino).getMonederoPrincipal(), monto);
    }

    public Transaccion revertirUltima(String idCliente) {
        return transacciones.revertirUltima(clienteObligatorio(idCliente));
    }

    // ===================================================================
    // Consultas
    // ===================================================================

    public double consultarSaldo(String idCliente, String idMonedero) {
        return buscarMonedero(idCliente, idMonedero).getSaldo();
    }

    public double consultarSaldoTotal(String idCliente) {
        return clienteObligatorio(idCliente).getSaldoTotal();
    }

    public ListaDoble<Transaccion> getHistorial(String idCliente, String idMonedero) {
        return buscarMonedero(idCliente, idMonedero).getHistorial();
    }

    /** Historial de todos los monederos del cliente, sin repetir transferencias internas. */
    public ListaDoble<Transaccion> getHistorialCompleto(String idCliente) {
        ListaDoble<Transaccion> todas = new ListaDoble<>();
        for (Monedero m : clienteObligatorio(idCliente).getMonederos()) {
            for (Transaccion t : m.getHistorial()) {
                if (!todas.contiene(t)) todas.agregarAlFinal(t);
            }
        }
        return todas;
    }

    public ServicioTransacciones.ResultadoVerificacion verificarIntegridad(String idCliente, String idMonedero) {
        return transacciones.verificarIntegridad(buscarMonedero(idCliente, idMonedero));
    }

    public ListaDoble<ServicioTransacciones.ResultadoVerificacion> verificarIntegridad(String idCliente) {
        return transacciones.verificarIntegridad(clienteObligatorio(idCliente));
    }

    /** Verifica todos los monederos de todos los clientes. */
    public ListaDoble<ServicioTransacciones.ResultadoVerificacion> verificarIntegridadSistema() {
        ListaDoble<ServicioTransacciones.ResultadoVerificacion> resultados = new ListaDoble<>();
        for (Cliente c : clientes.values()) {
            for (ServicioTransacciones.ResultadoVerificacion r : transacciones.verificarIntegridad(c)) {
                resultados.agregarAlFinal(r);
            }
        }
        return resultados;
    }

    // ===================================================================
    // Programadas, alertas y análisis (Persona 3)
    // ===================================================================

    public ServicioProgramadas getProgramadas() { return programadas; }

    /**
     * DEPOSITO: idMonederoOrigen = null, destino = idMonederoDestino (idClienteDestino null means the same client).
     * RETIRO: origen = idMonederoOrigen, destino = null.
     * TRANSFERENCIA: origen and destino both set; idClienteDestino is the other client.
     */
    public TransaccionProgramada programar(String idCliente, TipoTransaccion tipo,
                                           String idMonederoOrigen, String idClienteDestino,
                                           String idMonederoDestino, double monto, String categoria,
                                           LocalDate fecha, Periodicidad periodicidad) {
        Cliente c = clienteObligatorio(idCliente);
        Monedero origen = idMonederoOrigen == null ? null : buscarMonedero(idCliente, idMonederoOrigen);
        Monedero destino = idMonederoDestino == null ? null
                : buscarMonedero(idClienteDestino == null ? idCliente : idClienteDestino, idMonederoDestino);
        return programadas.programar(c, tipo, origen, destino, monto, categoria, fecha, periodicidad);
    }

    public List<TransaccionProgramada> listarProgramadas(String idCliente) {
        return programadas.pendientesDe(clienteObligatorio(idCliente));
    }

    public boolean cancelarProgramada(String idCliente, String idProgramada) {
        return programadas.cancelar(clienteObligatorio(idCliente), idProgramada);
    }

    public List<String> avanzarDias(int dias) { return programadas.avanzarDias(dias); }

    public int generarRecordatorios(int diasAnticipacion) {
        return programadas.generarRecordatorios(diasAnticipacion);
    }

    public LocalDate getFechaSimulada() { return programadas.getFechaSimulada(); }

    public List<Notificacion> getNotificaciones(String idCliente) {
        return alertas.getNotificaciones(clienteObligatorio(idCliente));
    }

    public List<Notificacion> procesarNotificaciones() { return alertas.procesarPendientes(); }

    public void asignarCategoria(String idTransaccion, String categoria) {
        analisis.asignarCategoria(idTransaccion, categoria);
    }

    // ---- Reportes ----

    public Optional<Map.Entry<String, Double>> categoriaConMayorGasto() {
        return analisis.categoriaConMayorGasto();
    }

    public Map<String, Double> gastoPorCategoria() { return analisis.getGastoPorCategoria(); }

    public Optional<Map.Entry<String, Double>> clienteQueMasTransfiere() {
        return analisis.clienteQueMasTransfiere();
    }

    public Optional<Map.Entry<String, Double>> clienteQueMasRecibe() {
        return analisis.clienteQueMasRecibe();
    }

    public List<String> conexionesDe(String idCliente) {
        return analisis.conexionesDeCliente(clienteObligatorio(idCliente).getId());
    }

    public List<String> recorridoDfsDe(String idCliente) {
        return analisis.recorridoDfsCliente(clienteObligatorio(idCliente).getId());
    }

    public boolean hayCiclosEnTransferencias() { return analisis.hayCiclosEnTransferencias(); }

    public List<String> monederosConectados(String idCliente, String idMonedero) {
        return analisis.monederosConectados(buscarMonedero(idCliente, idMonedero).getId());
    }

    public List<String> transicionesCategorias() { return analisis.transicionesCategorias(); }

    // ===================================================================
    // Acceso a los servicios de Personas 2 y 3
    // ===================================================================

    public ServicioPuntos getPuntos() { return puntos; }
    public ServicioAlertas getAlertas() { return alertas; }
    public ServicioAnalisis getAnalisis() { return analisis; }
    public ServicioTransacciones getTransacciones() { return transacciones; }

    private Cliente clienteObligatorio(String idCliente) {
        Cliente c = buscarCliente(idCliente);
        if (c == null) throw new OperacionInvalidaException("No existe el cliente " + idCliente);
        return c;
    }
}

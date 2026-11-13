package co.edu.uniquindio.poo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.uniquindio.poo.estructuras.ListaDoble;
import co.edu.uniquindio.poo.estructuras.Pila;
import co.edu.uniquindio.poo.modelo.Cliente;
import co.edu.uniquindio.poo.modelo.Monedero;
import co.edu.uniquindio.poo.modelo.Transaccion;
import co.edu.uniquindio.poo.servicios.DatosEjemplo;
import co.edu.uniquindio.poo.servicios.OperacionInvalidaException;
import co.edu.uniquindio.poo.servicios.ServicioTransacciones;
import co.edu.uniquindio.poo.servicios.SistemaMonedero;

class NucleoTest {

    private SistemaMonedero s;
    private Cliente ana;
    private Cliente beto;

    @BeforeEach
    void preparar() {
        s = new SistemaMonedero();
        ana = s.registrarCliente("1", "Ana Gómez");
        beto = s.registrarCliente("2", "Beto Ruiz");
    }

    // ---------------- estructuras ----------------

    @Test
    void listaDobleAgregaEliminaYRecorreEnAmbosSentidos() {
        ListaDoble<Integer> l = new ListaDoble<>();
        l.agregarAlFinal(2);
        l.agregarAlFinal(3);
        l.agregarAlInicio(1);
        assertEquals(3, l.getTamanio());
        assertEquals(2, l.obtener(1));
        StringBuilder alReves = new StringBuilder();
        for (int x : l.alReves()) alReves.append(x);
        assertEquals("321", alReves.toString());
        assertTrue(l.eliminar(2));
        assertEquals("[1 <-> 3]", l.toString());
        assertEquals(1, l.eliminarPrimero());
        assertEquals(3, l.eliminarUltimo());
        assertTrue(l.estaVacia());
        assertNull(l.getCabeza());
        assertNull(l.getCola());
    }

    @Test
    void pilaEsLifo() {
        Pila<String> p = new Pila<>();
        p.push("a");
        p.push("b");
        assertEquals("b", p.peek());
        assertEquals("b", p.pop());
        assertEquals("a", p.pop());
        assertTrue(p.estaVacia());
        assertThrows(java.util.NoSuchElementException.class, p::pop);
    }

    // ---------------- clientes ----------------

    @Test
    void buscaClientesPorIdYNombre() {
        assertEquals(ana, s.buscarCliente("1"));
        assertNull(s.buscarCliente("99"));
        assertEquals(1, s.buscarClientes("gómez").getTamanio());
        assertEquals(2, s.buscarClientes("").getTamanio());
        assertThrows(OperacionInvalidaException.class, () -> s.registrarCliente("1", "Otra"));
    }

    // ---------------- operaciones ----------------

    @Test
    void depositoRetiroYTransferencia() {
        s.depositar("1", 100_000);
        s.retirar("1", ana.getMonederoPrincipal().getId(), 20_000);
        s.transferir("1", "2", 30_000);
        assertEquals(50_000, ana.getSaldoTotal(), 0.001);
        assertEquals(30_000, beto.getSaldoTotal(), 0.001);
        assertEquals(3, ana.getMonederoPrincipal().getHistorial().getTamanio());
        assertEquals(1, beto.getMonederoPrincipal().getHistorial().getTamanio());
    }

    @Test
    void rechazaMontosInvalidosYSaldoInsuficiente() {
        String m = ana.getMonederoPrincipal().getId();
        assertThrows(OperacionInvalidaException.class, () -> s.depositar("1", 0));
        assertThrows(OperacionInvalidaException.class, () -> s.depositar("1", -5));
        assertThrows(OperacionInvalidaException.class, () -> s.retirar("1", m, 1));
        assertThrows(OperacionInvalidaException.class, () -> s.transferir("1", "2", 1));
        assertThrows(OperacionInvalidaException.class, () -> s.transferir("1", m, "1", m, 1));
        assertTrue(ana.getMonederoPrincipal().getHistorial().estaVacia());
    }

    @Test
    void multimonederoTransfiereEntreMonederosPropios() {
        Monedero ahorros = s.crearMonedero("1", "Ahorros");
        s.depositar("1", 100_000);
        s.transferir("1", ana.getMonederoPrincipal().getId(), "1", ahorros.getId(), 40_000);
        assertEquals(60_000, ana.getMonederoPrincipal().getSaldo(), 0.001);
        assertEquals(40_000, ahorros.getSaldo(), 0.001);
        assertEquals(100_000, ana.getSaldoTotal(), 0.001);
        assertEquals(2, s.getHistorialCompleto("1").getTamanio());
        assertThrows(OperacionInvalidaException.class, () -> s.crearMonedero("1", "ahorros"));
    }

    // ---------------- reversión ----------------

    @Test
    void revierteEnOrdenLifo() {
        s.depositar("1", 100_000);
        s.transferir("1", "2", 30_000);
        Transaccion r1 = s.revertirUltima("1");
        assertTrue(r1.isRevertida());
        assertEquals(100_000, ana.getSaldoTotal(), 0.001);
        assertEquals(0, beto.getSaldoTotal(), 0.001);
        s.revertirUltima("1");
        assertEquals(0, ana.getSaldoTotal(), 0.001);
        assertThrows(OperacionInvalidaException.class, () -> s.revertirUltima("1"));
    }

    @Test
    void noRevierteSiElDestinoYaGastoElDinero() {
        s.depositar("1", 100_000);
        s.transferir("1", "2", 50_000);
        s.retirar("2", beto.getMonederoPrincipal().getId(), 40_000);
        assertThrows(OperacionInvalidaException.class, () -> s.revertirUltima("1"));
        // La transferencia sigue en la pila y nada cambió
        assertEquals(1 + 1, ana.getPilaReversion().getTamanio());
        assertEquals(10_000, beto.getSaldoTotal(), 0.001);
    }

    // ---------------- integridad ----------------

    @Test
    void verificacionRecursivaPasaConDatosCorrectos() {
        DatosEjemplo.cargar(s);
        s.revertirUltima("1009");
        for (ServicioTransacciones.ResultadoVerificacion r : s.verificarIntegridadSistema()) {
            assertTrue(r.esValido(), r.toString());
            assertEquals(r.getSaldoGuardado(), r.getSaldoCalculado(), 0.001);
        }
    }

    @Test
    void verificacionDetectaSaldoAlterado() throws Exception {
        s.depositar("1", 100_000);
        Monedero m = ana.getMonederoPrincipal();
        // Se altera el saldo "por fuera" para simular un dato corrupto
        java.lang.reflect.Field f = Monedero.class.getDeclaredField("saldo");
        f.setAccessible(true);
        f.setDouble(m, 999);
        ServicioTransacciones.ResultadoVerificacion r = s.verificarIntegridad("1", m.getId());
        assertFalse(r.esValido());
        assertEquals(100_000, r.getSaldoCalculado(), 0.001);
        assertNotNull(r.getErrores().obtenerPrimero());
    }
}

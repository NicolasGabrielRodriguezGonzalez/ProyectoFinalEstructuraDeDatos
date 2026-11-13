package co.edu.uniquindio.poo.servicios;

/** Carga unos 10 clientes con movimientos para la demo y los reportes. */
public final class DatosEjemplo {

    private DatosEjemplo() { }

    public static void cargar(SistemaMonedero s) {
        String[][] clientes = {
            {"1001", "Ana Gómez"}, {"1002", "Bruno Díaz"}, {"1003", "Carla Ruiz"},
            {"1004", "David Torres"}, {"1005", "Elena Marín"}, {"1006", "Felipe Rojas"},
            {"1007", "Gloria Pérez"}, {"1008", "Hugo Castaño"}, {"1009", "Isabel Mora"},
            {"1010", "Jorge Valencia"}
        };
        for (String[] c : clientes) s.registrarCliente(c[0], c[1]);

        // Depósitos iniciales
        double[] iniciales = {250_000, 120_000, 600_000, 80_000, 1_500_000,
                              45_000, 300_000, 900_000, 200_000, 50_000};
        for (int i = 0; i < clientes.length; i++) s.depositar(clientes[i][0], iniciales[i]);

        // Monederos adicionales
        String ahorroAna = s.crearMonedero("1001", "Ahorros").getId();
        String viajesCarla = s.crearMonedero("1003", "Viajes").getId();
        String ahorroElena = s.crearMonedero("1005", "Ahorros").getId();

        // Movimientos
        s.transferir("1001", s.buscarCliente("1001").getMonederoPrincipal().getId(), "1001", ahorroAna, 100_000);
        s.transferir("1003", s.buscarCliente("1003").getMonederoPrincipal().getId(), "1003", viajesCarla, 250_000);
        s.transferir("1005", s.buscarCliente("1005").getMonederoPrincipal().getId(), "1005", ahorroElena, 700_000);
        s.transferir("1002", "1004", 30_000);
        s.transferir("1005", "1006", 150_000);
        s.transferir("1008", "1001", 75_000);
        s.transferir("1007", "1009", 60_000);
        s.retirar("1003", s.buscarCliente("1003").getMonederoPrincipal().getId(), 120_000);
        s.retirar("1008", s.buscarCliente("1008").getMonederoPrincipal().getId(), 200_000);
        s.retirar("1010", s.buscarCliente("1010").getMonederoPrincipal().getId(), 45_000);
        s.depositar("1004", 40_000);
        s.depositar("1009", 110_000);
        s.transferir("1009", "1002", 25_000);
    }
}

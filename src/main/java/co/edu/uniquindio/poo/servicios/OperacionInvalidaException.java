package co.edu.uniquindio.poo.servicios;

/** Error de negocio que la GUI le muestra al usuario (monto inválido, saldo insuficiente, etc.). */
public class OperacionInvalidaException extends RuntimeException {
    public OperacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}

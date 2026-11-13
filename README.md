# Monedero Virtual con Puntos

Proyecto final de Estructuras de Datos (Ingeniería de Sistemas, Universidad del Quindío).
Monedero virtual con sistema de puntos, rangos, transacciones programadas y reportes,
hecho en Java con JavaFX y estructuras de datos propias.

## Equipo y módulos

| Persona | Módulo | Rama |
|---|---|---|
| Persona 1 | Núcleo: modelo, `SistemaMonedero`, transacciones, historial, pila de reversión, integridad recursiva, `VentanaPrincipal` | `p1-nucleo` |
| Persona 2 | Puntos y rangos: `TablaHash`, `ABB`, `AVL`, canje, beneficios, `PanelPuntos` | `p2-puntos` |
| Persona 3 | Programadas, alertas y análisis: `ColaPrioridad`, `ListaCircular`, `Cola`, `Grafo`, `PanelProgramadas`, `PanelReportes` | `p3-programadas` |

## Estructura de paquetes

```
src/main/java/co/edu/uniquindio/poo/
├── App.java          (punto de entrada)
├── estructuras/      (ListaDoble, Pila, TablaHash, ABB, AVL, Cola, ColaPrioridad, ListaCircular, Grafo)
├── modelo/           (Cliente, Monedero, Transaccion, TipoTransaccion, Rango, ...)
├── servicios/        (SistemaMonedero = fachada, y un servicio por módulo)
└── gui/              (VentanaPrincipal y un panel por módulo)
src/test/java/co/edu/uniquindio/poo/   (pruebas JUnit 5)
```

Regla: la GUI solo llama a `SistemaMonedero`; nunca toca las estructuras directamente.

## Requisitos y cómo ejecutar

- JDK 21 o superior (el proyecto compila para Java 21; sirve JDK 21, 25 o 26)
- Maven (IntelliJ y NetBeans lo traen integrado)

```bash
mvn compile        # compilar
mvn test           # correr pruebas
mvn javafx:run     # abrir la aplicación
```

En IntelliJ: *File → Open* y elegir la carpeta del proyecto (lo detecta como Maven).

## Cómo trabajamos en Git

Ver [CONTRIBUTING.md](CONTRIBUTING.md).

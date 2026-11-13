# Cómo trabajamos en Git

## Ramas

- `main`: siempre compila. Nadie hace push directo; todo entra por Pull Request.
- `p1-nucleo`, `p2-puntos`, `p3-programadas`: la rama de cada persona.
- Si una tarea es grande, se puede sacar una rama corta desde la propia, por ejemplo `p1-reversion`.

## Primera vez (cada integrante)

```bash
git clone https://github.com/NicolasGabrielRodriguezGonzalez/ProyectoFinalEstructuraDeDatos.git
cd ProyectoFinalEstructuraDeDatos
git checkout p1-nucleo          # o p2-puntos / p3-programadas
```

## Ciclo diario

```bash
# 1. Al empezar: traer lo último de main a tu rama
git checkout p1-nucleo
git pull origin main

# 2. Trabajar y hacer commits pequeños
git add src/main/java/co/edu/uniquindio/poo/estructuras/Pila.java
git commit -m "feat(estructuras): Pila<T> con push, pop y peek"

# 3. Subir tu rama
git push origin p1-nucleo

# 4. En GitHub: abrir Pull Request de p1-nucleo hacia main
```

## Mensajes de commit

Formato: `tipo(paquete): qué hace`, en presente y en español.

| Tipo | Cuándo | Ejemplo |
|---|---|---|
| `feat` | funcionalidad nueva | `feat(servicios): depositar y retirar en ServicioTransacciones` |
| `fix` | corregir un error | `fix(estructuras): ListaDoble no actualizaba la cola al eliminar` |
| `test` | pruebas | `test(estructuras): pruebas de Pila` |
| `docs` | README, informe, comentarios | `docs: sección de pilas en el informe` |
| `refactor` | ordenar código sin cambiar lo que hace | `refactor(modelo): extraer validación de monto` |

Un commit = un cambio con sentido. Mejor 5 commits pequeños que 1 gigante; el profesor puede ver el historial de cada uno.

## Pull Requests

1. Título igual que un commit: `feat(estructuras): ListaDoble y Pila`.
2. Antes de abrirlo: `mvn compile` y `mvn test` deben pasar.
3. Otra persona del equipo lo revisa y lo aprueba; después se hace *Squash and merge* o *Merge*.
4. Si hay conflicto, quien abrió el PR lo resuelve en su rama (`git pull origin main`, arreglar, commit, push).

## Reglas para evitar conflictos

- Cada quien edita solo sus archivos (ver tabla del README).
- Los archivos compartidos (`modelo/`, `SistemaMonedero`, `VentanaPrincipal`) se cambian avisando al grupo y en PRs pequeños.
- Los nombres y parámetros de los métodos del contrato no se cambian sin acuerdo.

## Integraciones (fechas del plan)

| Momento | Qué se une a `main` |
|---|---|
| Final del Día 1 | Estructuras base + contrato con métodos vacíos. Debe compilar. |
| Final del Día 2 | Todos los paneles en la ventana principal. |
| Día 3 | Solo correcciones y pruebas. Se etiqueta la entrega con `git tag v1.0`. |

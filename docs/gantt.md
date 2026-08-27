# Feature: Gantt (equipo de Argentina)

Vista **Team Gantt** de la app de time-tracking (ruta `/gantt`). Muestra el **cronograma
planificado** del equipo de Argentina en dos modos. Hoy funciona con **datos de ejemplo
(fake)**, sin necesidad de Jira ni configuración, para poder correr y demostrar la pantalla.

> Acceso: la vista está disponible únicamente por URL en `http://localhost:8080/gantt`
> (no se agregó al menú lateral, para no tocar nada de lo ya existente).

---

## Parte 1 — Explicación no técnica

### Qué es y qué muestra

Un **Gantt** clásico: una fila por trabajo con una barra ubicada sobre una línea de tiempo
(inicio → fin planificados). Cada barra representa la **ventana planificada** de un issue.
Una línea punteada roja marca **hoy**, para ver de un vistazo qué está atrasado, en curso o
por empezar.

### Los dos modos (toggle arriba)

- **By team**: el cronograma general, organizado como **milestone → epic → issue**. Cada
  milestone es una sección **colapsable**; se expande para ver sus epics e issues. Los issues
  se colorean por **estado**: verde = Done, azul = In Progress, gris = To Do.
- **By role**: el mismo trabajo **agrupado por rol** (Backend, Frontend, Full Stack, DevOps,
  Mobile Developer). Acá **todo se muestra de una**, sin expandir: cada rol es un encabezado
  con todos sus issues debajo, y las barras se colorean por rol.

### Cómo leer los números

- Las barras son **fechas planificadas** (inicio y fin), no tiempo registrado.
- La barra de un milestone o de un epic abarca de la **primera** fecha de inicio a la
  **última** fecha de fin de sus hijos.
- Los datos son de ejemplo: no reflejan trabajo real todavía.

---

## Parte 2 — Explicación técnica

### Aislamiento

El feature vive **completo** en `src/main/java/com/example/timetracking/gantt/` y **no
depende de ningún otro feature** (`milestone`, `velocity`, `shared`). Trae su propio modelo,
sus propios widgets, su propia paleta (`GanttStyle`) y su propio proveedor de datos. Lo único
que referencia de afuera es `views/MainLayout` como *layout* del `@Route` (no lo modifica).

### Arquitectura del módulo

| Capa | Contenido |
|---|---|
| `gantt/` (raíz) | `GanttView` — única ruta Vaadin (`@Route("gantt")`), toggle de modo y leyenda |
| `application/model/` | `Role`, `TeamMember`, `GanttTask` — modelo propio del feature |
| `application/data/` | `FakeGanttDataProvider` — **único punto de datos** (dataset hardcodeado) |
| `application/dto/` | `GanttChart`, `GanttGroup`, `GanttRow`, `GanttTimeline`, `GanttColors` |
| `application/usecase/` | `BuildTeamGanttUseCase`, `BuildRoleGanttUseCase` (+ `GanttBounds`) |
| `ui/style/` | `GanttStyle` — constantes locales y formato de fechas |
| `ui/widget/` | `GanttChartWidget`, `GanttBar`, `GanttTimeAxis`, `ModeToggle`, `CollapsibleSection` |

### Flujo de datos

```
GanttView (@Route "gantt")
    │  render() despacha según ModeToggle.Mode
    ├── BY_TEAM → BuildTeamGanttUseCase ─┐
    └── BY_ROLE → BuildRoleGanttUseCase ─┴─→ GanttChart ──→ GanttChartWidget
                        │
                        └── FakeGanttDataProvider.tasks()  (List<GanttTask>)
```

Ambos use cases parten del **mismo** `List<GanttTask>` y solo cambian el agrupamiento, así
que la línea de tiempo (min/max global) es idéntica entre modos.

### El cálculo

- `BuildTeamGanttUseCase`: agrupa por milestone → epic → issue (preservando el orden del
  dataset). El milestone es un `GanttGroup` colapsable; el epic y el issue son filas
  (`depth` 1 y 2). Las fechas de un nodo agregado = `min(start)` / `max(end)` de sus hijos.
- `BuildRoleGanttUseCase`: un `GanttGroup` **no colapsable** por cada `Role`, con una fila
  por issue ordenada por fecha de inicio.

### UI: Gantt sin librería de charts

Todo es **CSS puro** (divs y `position:absolute`), en línea con el resto de la app:

- **`GanttChartWidget`**: cada fila es un flex de dos celdas — label de ancho fijo
  (`GanttStyle.LABEL_COL_WIDTH`) + un *track* relativo. La barra se posiciona con
  `left = GanttChart.offsetPercent(start)` y `width = GanttChart.widthPercent(start, end)`.
- **Gridlines** verticales por mes (`GanttTimeline.monthTicks`) y **línea de "hoy"**
  (`LocalDate.now()`), ambas dibujadas en cada track; se ocultan si "hoy" cae fuera del rango.
- **`GanttTimeAxis`**: encabezado con las etiquetas de mes.
- **`CollapsibleSection`**: copia local (igual que hizo `velocity`) usada solo por *By team*.

### Datos de ejemplo

`FakeGanttDataProvider` define el equipo de Argentina (10 personas, 2 por rol) y ~17 issues
repartidos en 3 milestones a lo largo de **2026** (Feb–Nov), con estados Done / In Progress /
To Do. Es el **único** lugar a reemplazar cuando lleguen datos reales (por ejemplo, un loader
sobre Jira con las fechas planificadas), sin tocar los use cases ni la UI.

### Archivos clave

- `gantt/GanttView.java`
- `gantt/application/data/FakeGanttDataProvider.java`
- `gantt/application/usecase/BuildTeamGanttUseCase.java` / `BuildRoleGanttUseCase.java`
- `gantt/ui/widget/GanttChartWidget.java`

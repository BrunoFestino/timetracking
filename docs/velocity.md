# Feature: Velocity (velocidad de entrega)

Vista **Delivery Velocity** de la app de time-tracking (ruta `/velocity`, tercer ítem del menú lateral). Este documento tiene dos partes: una explicación **no técnica** (qué es, cómo usarla y cómo leer los números) y una explicación **técnica** (arquitectura, cálculo y performance).

---

## Parte 1 — Explicación no técnica

### Qué es y qué mide

La velocity responde una pregunta simple: **¿cuánto tarda el equipo en terminar un milestone?**

A diferencia de la "velocity" clásica de Scrum, acá **no se usan story points, ni cantidad de tickets cerrados, ni un ritmo semanal**. La medida es el **tiempo de entrega**: para cada milestone ya entregado, los días de calendario que pasaron entre su arranque y su entrega efectiva. Al lado de ese tiempo se muestra el **esfuerzo** que costó (los worklogs de Jira, en man-days u horas), para distinguir "tardó mucho" de "costó mucho".

La vista solo trabaja con **milestones entregados (delivered)**: un milestone en curso todavía no tiene tiempo de entrega que comparar.

### Comparar milestones dentro de un proyecto

Se elige **un proyecto** y, dentro de él, **uno o varios milestones entregados** — no importa que sean de **tipos** distintos. Cada milestone se mide contra **su propia ventana de entrega** (arranque → entrega), nunca contra el calendario, así que dos milestones que corrieron en momentos distintos siguen siendo comparables lado a lado.

### Qué preguntas de negocio responde

- **¿En qué se diferencian los milestones de un vistazo?** El "Comparison overview" de arriba muestra los **deltas** entre ellos (cuánto más rápido, cuánto más esfuerzo, etc.), no promedios.
- **¿En qué se diferencian en detalle?** El tab "Compare milestones" los pone **lado a lado** en una tabla (una columna por milestone, una fila por métrica) para leerlos línea por línea.
- **¿Terminar rápido salió caro?** La tabla muestra, por milestone, su esfuerzo total y su **esfuerzo por día abierto** (pace): dos milestones de la misma duración con paces distintos implican equipos de tamaño distinto o tiempo muerto.
- **¿Quién cargó cada entrega?** La tabla termina con un bloque de esfuerzo por persona, y el tab "Per person" desglosa la misma selección desde cada contribuidor.

### Cómo usar la pantalla

1. Elegir **un proyecto**.
2. Seleccionar **uno o más milestones entregados** de ese proyecto.
3. Presionar **Search**.

El toggle **MD / Hours** de la toolbar cambia solo cómo se muestra el **esfuerzo**; las duraciones siempre van en días. Cambiar la unidad no recalcula nada, solo re-renderiza.

Qué muestra:

La vista arma **tres cards apiladas a todo el ancho** (sin tabs):

- **Comparison overview**: una **card por milestone** (borde superior en su color + clave + nombre) con una grilla 2×2 de las cuatro métricas de velocidad — **Duration**, **Total effort**, **Weekly effort rate** (esfuerzo por semana) y **Team size**. Las cards se estiran para llenar el ancho y wrapean; al tener el mismo layout, los valores quedan alineados entre milestones para comparar de un vistazo. Sin frase-insight automática. Con un solo milestone, muestra un hint para agregar otro.
- **Delivery velocity**: el **único gráfico** del feature — **Effort over time**, una **línea por milestone** con el esfuerzo (absoluto) logueado por **semana** desde el arranque de cada uno (alineados por días-desde-su-inicio, no por calendario, así dos corridas de épocas distintas se superponen; eje X con gridline+etiqueta en ~8 buckets), cada línea en el **color** de su milestone, a todo el ancho de la card con la **leyenda** debajo.
- **Compare milestones**: la comparación detallada lado a lado (una columna por milestone, a todo el ancho), con las filas agrupadas en tres bloques — **Delivery dates** (Duration, Started, Delivered, Planned delivery), **Effort** (Total effort, Estimate, Consumption %, Variance, Active days, Effort per active day, Effort per week, Effort per open day) y **Team** (Contributors + **esfuerzo absoluto por persona**: una fila por persona con una **barra horizontal** por milestone en el color del milestone y el valor en **man-days** — "0 MD" si no participó, sin porcentajes).

El mismo milestone lleva **el mismo color** en su card del overview, en la línea del gráfico y en su columna de la tabla, para identificarlo de un vistazo.

### Cómo leer los números sin malinterpretarlos

- **La duración es tiempo de calendario, no esfuerzo.** Un milestone de 40 días puede haber tenido 5 días de trabajo real: los días incluyen fines de semana, esperas y pausas. Para el trabajo real está la fila de esfuerzo.
- **Un milestone sin fechas de entrega no promedia.** Si no se puede establecer arranque o entrega, se muestra "n/a" y queda afuera del promedio (no cuenta como cero).
- **Solo cuenta lo que se registra.** El esfuerzo depende de la disciplina de carga de worklogs en Jira; el tiempo de entrega no, porque sale de las fechas del milestone.
- **Consumo y varianza necesitan estimado.** Salen del estimado en man-days del milestone (`effortEstimateManDays` del propio milestone, o el original estimate de Jira como fallback — la misma regla que el dashboard de milestone). Viven solo en la tabla; un milestone sin estimado muestra "n/a", no cero.
- **Velocidad ≠ pace de calendario.** El **weekly effort rate** (esfuerzo por semana) del overview y el **effort per active day** de la tabla (esfuerzo por día con worklog, sin castigar el idle como el "effort per open day" de calendario) miden ritmo real. Un milestone sin días activos o sin ventana fechada cae en "n/a".
- **El gráfico de esfuerzo en el tiempo necesita fechas de worklog.** Cada punto sale de la fecha de cada worklog; trabajo sin fecha parseable no entra en la curva (sí en los totales).
- **Solo cuenta el trabajo dentro del árbol del milestone** (milestone → epics → issues → subtasks). Trabajo logueado fuera de esa jerarquía no aparece.
- **Los datos pueden tener hasta ~5 minutos de atraso.** La app cachea los árboles de milestone de Jira y limpia la cache cada 5 minutos.

---

## Parte 2 — Explicación técnica

### Arquitectura del módulo

El feature vive en `src/main/java/com/example/timetracking/velocity/`, con capas al estilo clean architecture:

| Capa | Contenido |
|---|---|
| `velocity/` (raíz) | `VelocityView` — la única ruta Vaadin del feature |
| `application/usecase/` | `LoadDeliveredMilestonesUseCase` (qué se puede elegir), `ComputeVelocityUseCase` (orquestación del cálculo) |
| `application/mapper/` | `MilestoneDelivery` (reglas de entrega), `VelocityAggregator` — cálculo puro, sin dependencias de Jira ni de UI |
| `application/dto/` | Records inmutables: `VelocityReport`, `MilestoneVelocity`, `PersonVelocity`, `PersonMilestoneEffort`, `EffortBucket` |
| `ui/style/` | `VelocityStyles` — stylesheet inyectada, empty-state, KPI tiles y la **paleta por milestone** (`colorFor`) |
| `ui/widget/` | `EffortTimelineChart` (SVG, único gráfico), `UnitToggle`; `ComparisonBarChart`, `Sparkline` y `CollapsibleSection` quedaron sin uso tras el rediseño |

Dirección de dependencias: `velocity` depende del feature `milestone` (loaders, dominio, estilos) y de `shared/jira`; **nada depende de velocity**. Es una capa de solo lectura/analítica sobre los mismos datos de Jira que usa la vista Milestone.

Convención central: **todo el esfuerzo se guarda en segundos** (`Worklog.timeSpentSeconds()`) y **toda duración se guarda en días de calendario**; la UI convierte el esfuerzo a MD u horas al renderizar según `UnitToggle`.

### Flujo de datos

```
VelocityView (@Route "velocity")
    ├── selección de proyecto → LoadDeliveredMilestonesUseCase.loadDeliveredMilestones(projectKey)
    │        └── JiraApiClient.searchMilestonesWithDeliveryByProject  (+ filtro MilestoneDelivery.isDelivered)
    └── Search → ComputeVelocityUseCase.execute(milestoneKeys) → VelocityAggregator → VelocityReport
                     └── LoadMilestoneDetailsUseCase.loadByKey (feature milestone, @Cacheable)
                             └── JiraApiClient (milestone → epics → issues → subtasks)
```

La fuente de datos es el árbol de milestone del feature `milestone`: cada `JiraTicket` trae sus `worklogs()` y sus `children()`, construido recursivamente por `LoadMilestoneDetailsUseCase.loadByKey`. El compute recibe una lista de claves de milestone y carga cada árbol por su propia clave; el aggregator es agnóstico del proyecto (por eso la restricción a un solo proyecto vive únicamente en la vista, no en el modelo).

### Qué cuenta como "delivered"

Concentrado en `MilestoneDelivery`, para que el selector y el aggregator no puedan discrepar:

```java
// MilestoneDelivery.isDelivered
return parseDate(metadata.effectiveDeliveryDate()) != null || hasDeliveredStatus(metadata);
```

- **Entregado** si tiene *effective delivery date* (`customfield_13445`), o si su status es terminal (`delivered`, `done`, `closed`, `resolved`, `completed`, `finished`, `released`).
- **Fecha de entrega** (`deliveryDate`): effective delivery date → `resolutiondate` → último worklog.
- **Fecha de arranque** (`startDate`): el custom field de start date (`customfield_15030`) → primer worklog.
- Las fechas **planificadas** (`dueDate`, `baselineDeliveryDate`) nunca se usan como fecha de entrega; y desde que se sacó la puntualidad del feature, no se usan para nada (por eso salieron del JQL de listado).

El tipo de milestone **no se filtra**: cualquier milestone entregado entra en la comparación.

### El cálculo

#### Duración — `VelocityAggregator` + `MilestoneDelivery`

```java
// MilestoneDelivery.durationDays — días de calendario, inclusive
if (start == null || delivery == null || delivery.isBefore(start)) {
    return 0;   // sin ventana medible
}
return (int) (delivery.toEpochDay() - start.toEpochDay()) + 1;
```

Un `durationDays == 0` significa "no medible" y la UI lo muestra como `n/a`.

#### Cifras de equipo — `VelocityReport`

```java
// VelocityReport.avgDurationDays — el promedio ignora lo no medible
List<MilestoneVelocity> datable = datable();   // milestones con hasDuration()
return (int) Math.round(datable.stream().mapToInt(MilestoneVelocity::durationDays).average().orElse(0));
```

- `MilestoneVelocity.secondsPerDay()`: pace de calendario = esfuerzo / días abiertos (fila "Effort per open day").

**Extremos (`fastest/slowest/heaviestByEffort/densest/mostThroughput/...`)** — min/max/filtros O(n)
sobre `datable()`. Hoy la UI usa **solo `fastest()`**, para resaltar en verde la duración más
rápida de la tabla (`fastest().map(durationDays)`); el resto quedaron dormidos tras rediseñar el
overview de fila-de-deltas a cards por milestone (siguen en el DTO por si vuelven a hacer falta).
Igual que `avgDurationDays()`, que alimentaba la vieja línea de promedio del gráfico de barras.

#### Velocidad, estimado y esfuerzo en el tiempo — `VelocityAggregator`

El aggregator ya **no colapsa el árbol** a "segundos de worklog + primer/último worklog".
Además, por milestone deriva (todo desde el árbol ya cargado, sin llamadas nuevas a Jira):

- **`activeDays`**: cantidad de días calendario distintos con worklog. Base de la velocidad real:
  **`effectivePaceSeconds()`** = `spent / activeDays` (esfuerzo por día trabajado, ignora el idle,
  a diferencia de `secondsPerDay()` que divide por días calendario) y
  **`effortThroughputSecondsPerWeek()`** = `spent × 7 / durationDays` (esfuerzo por semana).
- **`estimateSeconds`**: estimado del milestone con **la misma regla que `ProgressAggregator.budget`**
  del dashboard de milestone — `effortEstimateManDays` **del propio nodo milestone** (× `SECONDS_PER_MAN_DAY`),
  si es 0 cae a `totalOriginalEstimateSeconds()`. **No se suma el custom field de los hijos**: el del
  milestone ya representa todo el milestone, sumarlos lo multi-contaba (bug corregido). `0` = sin
  estimar → `hasEstimate()` false. Alimenta `consumptionPct()`/`varianceSeconds()`/`overBudget()`,
  que viven solo en la tabla.
- **`plannedDeliveryDate`** = baseline delivery date (planificada) del metadata, mostrada en la fila
  "Planned delivery". (`scheduleSlipDays()` sigue calculado pero ya no se muestra tras el rediseño.)
- **`effortOverTime`**: lista densa de `EffortBucket(dayOffset, seconds)` — worklogs bucketeados
  en ventanas de `BUCKET_DAYS = 7` (semanales) desde `startDate`, rellenando ventanas vacías con
  cero. El trabajo logueado antes del arranque se atribuye a la ventana 0 en vez de descartarse.

#### Cifras por persona — `PersonVelocity` + `PersonMilestoneEffort`

Por cada persona y cada milestone se acumulan los segundos (`PersonMilestoneEffort` = `milestoneKey` + `seconds`). En el bloque **Team** de la tabla, cada persona es una fila con una **barra horizontal por milestone** (escala compartida = mayor esfuerzo persona-milestone de la selección) y el valor en **man-days absolutos** — "0 MD" si no participó, **sin porcentajes**. El cross-tab se arma con `MilestoneVelocity.secondsByPerson()` (unión de personas de la selección). El aggregator **deduplica tickets alcanzables desde más de un milestone seleccionado** con un set `seenTickets`, atribuyéndolos al primer milestone que los alcanza.

### UI: gráficos sin librería de charts

Casi todo es **divs con CSS puro**, sin librería externa; la única excepción es el gráfico de
esfuerzo en el tiempo, que necesita geometría de líneas y ejes y por eso se arma como SVG:

- **`EffortTimelineChart`** (el único gráfico): construye un **string SVG** (líneas, ejes,
  gridlines) y lo renderiza como imagen vía `DashboardStyle.svgImage(svg, alt)` — el único
  camino a vectores reales en el feature, ya que no hay librería de charts. Una **polyline por
  milestone** en su `colorFor`, alineadas por índice de bucket (= semanas desde el arranque de cada
  milestone), escaladas al máximo global de esfuerzo por ventana; eje Y (0/50/100% del máximo,
  formateados en MD/h) y eje X con **gridline + etiqueta de día en ~8 buckets equiespaciados**
  (`step = ceil(buckets / 8)`). `viewBox 960×280` y `width: 100%` sin tope, así **llena el ancho** de
  la card. Al ser un `<img>` no tiene tooltips DOM, así que lleva una **leyenda** (swatch + clave) en
  DOM real debajo. Un milestone sin worklogs fechados no aporta serie; si ninguno tiene, empty-state.
- **Cards del overview** (`VelocityView.epicOverviewCard`): fila flex `flex-wrap` de una card por
  milestone (`flex: 1 1 240px`), cada una con header coloreado + grilla 2×2 de `overviewMetric`
  (value + label). Llenan el ancho y wrapean; mismo layout → valores alineados entre milestones.
- **Tabla comparativa** (`VelocityView.compareCard`): CSS grid `minmax(160px,240px) repeat(N,
  minmax(140px,1fr))` con `width: 100%` (llena el ancho) dentro de un contenedor con `overflow-x:
  auto` solo para muchos milestones. Header por milestone (borde superior en su color); filas
  agrupadas con bandas `groupBand(...)` (Delivery dates / Effort / Team) y `addRow(...)`; la duración
  resalta el más rápido en verde; el bloque Team termina en las barras horizontales de esfuerzo por
  persona (`personBarCell`, MD absolutos). Todo desde el `VelocityReport` ya computado.
- **`UnitToggle`**: wrapper fino sobre `Tabs` de Vaadin. Cambiar de unidad solo re-renderiza; el report ya computado no se recalcula.
- Los widgets `ComparisonBarChart`, `Sparkline` y `CollapsibleSection` quedaron **sin uso** tras el
  rediseño (se removió el gráfico de duración y el tab Per person); se conservan los archivos por si
  se reutilizan.

### Datos, caching y paralelismo

- **Cache**: `LoadMilestoneDetailsUseCase.loadByKey` es `@Cacheable(MILESTONE_TREE_CACHE)` con key = clave del milestone, sobre un `ConcurrentMapCacheManager` (`shared/config/CacheConfig`). Un job programado (`MilestonesCacheEviction`, cron `0 */5 * * * *`) evicta **todas** las entradas cada 5 minutos para mantener frescura. Abrir la vista Milestone primero calienta la cache que velocity después reutiliza.
- **Carga paralela**: cada árbol de milestone son varias llamadas encadenadas a Jira y la evicción de 5 minutos hace que la mayoría de los computes sean cold-load, así que `ComputeVelocityUseCase.loadTreesInParallel` submitea cada `loadByKey` a un pool fijo acotado (`PARALLEL_LOADS = 6`, moderado para respetar rate limits de Jira), preservando el orden de submission. El wall time queda en ~el batch más lento en vez de la suma de todos los milestones. Si un milestone falla, falla el compute completo. Como se invoca el bean inyectado (proxy de Spring), `@Cacheable` sigue aplicando dentro del pool.
- **Listado de milestones**: `searchMilestonesWithDeliveryByProject` es una búsqueda por proyecto que trae status y las fechas necesarias para detectar entrega (`customfield_13445`, `resolutiondate`) además del summary; el filtro de entregados corre en memoria, así que no depende de adivinar nombres de status en el JQL.
- **Tabla y colores**: la tabla comparativa y el cross-tab por persona se arman con lo que ya trae el `VelocityReport` — cero llamadas nuevas a Jira. La paleta (`VelocityStyles.colorFor`) es determinística por índice de selección, así que gráfico y tabla coinciden.
- **Validación**: `ComputeVelocityUseCase` valida las claves de milestone contra `^[A-Z][A-Z0-9_]+-\d+$`.

### Limitaciones y decisiones de modelado conocidas

- **Duración = calendario, no esfuerzo.** Incluye fines de semana, feriados y pausas. Es deliberado: la pregunta es "en cuánto tiempo se termina", no "cuántos días hábiles de trabajo tuvo".
- **Fallback al último worklog.** Un milestone cerrado sin effective delivery date ni resolution date se fecha con su último worklog, que subestima la entrega si el trabajo terminó antes del cierre formal.
- **Worklogs fuera del árbol**: solo cuenta el esfuerzo en tickets alcanzables desde el milestone (milestone → epics → issues → subtasks).
- **Autor desconocido**: worklogs sin autor se agrupan bajo `"Unknown"`.
- **Un solo proyecto** vive solo en la vista: el aggregator es agnóstico del proyecto (recibe una lista de claves), así que el modelo no impide comparar cross-project si en el futuro se quisiera reactivar.
- **Muchos milestones a la vez**: la tabla escala en horizontal (scroll); pensada para comparar de a pocos (típicamente dos), no decenas.

### Archivos clave

- `velocity/VelocityView.java` — UI: selección de proyecto y las tres cards (overview por milestone, gráfico de esfuerzo, tabla comparativa agrupada con esfuerzo por persona)
- `velocity/application/usecase/ComputeVelocityUseCase.java` / `LoadDeliveredMilestonesUseCase.java`
- `velocity/application/mapper/VelocityAggregator.java` / `MilestoneDelivery.java`
- `velocity/ui/style/VelocityStyles.java` (paleta), `velocity/ui/widget/EffortTimelineChart.java` (SVG héroe), `ComparisonBarChart.java`, `Sparkline.java`
- `milestone/application/usecase/LoadMilestoneDetailsUseCase.java`, `shared/config/CacheConfig.java`, `milestone/application/cache/MilestonesCacheEviction.java`

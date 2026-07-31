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

- **Comparison overview**: depende de cuántos milestones se comparen.
  - **Con dos milestones** (el caso principal), un **head-to-head M1 vs M2**: arriba, un **veredicto** en lenguaje claro que sintetiza el trade-off ("M1 entregó 2.3× más rápido, y con menos esfuerzo" / "…, pero a +45% de esfuerzo"); debajo, una fila por métrica de velocidad — **Duration, Total effort, Effective pace, Throughput, Schedule slip, Team size** — con el valor de cada milestone lado a lado. El lado "mejor en velocidad" (menos días / más pace / más throughput / menos slip) se resalta **en el color de su milestone**; en esfuerzo y team size se marca neutro qué lado gastó más / tuvo más gente (más no es "mejor", es costo — lo interpreta el veredicto).
  - **Con tres o más**, la fila de **deltas** entre extremos de la selección — **Duration gap** ("N× faster"), **Effort gap** (abs y %), **Effective pace** ("N× faster"), **Throughput** ("N× more"), **Schedule slip** y **Team size** — con el key "ganador" de cada delta en **su color**.
  - **Con uno**, un hint para agregar otro.
- **Tab "Compare milestones"**: arriba, el gráfico héroe **Effort over time** — una **línea por milestone** con el esfuerzo logueado por **semana** desde el arranque de cada uno (alineados por días-desde-su-inicio, no por calendario, así dos corridas de épocas distintas se superponen; eje X con etiquetas cada ~semana/dos semanas), cada línea en el **color** de su milestone. Un toggle **Share % / Absolute** cambia el eje Y: en **Share** cada línea se escala a **su propio pico** (100%), así las *formas* de cadencia se comparan sin que el milestone más grande aplaste al resto; en **Absolute** el eje va en MD/h contra un máximo compartido. Debajo, el **trade-off scatter** — cada milestone como un punto por **duración (x, izquierda = más rápido)** vs **esfuerzo (y, arriba = más)**, así "rápido y barato" (abajo-izquierda) vs "lento y caro" (arriba-derecha) se lee de un vistazo. Luego el **gráfico de barras** de días de cada milestone (con **línea punteada del promedio**) y una **tabla comparativa lado a lado**: una columna por milestone, y filas de métricas — duración (se resalta la más rápida), fechas de arranque/entrega, **fecha planificada** y **desvío de cronograma**, esfuerzo total, **estimado**, **consumo %** (rojo si pasó el estimado), **varianza** (rojo over / verde under), **días activos**, **esfuerzo por día activo**, **esfuerzo por semana**, esfuerzo por día abierto y contribuidores — más un bloque de esfuerzo por persona (cada persona con lo que puso en cada milestone y su porcentaje, o "—" si no participó).
- **Tab "Per person"**: una sección colapsable por persona, con un mini gráfico del reparto de su esfuerzo entre los milestones seleccionados y, al expandir, cuánto puso en cada milestone y **qué porcentaje del esfuerzo total del milestone** representó (cuánto de esa entrega cargó).

El mismo milestone lleva **el mismo color** en el gráfico y en su columna de la tabla, para identificarlo de un vistazo.

### Cómo leer los números sin malinterpretarlos

- **La duración es tiempo de calendario, no esfuerzo.** Un milestone de 40 días puede haber tenido 5 días de trabajo real: los días incluyen fines de semana, esperas y pausas. Para el trabajo real está la fila de esfuerzo.
- **Un milestone sin fechas de entrega no promedia.** Si no se puede establecer arranque o entrega, se muestra "n/a" y queda afuera del promedio (no cuenta como cero).
- **Solo cuenta lo que se registra.** El esfuerzo depende de la disciplina de carga de worklogs en Jira; el tiempo de entrega no, porque sale de las fechas del milestone.
- **Consumo y varianza necesitan estimado.** Salen del estimado en man-days del milestone (`effortEstimateManDays` del propio milestone, o el original estimate de Jira como fallback — la misma regla que el dashboard de milestone). Viven solo en la tabla; un milestone sin estimado muestra "n/a", no cero.
- **Velocidad ≠ pace de calendario.** El **effective pace** es esfuerzo por **día activo** (día con worklog), así el tiempo muerto no lo baja como al "esfuerzo por día abierto" (calendario). El **throughput** es esfuerzo por semana sobre la ventana de entrega. Un milestone sin días activos o sin ventana fechada cae en "n/a".
- **El desvío de cronograma necesita fecha planificada.** Sale de la baseline delivery date; sin ella, la fila/tile muestra "n/a". Positivo = se entregó tarde, negativo = adelantado.
- **El gráfico de esfuerzo en el tiempo necesita fechas de worklog.** Cada barra/punto sale de la fecha de cada worklog; trabajo sin fecha parseable no entra en la curva (sí en los totales).
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
| `ui/widget/` | `EffortTimelineChart` (SVG héroe); widgets de CSS puro `TradeoffScatter`, `ComparisonBarChart`, `Sparkline`; `UnitToggle`, `CollapsibleSection` |

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

- `avgDurationDays()`: promedio sobre `datable()`; lo usa **solo la línea del gráfico**.
- `MilestoneVelocity.secondsPerDay()`: pace = esfuerzo / días abiertos.

**Extremos para el "Comparison overview"** — cada delta es la brecha entre los extremos de la selección en una dimensión, así que con 2 milestones se reduce a A-vs-B:

```java
public Optional<MilestoneVelocity> fastest()  { return datable().stream().min(byDuration); }
public Optional<MilestoneVelocity> slowest()  { return datable().stream().max(byDuration); }
// + heaviestByEffort/lightestByEffort, densest/sparsest (por secondsPerDay), most/fewestContributors
// + mostEffectivePace/leastEffectivePace (por effectivePaceSeconds), mostThroughput/leastThroughput, mostSlipped
```

Son todos min/max/filtros O(n) sobre listas chicas. La vista arma los deltas con estos
`Optional` (y guardas contra división por cero para los ratios/porcentajes); un `Optional`
vacío cae en "n/a". La tabla resalta el más rápido con `fastest().map(durationDays)`.

El summary ya no promedia la selección: `avgSecondsPerMilestone`, `fastestDurationDays`, la
mediana, el rango y la puntualidad se quitaron en rondas previas (promediar dos milestones
esconde la comparación; el on-time dependía de fechas planificadas que no siempre están).

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
- **`plannedDeliveryDate`** = baseline delivery date (planificada) del metadata;
  **`scheduleSlipDays()`** = `deliveryDate − plannedDeliveryDate` (positivo = tarde),
  guardado por `hasPlannedDelivery()`.
- **`effortOverTime`**: lista densa de `EffortBucket(dayOffset, seconds)` — worklogs bucketeados
  en ventanas de `BUCKET_DAYS = 7` (semanales) desde `startDate`, rellenando ventanas vacías con
  cero. El trabajo logueado antes del arranque se atribuye a la ventana 0 en vez de descartarse.

#### Cifras por persona — `PersonVelocity` + `PersonMilestoneEffort`

Por cada persona y cada milestone se acumulan los segundos (`PersonMilestoneEffort` = `milestoneKey` + `seconds`). En la tabla comparativa y en el tab Per person se muestra, por milestone, el esfuerzo de la persona y su **porcentaje del esfuerzo total del milestone** (cuánto de esa entrega cargó); `avgSecondsPerMilestone()` es su esfuerzo promedio por milestone participado. El cross-tab de la tabla se arma con `MilestoneVelocity.secondsByPerson()` (unión de personas de la selección). El aggregator **deduplica tickets alcanzables desde más de un milestone seleccionado** con un set `seenTickets`, atribuyéndolos al primer milestone que los alcanza.

### UI: gráficos sin librería de charts

Casi todo es **divs con CSS puro**, sin librería externa; la única excepción es el gráfico de
esfuerzo en el tiempo, que necesita geometría de líneas y ejes y por eso se arma como SVG:

- **`EffortTimelineChart`** (el gráfico héroe): construye un **string SVG** (líneas, ejes,
  gridlines) y lo renderiza como imagen vía `DashboardStyle.svgImage(svg, alt)` — el único
  camino a vectores reales en el feature, ya que no hay librería de charts. El `<svg>` declara
  `width`/`height` explícitos **además** del `viewBox`, para que el `<img>` tenga dimensiones
  intrínsecas (si no, con `height:auto` colapsa a 0 y no se ve). Una **polyline por milestone** en
  su `colorFor`, alineadas por índice de bucket (= semanas desde el arranque de cada milestone);
  eje X con **gridline + etiqueta de día en ~8 buckets equiespaciados** (`step = ceil(buckets / 8)`),
  para leer la cadencia semanal en milestones cortos y largos. El **eje Y tiene dos modos**
  (`Scale`): en **`ABSOLUTE`** las series se escalan al **máximo global** de esfuerzo por ventana y
  el eje va en MD/h (las magnitudes comparan, pero un milestone grande aplasta al resto); en
  **`SHARE`** cada serie se escala a **su propio pico** (100%) y el eje va en **%** (las *formas* de
  cadencia comparan sin importar el tamaño). El modo lo controla un toggle de la vista y se preserva
  entre renders. Al ser un `<img>` no tiene tooltips DOM, así que lleva una **leyenda** (swatch +
  clave) en DOM real debajo. Un milestone sin worklogs fechados no aporta serie; si ninguno tiene,
  la sección se omite.
- **`TradeoffScatter`**: el gráfico de trade-off, **CSS puro** (sin SVG-as-`<img>`, patrón
  `ComparisonBarChart`/`Sparkline`). Cada milestone es un **dot** posicionado en una caja relativa
  por **duración (x, izquierda = más rápido)** y **esfuerzo (y, arriba = más)**, normalizando cada
  eje al min/max de la selección con padding (banda `[12%, 88%]`) para que los dots no toquen los
  bordes; cada dot en el `colorFor` de su milestone, con su key al lado y tooltip con duración +
  esfuerzo. Captions de eje ("← faster / slower →", "↑ more effort"). Los milestones sin duración
  medible no se ubican y se listan como "Not dated: …". Sin puntos fechados → `emptyState`.
- **`ComparisonBarChart`**: área de 140 px con una barra por milestone que escala dentro de 118 px (`BAR_AREA_PX`, deja lugar al valor arriba de cada barra). Cada `Column` lleva su **color** (el del milestone, vía `VelocityStyles.colorFor`); un color `null` cae al color por defecto. La escala usa `max(promedio, máximo de las columnas)` para que la línea de promedio siempre entre en el gráfico. La línea de referencia es solo el **promedio**: un `Div` absoluto con `border-top: 1px dashed` en `bottom = round(avg * 118 / max)` px, con su etiqueta ("avg N days") a la derecha. Una columna sin duración medible conserva su lugar con la etiqueta `n/a` y sin barra.
- **Tabla comparativa** (`VelocityView.comparisonTable`): un CSS grid `minmax(150px,200px) repeat(N, minmax(150px,1fr))` dentro de un contenedor con `overflow-x: auto`. Header con una columna por milestone (borde superior en su color + clave + nombre); filas de métricas construidas con `addRow(...)` (una función `MilestoneVelocity → celda` por fila); la fila de duración resalta el más rápido en verde; el bloque "Effort per person" agrega una fila por persona con su esfuerzo y % en cada milestone. Todo desde el `VelocityReport` ya computado.
- **`Sparkline`**: flexbox de barras finitas, altura porcentual sobre el máximo, tooltip por barra. En el tab "Per person" muestra el reparto del esfuerzo de esa persona entre los milestones seleccionados.
- **`UnitToggle`**: wrapper fino sobre `Tabs` de Vaadin. Cambiar de unidad solo re-renderiza; el report ya computado no se recalcula.
- **`CollapsibleSection`**: secciones expandibles del tab Per person (copia local del widget, movida desde el feature milestone).

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

- `velocity/VelocityView.java` — UI: selección de proyecto, resumen, gráfico + tabla comparativa, tab por persona
- `velocity/application/usecase/ComputeVelocityUseCase.java` / `LoadDeliveredMilestonesUseCase.java`
- `velocity/application/mapper/VelocityAggregator.java` / `MilestoneDelivery.java`
- `velocity/ui/style/VelocityStyles.java` (paleta), `velocity/ui/widget/EffortTimelineChart.java` (SVG héroe, modo Absolute/Share), `TradeoffScatter.java` (trade-off duración↔esfuerzo, CSS puro), `ComparisonBarChart.java`, `Sparkline.java`
- `milestone/application/usecase/LoadMilestoneDetailsUseCase.java`, `shared/config/CacheConfig.java`, `milestone/application/cache/MilestonesCacheEviction.java`

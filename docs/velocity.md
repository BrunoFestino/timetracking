# Feature: Velocity (velocidad de entrega)

Vista **Delivery Velocity** de la app de time-tracking (ruta `/velocity`, tercer ítem del menú lateral). Este documento tiene dos partes: una explicación **no técnica** (qué es, cómo usarla y cómo leer los números) y una explicación **técnica** (arquitectura, cálculo y performance).

---

## Parte 1 — Explicación no técnica

### Qué es y qué mide

La velocity responde una pregunta simple: **¿cuánto tarda el equipo en terminar un milestone?**

A diferencia de la "velocity" clásica de Scrum, acá **no se usan story points, ni cantidad de tickets cerrados, ni un ritmo semanal**. La medida es el **tiempo de entrega**: para cada milestone ya entregado, los días de calendario que pasaron entre su arranque y su entrega efectiva. Al lado de ese tiempo se muestra el **esfuerzo** que costó (los worklogs de Jira, en man-days u horas), para distinguir "tardó mucho" de "costó mucho".

La vista solo trabaja con **milestones entregados (delivered)**: un milestone en curso todavía no tiene tiempo de entrega que comparar.

### Comparar milestones de proyectos distintos

La selección **no está atada a un proyecto**. Se pueden elegir varios proyectos y armar una selección mezclada — un milestone del proyecto X y otro del proyecto Y — y tampoco importa que sean del mismo **tipo** de milestone. Cada milestone se mide contra **su propia ventana de entrega** (arranque → entrega), nunca contra el calendario, así que dos milestones que corrieron en momentos distintos siguen siendo comparables lado a lado.

### Qué preguntas de negocio responde

- **¿Cuánto tarda en promedio un milestone en terminarse?** El número principal ("Avg time to deliver") es el promedio de días de los milestones seleccionados.
- **¿Cuál se entregó más rápido y cuál más lento?** Las tarjetas "Fastest" / "Slowest" y el gráfico de barras, que ordena visualmente la selección con una línea punteada en el promedio.
- **¿Terminar rápido salió caro?** Cada milestone muestra su esfuerzo total y su **esfuerzo por día abierto** (intensidad): dos milestones de la misma duración con intensidades distintas implican equipos de tamaño distinto o tiempo muerto.
- **¿Quién participó de cada entrega, con cuánto esfuerzo y por cuánto tiempo?** El tab "Per person" desglosa la misma selección por persona.

### Cómo usar la pantalla

1. Elegir **uno o más proyectos**.
2. Seleccionar **uno o más milestones entregados** del pool combinado (aparecen los de todos los proyectos elegidos).
3. Presionar **Search**.

El toggle **MD / Hours** de la toolbar cambia solo cómo se muestra el **esfuerzo**; las duraciones siempre van en días. Cambiar la unidad no recalcula nada, solo re-renderiza.

Qué muestra:

- **Tarjeta de resumen del equipo**: tiempo promedio de entrega, cantidad de milestones y de proyectos involucrados, el más rápido y el más lento, esfuerzo total y promedio por milestone, y cantidad de contribuidores.
- **Tab "By milestone"**: un **gráfico de barras** con los días de cada milestone (la clave abajo de cada barra) y una **línea punteada con el promedio**. Debajo, una sección colapsable por milestone con sus fechas de arranque y entrega, su duración, su esfuerzo, su intensidad y el esfuerzo de cada persona con su porcentaje.
- **Tab "Per person"**: una sección colapsable por persona, con un mini gráfico del reparto de su esfuerzo entre los milestones seleccionados y, al expandir, cuánto puso en cada milestone, **cuántos días estuvo enganchada** (de su primer a su último worklog en ese milestone) y qué porcentaje del milestone representó.

### Cómo leer los números sin malinterpretarlos

- **La duración es tiempo de calendario, no esfuerzo.** Un milestone de 40 días puede haber tenido 5 días de trabajo real: los días incluyen fines de semana, esperas y pausas. Para el trabajo real está la columna de esfuerzo.
- **"Días enganchada" de una persona no es la duración del milestone.** Es el lapso entre su primer y su último worklog en ese milestone; si es mucho menor que la duración, esa persona entró para un tramo y no acompañó toda la entrega.
- **Un milestone sin fechas no promedia.** Si no se puede establecer arranque o entrega, se muestra "n/a" y queda afuera del promedio, del más rápido y del más lento (no cuenta como cero).
- **Solo cuenta lo que se registra.** El esfuerzo depende de la disciplina de carga de worklogs en Jira; el tiempo de entrega no, porque sale de las fechas del milestone.
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
| `application/dto/` | Records inmutables: `VelocityReport`, `MilestoneVelocity`, `PersonVelocity`, `PersonMilestoneEffort` |
| `ui/widget/` | Widgets de CSS puro: `ComparisonBarChart`, `Sparkline`, `UnitToggle`, `CollapsibleSection` |

Dirección de dependencias: `velocity` depende del feature `milestone` (loaders, dominio, estilos) y de `shared/jira`; **nada depende de velocity**. Es una capa de solo lectura/analítica sobre los mismos datos de Jira que usa la vista Milestone.

Convención central: **todo el esfuerzo se guarda en segundos** (`Worklog.timeSpentSeconds()`) y **toda duración se guarda en días de calendario**; la UI convierte el esfuerzo a MD u horas al renderizar según `UnitToggle`.

### Flujo de datos

```
VelocityView (@Route "velocity")
    ├── selección de proyectos → LoadDeliveredMilestonesUseCase.loadDeliveredMilestones(projectKey)
    │        └── JiraApiClient.searchMilestonesWithDeliveryByProject  (+ filtro MilestoneDelivery.isDelivered)
    └── Search → ComputeVelocityUseCase.execute(milestoneKeys) → VelocityAggregator → VelocityReport
                     └── LoadMilestoneDetailsUseCase.loadByKey (feature milestone, @Cacheable)
                             └── JiraApiClient (milestone → epics → issues → subtasks)
```

La fuente de datos es el árbol de milestone del feature `milestone`: cada `JiraTicket` trae sus `worklogs()` y sus `children()`, construido recursivamente por `LoadMilestoneDetailsUseCase.loadByKey`. Las claves de milestone del compute pueden pertenecer a proyectos distintos: cada árbol se carga por su propia clave, sin ningún parámetro de proyecto.

### Qué cuenta como "delivered"

Concentrado en `MilestoneDelivery`, para que el selector y el aggregator no puedan discrepar:

```java
// MilestoneDelivery.isDelivered
return parseDate(metadata.effectiveDeliveryDate()) != null || hasDeliveredStatus(metadata);
```

- **Entregado** si tiene *effective delivery date* (`customfield_13445`), o si su status es terminal (`delivered`, `done`, `closed`, `resolved`, `completed`, `finished`, `released`).
- **Fecha de entrega** (`deliveryDate`): effective delivery date → `resolutiondate` → último worklog.
- **Fecha de arranque** (`startDate`): el custom field de start date (`customfield_15030`) → primer worklog.
- Las fechas **planificadas** (`dueDate`, `baselineDeliveryDate`) nunca se usan como fecha de entrega: dicen cuándo *debía* entregarse, no cuándo se entregó.

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

`fastest()` / `slowest()` son el mín/máx sobre esa misma lista `datable()`. `avgSecondsPerMilestone()` sí divide por **todos** los milestones seleccionados, porque el esfuerzo se conoce aunque las fechas no.

`MilestoneVelocity.secondsPerDay()` es la intensidad: esfuerzo / días abiertos.

#### Cifras por persona — `PersonVelocity` + `PersonMilestoneEffort`

Por cada persona y cada milestone se acumulan segundos y el **primer y último día con worklog** de esa persona en ese milestone:

```java
// PersonMilestoneEffort.engagedDays
if (firstDay == null || lastDay == null || lastDay.isBefore(firstDay)) {
    return 0;
}
return (int) (lastDay.toEpochDay() - firstDay.toEpochDay()) + 1;
```

`avgEngagedDays()` promedia solo los milestones donde ese lapso es datable. El aggregator **deduplica tickets alcanzables desde más de un milestone seleccionado** con un set `seenTickets`, atribuyéndolos al primer milestone que los alcanza.

### UI: gráficos sin librería de charts

Todos los gráficos son **divs con CSS puro**, sin librería externa:

- **`ComparisonBarChart`**: área de 140 px con una barra por milestone que escala dentro de 118 px (`BAR_AREA_PX`, deja lugar al valor arriba de cada barra). La escala usa `max(promedio, máximo de las columnas)` para que la línea de promedio siempre entre en el gráfico. La línea es un `Div` absoluto con `border-top: 1px dashed` en `bottom = round(avg * 118 / max)` px, con su etiqueta ("avg N days") a la derecha. Una columna sin duración medible conserva su lugar con la etiqueta `n/a` y sin barra.
- **`Sparkline`**: flexbox de barras finitas, altura porcentual sobre el máximo, tooltip por barra. En el tab "Per person" muestra el reparto del esfuerzo de esa persona entre los milestones seleccionados.
- **`UnitToggle`**: wrapper fino sobre `Tabs` de Vaadin. Cambiar de unidad solo re-renderiza; el report ya computado no se recalcula.
- **`CollapsibleSection`**: secciones expandibles de milestone y de persona (copia local del widget, movida desde el feature milestone).

### Datos, caching y paralelismo

- **Cache**: `LoadMilestoneDetailsUseCase.loadByKey` es `@Cacheable(MILESTONE_TREE_CACHE)` con key = clave del milestone, sobre un `ConcurrentMapCacheManager` (`shared/config/CacheConfig`). Un job programado (`MilestonesCacheEviction`, cron `0 */5 * * * *`) evicta **todas** las entradas cada 5 minutos para mantener frescura. Abrir la vista Milestone primero calienta la cache que velocity después reutiliza.
- **Carga paralela**: cada árbol de milestone son varias llamadas encadenadas a Jira y la evicción de 5 minutos hace que la mayoría de los computes sean cold-load, así que `ComputeVelocityUseCase.loadTreesInParallel` submitea cada `loadByKey` a un pool fijo acotado (`PARALLEL_LOADS = 6`, moderado para respetar rate limits de Jira), preservando el orden de submission. El wall time queda en ~el batch más lento en vez de la suma de todos los milestones. Si un milestone falla, falla el compute completo. Como se invoca el bean inyectado (proxy de Spring), `@Cacheable` sigue aplicando dentro del pool.
- **Listado de milestones**: `searchMilestonesWithDeliveryByProject` es una búsqueda por proyecto (una por proyecto seleccionado) que trae status y fechas de ciclo de vida además del summary; el filtro de entregados corre en memoria, así que no depende de adivinar nombres de status en el JQL.
- **Validación**: `ComputeVelocityUseCase` valida las claves de milestone contra `^[A-Z][A-Z0-9_]+-\d+$`.

### Limitaciones y decisiones de modelado conocidas

- **Duración = calendario, no esfuerzo.** Incluye fines de semana, feriados y pausas. Es deliberado: la pregunta es "en cuánto tiempo se termina", no "cuántos días hábiles de trabajo tuvo".
- **Fallback al último worklog.** Un milestone cerrado sin effective delivery date ni resolution date se fecha con su último worklog, que subestima la entrega si el trabajo terminó antes del cierre formal.
- **Worklogs fuera del árbol**: solo cuenta el esfuerzo en tickets alcanzables desde el milestone (milestone → epics → issues → subtasks).
- **Autor desconocido**: worklogs sin autor se agrupan bajo `"Unknown"`.
- **Tamaño de la selección**: comparar milestones de tamaños muy distintos es válido para el tiempo de entrega, pero el promedio de esfuerzo por milestone mezcla peras con manzanas — mirarlo junto a la intensidad por día.

### Archivos clave

- `velocity/VelocityView.java` — UI: selección multi-proyecto, resumen, tabs por milestone y por persona
- `velocity/application/usecase/ComputeVelocityUseCase.java` / `LoadDeliveredMilestonesUseCase.java`
- `velocity/application/mapper/VelocityAggregator.java` / `MilestoneDelivery.java`
- `velocity/ui/widget/ComparisonBarChart.java`, `Sparkline.java`
- `milestone/application/usecase/LoadMilestoneDetailsUseCase.java`, `shared/config/CacheConfig.java`, `milestone/application/cache/MilestonesCacheEviction.java`

# Feature: Velocity (velocidad del equipo)

Vista **Team Velocity** de la app de time-tracking (ruta `/velocity`, tercer ítem del menú lateral). Este documento tiene dos partes: una explicación **no técnica** (qué es, cómo usarla y cómo leer los números) y una explicación **técnica** (arquitectura, cálculo y performance).

---

## Parte 1 — Explicación no técnica

### Qué es y qué mide

La velocity responde una pregunta simple: **¿cuánto trabajo registra el equipo por semana?**

A diferencia de la "velocity" clásica de Scrum, acá **no se usan story points ni cantidad de tickets cerrados**. La medida es el **tiempo registrado en los worklogs de Jira**: cada vez que alguien loguea horas en un ticket, ese esfuerzo cuenta para la semana en la que se hizo el trabajo. La vista suma ese esfuerzo, lo agrupa por semana y lo promedia.

Los valores se muestran en **man-days (MD)** o en **horas**, según el toggle **MD / Hours** de la parte superior. Cambiar la unidad solo cambia cómo se muestran los números, no el cálculo.

### Qué preguntas de negocio responde

- **¿Cuál es el ritmo semanal sostenible del equipo?** El número principal ("Team velocity") es el promedio de esfuerzo por semana.
- **¿Cómo evoluciona un milestone?** El sparkline (mini gráfico de barras) en el encabezado de cada milestone muestra si está arrancando, en su pico o terminando.
- **¿Qué semanas estuvieron por encima o por debajo del promedio?** El gráfico de barras semanal trae una línea punteada con el promedio, para leerlo de un vistazo.
- **¿Quién trabajó, cuánto y en qué?** Cada semana se puede expandir para ver el esfuerzo por persona y la lista de issues trabajados esa semana.

### Cómo usar la pantalla

La vista tiene dos modos, que se eligen con el toggle de arriba a la izquierda:

#### Modo "Per milestone" (por milestone)

Compara milestones entre sí, alineando sus arranques.

1. Elegir un **proyecto**.
2. Seleccionar **uno o más milestones**.
3. Presionar **Compute**.

Las semanas son **relativas al inicio de cada milestone**: la "Week 1" de cada milestone es su primera semana de vida, así se puede comparar el ramp-up de milestones distintos lado a lado.

Qué muestra:

- **Tarjeta de resumen del equipo**: cantidad de milestones, contribuidores, total registrado, semanas activas, la **Team velocity** (MD por semana) y la velocity por contribuidor.
- **Tab "Team velocity"**: una sección colapsable por milestone, con su sparkline de tendencia semanal y el total / promedio por semana. Al expandir, se ve cada semana relativa (por ej. "Week 1 (Jul 6 – Jul 12)") con su esfuerzo.
- **Tab "Per person"**: lo mismo pero desglosado por persona, y dentro de cada persona por milestone y semana.

#### Modo "Per week" (por semana calendario)

Mira el proyecto completo sobre la línea de tiempo real.

1. Elegir un **proyecto**.
2. Definir un **rango de fechas** con los date pickers, o usar un preset **"Last N weeks"** (4, 8 o 12 semanas).
3. Presionar **Compute**.

Acá las semanas son **semanas calendario reales, de lunes a domingo**, y se agrega todo el proyecto (no hay selección de milestones).

Qué muestra:

- **Tarjeta de resumen del equipo**: rango, semanas del rango, contribuidores, total registrado y Team velocity.
- **Tab "Team velocity"**: un **gráfico de barras** con una barra por semana (el valor arriba de cada barra, el lunes de la semana abajo) y una **línea punteada con el promedio** ("avg X MD/week"). Debajo, una sección colapsable por semana con el esfuerzo por persona y **todos los issues trabajados esa semana**.
- **Tab "Per person"**: por cada persona, un sparkline sobre el rango y su desglose semanal.

### Cómo leer los números sin malinterpretarlos

- **El rango se redondea a semanas completas.** En modo Per week, la fecha "desde" se lleva al lunes de su semana y la "hasta" al domingo. El promedio nunca divide por una semana parcial, así que la última semana no "baja" el promedio artificialmente por estar incompleta.
- **"Per contributor" es un reparto parejo, no un promedio individual.** Es la velocity del equipo dividida por la cantidad de personas que registraron tiempo; no refleja el ritmo real de cada persona (para eso está el tab "Per person").
- **Solo cuenta lo que se registra.** Si el equipo no loguea horas en Jira, la velocity se subestima. La calidad del dato depende de la disciplina de registro de worklogs.
- **Las semanas sin registro cuentan como cero.** En Per week, una semana sin worklogs aparece como columna vacía y sí baja el promedio: es deliberado, porque una semana sin trabajo registrado es información.
- **Los datos pueden tener hasta ~5 minutos de atraso.** La app cachea los datos de Jira y limpia la cache cada 5 minutos.
- **En Per milestone, mezclar milestones muy distintos distorsiona el promedio.** Ver la sección de limitaciones en la parte técnica.

---

## Parte 2 — Explicación técnica

### Arquitectura del módulo

El feature vive en `src/main/java/com/example/timetracking/velocity/`, con capas al estilo clean architecture:

| Capa | Contenido |
|---|---|
| `velocity/` (raíz) | `VelocityView` — la única ruta Vaadin del feature |
| `application/usecase/` | `ComputeVelocityUseCase`, `ComputeWeeklyVelocityUseCase` — orquestación |
| `application/mapper/` | `VelocityAggregator`, `WeeklyVelocityAggregator` — cálculo puro, sin dependencias de Jira ni de UI |
| `application/dto/` | Records inmutables: `VelocityReport`, `MilestoneVelocity`, `PersonVelocity`, `WeeklyVelocityReport`, `CalendarWeekVelocity`, `PersonWeeklyVelocity`, `IssueEffort` |
| `ui/widget/` | Widgets de CSS puro: `WeeklyBarChart`, `Sparkline`, `ModeToggle`, `UnitToggle`, `CollapsibleSection` |

Dirección de dependencias: `velocity` depende del feature `milestone` (loaders, dominio, estilos) y de `shared/jira`; **nada depende de velocity**. Es una capa de solo lectura/analítica sobre los mismos datos de Jira que usa la vista Milestone.

Convención central: **todo el esfuerzo se guarda en segundos** (`Worklog.timeSpentSeconds()`); la UI convierte a MD u horas al renderizar según `UnitToggle`.

### Flujo de datos

```
VelocityView (@Route "velocity")
    │  compute() despacha según ModeToggle.Mode
    ├── PER_MILESTONE → ComputeVelocityUseCase ──────→ VelocityAggregator ──→ VelocityReport
    └── PER_WEEK      → ComputeWeeklyVelocityUseCase → WeeklyVelocityAggregator → WeeklyVelocityReport
                              │
                              └── LoadMilestoneDetailsUseCase.loadByKey (feature milestone, @Cacheable)
                                      └── JiraApiClient (milestone → epics → issues → subtasks)
```

La fuente de datos es el árbol de milestone del feature `milestone`: cada `JiraTicket` trae sus `worklogs()` y sus `children()`, construido recursivamente por `LoadMilestoneDetailsUseCase.loadByKey`.

### El cálculo

**Atribución a semanas: por la fecha `started` del worklog** (no por fecha de resolución del ticket). Ambos aggregators parsean los primeros 10 caracteres (`yyyy-MM-dd`) con `LocalDate.parse`; una fecha inválida se descarta (per-week) o cae en la semana 1 (per-milestone).

#### Modo Per milestone — `VelocityAggregator`

Las semanas son relativas al inicio de cada milestone. El inicio es el custom field `startDate`, con fallback al worklog más temprano (`startOf`). El bucketing:

```java
// VelocityAggregator.weekOf — semana 1 arranca en el inicio del milestone
private int weekOf(LocalDate start, String worklogDate) {
    LocalDate date = parseDate(worklogDate);
    if (start == null || date == null || date.isBefore(start)) {
        return 1;
    }
    return (int) (ChronoUnit.DAYS.between(start, date) / 7) + 1;
}
```

La velocity del equipo divide el total por `teamObservedWeeks`, la **semana relativa más alta observada en toda la selección**:

```java
// VelocityAggregator.aggregate
teamObservedWeeks = Math.max(teamObservedWeeks, week);   // dentro del loop de worklogs
...
teamObservedWeeks > 0 ? totalSeconds / teamObservedWeeks : 0   // teamAvgSecondsPerWeek
```

#### Modo Per week — `ComputeWeeklyVelocityUseCase` + `WeeklyVelocityAggregator`

El use case **redondea el rango hacia afuera a semanas completas**, para que el denominador nunca sea una semana parcial:

```java
// ComputeWeeklyVelocityUseCase.execute
LocalDate snappedFrom = from.with(DayOfWeek.MONDAY);
LocalDate snappedTo = to.with(DayOfWeek.SUNDAY);
```

El aggregator agrupa por el lunes de la semana calendario de cada worklog, filtrando lo que cae fuera del rango, y **deduplica tickets alcanzables desde más de un árbol de milestone** con un set `seenTickets`:

```java
// WeeklyVelocityAggregator.collect
if (seenTickets.add(ticket.key())) {
    for (Worklog worklog : ticket.worklogs()) {
        LocalDate date = parseDate(worklog.startedDate());
        if (date == null || date.isBefore(from) || date.isAfter(to)) {
            continue;
        }
        LocalDate weekStart = date.with(DayOfWeek.MONDAY);
        ...
        teamByWeek.merge(weekStart, seconds, Long::sum);
```

El denominador y la velocity:

```java
// WeeklyVelocityAggregator.aggregate
int weeksInRange = (int) ((to.toEpochDay() - from.toEpochDay() + 1) / 7);
...
weeksInRange > 0 ? totalSeconds / weeksInRange : 0   // teamAvgSecondsPerWeek
```

Las semanas sin trabajo **se mantienen como columnas en cero** (`weeks()` itera `from.plusWeeks(i)` para las `weeksInRange` semanas), así el timeline no tiene huecos y las semanas vacías pesan en el promedio.

#### Cifras derivadas (ambos reports)

- `perContributorSecondsPerWeek = teamAvgSecondsPerWeek / contributors()` — reparto parejo, no promedio individual (`VelocityReport`, `WeeklyVelocityReport`).
- La **línea de promedio** del gráfico y el número "Team velocity" son el mismo valor: `teamAvgSecondsPerWeek`.

### UI: gráficos sin librería de charts

Todos los gráficos son **divs con CSS puro**, sin librería externa:

- **`WeeklyBarChart`**: área de 140 px con barras que escalan dentro de 118 px (`BAR_AREA_PX`, deja lugar al valor arriba de cada barra). La escala usa `max(avgSeconds, máximo de las columnas)` para que la línea de promedio siempre entre en el gráfico. La línea es un `Div` absoluto con `border-top: 1px dashed` en `bottom = round(avgSeconds * 118 / max)` px, con su etiqueta ("avg X MD/week") a la derecha.
- **`Sparkline`**: flexbox de barras finitas, altura porcentual sobre el máximo, tooltip por barra.
- **`ModeToggle` / `UnitToggle`**: wrappers finos sobre `Tabs` de Vaadin. Cambiar de unidad solo re-renderiza; los reports ya computados no se recalculan.
- **`CollapsibleSection`**: secciones expandibles de milestone/semana/persona (copia local del widget, movida desde el feature milestone).

### Datos, caching y paralelismo

- **Cache**: `LoadMilestoneDetailsUseCase.loadByKey` es `@Cacheable(MILESTONE_TREE_CACHE)` con key = clave del milestone, sobre un `ConcurrentMapCacheManager` (`shared/config/CacheConfig`). Un job programado (`MilestonesCacheEviction`, cron `0 */5 * * * *`) evicta **todas** las entradas cada 5 minutos para mantener frescura. Abrir la vista Milestone primero calienta la cache que velocity después reutiliza.
- **Carga paralela (Per week)**: como el modo Per week carga el árbol de *todos* los milestones del proyecto y la evicción de 5 minutos hace que la mayoría de los computes sean cold-load, `ComputeWeeklyVelocityUseCase.loadTreesInParallel` submitea cada `loadByKey` a un pool fijo acotado (`PARALLEL_LOADS = 6`, moderado para respetar rate limits de Jira), preservando el orden de submission. El wall time queda en ~el batch más lento en vez de la suma de todos los milestones. Si un milestone falla, falla el compute completo (mismo comportamiento que la versión secuencial). Como se invoca el bean inyectado (proxy de Spring), `@Cacheable` sigue aplicando dentro del pool.
- **Validación**: `ComputeVelocityUseCase` valida las claves de milestone contra `^[A-Z][A-Z0-9_]+-\d+$`; el weekly valida que el rango exista y que `to >= from`.

### Limitaciones y decisiones de modelado conocidas

- **Denominador en Per milestone**: la velocity de equipo divide por la semana relativa más alta observada en *toda* la selección. Si se mezclan milestones de duraciones muy distintas, el más largo domina el denominador y la velocity combinada puede quedar **subestimada** (no es un promedio ponderado por milestone). Es una decisión de diseño: el modo apunta a comparar ramp-ups, no a blends precisos — para throughput real del equipo está el modo Per week.
- **Worklogs fuera del árbol**: solo cuenta el esfuerzo en tickets alcanzables desde algún milestone del proyecto (milestone → epics → issues → subtasks). Trabajo logueado en tickets fuera de esa jerarquía no aparece.
- **Autor desconocido**: worklogs sin autor se agrupan bajo `"Unknown"`.

### Archivos clave

- `velocity/VelocityView.java` — UI, ambos modos, dispatch en `compute()`
- `velocity/application/usecase/ComputeVelocityUseCase.java` / `ComputeWeeklyVelocityUseCase.java`
- `velocity/application/mapper/VelocityAggregator.java` / `WeeklyVelocityAggregator.java`
- `velocity/ui/widget/WeeklyBarChart.java`, `Sparkline.java`
- `milestone/application/usecase/LoadMilestoneDetailsUseCase.java`, `shared/config/CacheConfig.java`, `milestone/application/cache/MilestonesCacheEviction.java`

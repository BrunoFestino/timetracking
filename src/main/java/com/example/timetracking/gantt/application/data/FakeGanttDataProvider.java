package com.example.timetracking.gantt.application.data;

import com.example.timetracking.gantt.application.model.GanttTask;
import com.example.timetracking.gantt.application.model.Role;
import com.example.timetracking.gantt.application.model.TeamMember;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Hardcoded sample dataset for the Argentina team, so the Gantt runs today with no Jira.
 *
 * <p>The whole feature reads its data through this one class; swapping it for a real
 * loader later (e.g. a Jira-backed one) requires no change to the use cases or the UI.
 *
 * <p>Dates are spread across 2026 (Feb–Nov) so the "today" marker falls mid-timeline and
 * the demo shows a mix of Done / In Progress / To Do work.
 */
@Component
public class FakeGanttDataProvider {

    private static final String DONE = "Done";
    private static final String IN_PROGRESS = "In Progress";
    private static final String TODO = "To Do";

    // ── Argentina team (2 people per role) ───────────────────────────────────────
    private static final TeamMember LUCIA = new TeamMember("Lucía Fernández", Role.BACKEND);
    private static final TeamMember MATEO = new TeamMember("Mateo Gómez", Role.BACKEND);
    private static final TeamMember SOFIA = new TeamMember("Sofía Ramírez", Role.FRONTEND);
    private static final TeamMember JULIAN = new TeamMember("Julián Torres", Role.FRONTEND);
    private static final TeamMember CAMILA = new TeamMember("Camila Rossi", Role.FULL_STACK);
    private static final TeamMember BRUNO = new TeamMember("Bruno Herrera", Role.FULL_STACK);
    private static final TeamMember VALEN = new TeamMember("Valentina Díaz", Role.DEVOPS);
    private static final TeamMember TOMAS = new TeamMember("Tomás Álvarez", Role.DEVOPS);
    private static final TeamMember MARTINA = new TeamMember("Martina Sosa", Role.MOBILE);
    private static final TeamMember NICO = new TeamMember("Nicolás Paz", Role.MOBILE);

    private static final String M1 = "M1 · Platform Foundation";
    private static final String M2 = "M2 · Customer Portal";
    private static final String M3 = "M3 · Mobile Launch";

    private final List<GanttTask> tasks = List.of(
            // ── M1 · Platform Foundation (Feb–May) ───────────────────────────────
            task("ARG-101", "Auth service & JWT", M1, "Epic · Core Services", LUCIA,
                    date(2, 3), date(3, 14), DONE),
            task("ARG-102", "User & roles API", M1, "Epic · Core Services", MATEO,
                    date(2, 17), date(4, 4), DONE),
            task("ARG-103", "CI/CD pipeline", M1, "Epic · Platform", VALEN,
                    date(2, 3), date(3, 28), DONE),
            task("ARG-104", "Kubernetes base cluster", M1, "Epic · Platform", TOMAS,
                    date(3, 3), date(5, 2), DONE),
            task("ARG-105", "Design system & tokens", M1, "Epic · Web Shell", SOFIA,
                    date(3, 10), date(4, 25), DONE),

            // ── M2 · Customer Portal (Apr–Sep) ───────────────────────────────────
            task("ARG-201", "Billing service", M2, "Epic · Billing", LUCIA,
                    date(4, 14), date(6, 20), DONE),
            task("ARG-202", "Invoices & PDF export", M2, "Epic · Billing", CAMILA,
                    date(6, 2), date(8, 15), IN_PROGRESS),
            task("ARG-203", "Portal dashboard UI", M2, "Epic · Portal Web", JULIAN,
                    date(5, 19), date(8, 8), IN_PROGRESS),
            task("ARG-204", "Notifications center", M2, "Epic · Portal Web", SOFIA,
                    date(7, 7), date(9, 12), IN_PROGRESS),
            task("ARG-205", "Observability & alerts", M2, "Epic · Platform", VALEN,
                    date(6, 16), date(8, 29), IN_PROGRESS),
            task("ARG-206", "Payments integration", M2, "Epic · Billing", BRUNO,
                    date(8, 4), date(10, 3), TODO),

            // ── M3 · Mobile Launch (Aug–Nov) ─────────────────────────────────────
            task("ARG-301", "Mobile app shell", M3, "Epic · Mobile App", MARTINA,
                    date(8, 11), date(9, 26), IN_PROGRESS),
            task("ARG-302", "Offline sync", M3, "Epic · Mobile App", NICO,
                    date(9, 8), date(10, 24), TODO),
            task("ARG-303", "Push notifications", M3, "Epic · Mobile App", MARTINA,
                    date(9, 29), date(10, 31), TODO),
            task("ARG-304", "Mobile BFF endpoints", M3, "Epic · Core Services", MATEO,
                    date(9, 1), date(10, 10), TODO),
            task("ARG-305", "App store release", M3, "Epic · Mobile App", NICO,
                    date(11, 2), date(11, 27), TODO),
            task("ARG-306", "Store CDN & rollout", M3, "Epic · Platform", TOMAS,
                    date(10, 13), date(11, 20), TODO));

    /** The flat list of planned tasks for the Argentina team. */
    public List<GanttTask> tasks() {
        return tasks;
    }

    private static GanttTask task(String key, String summary, String milestone, String epic,
                                  TeamMember assignee, LocalDate start, LocalDate end, String status) {
        return new GanttTask(key, summary, milestone, epic, assignee, start, end, status);
    }

    private static LocalDate date(int month, int day) {
        return LocalDate.of(2026, month, day);
    }
}

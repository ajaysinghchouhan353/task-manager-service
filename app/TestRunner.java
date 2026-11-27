package app;

import service.DateHelper;
import api.ApiResponse;
import api.TaskApi;
import model.Priority;
import model.TaskQuery;
import model.TaskRequest;
import repository.InMemoryTaskRepository;
import repository.IdempotencyStore;
import repository.TaskRepository;
import service.TaskService;
import parser.LlmClient;
import java.io.IOException;
import java.util.Map;

import java.util.Objects;

/**
 * INSTRUCTOR-PROVIDED EXAMPLE (Step 9): one finished check, to copy as a pattern.
 *
 * Pattern for every check:   call  ->  compare  ->  print PASS or FAIL
 *
 * Students extend this with checks against THEIR TaskApi, e.g. (once the API exists):
 *
 *   TaskApi api = new TaskApi(new TaskService(repo, ...));
 *   ApiResponse r = api.getTask(999);
 *   check("getTask unknown id -> status 404", 404, r.getStatus());
 *   check("getTask unknown id -> code NOT_FOUND", true, r.toJson().contains("\"code\":\"NOT_FOUND\""));
 *
 * Tests must run with NO key and NO internet - plug in FakeLlmClient / FaultyLlmClient.
 * Reset shared state (the Singleton repository and idempotency store!) between checks.
 *
 * When you copy this into your own project, change the package line to: package app;
 */
public class TestRunner {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        // ---- the one finished check ----
        check("2026-02-30 is rejected as a date", false, DateHelper.isValidDate("2026-02-30"));

        InMemoryTaskRepository repo = InMemoryTaskRepository.getInstance();
        repo.clear();
        IdempotencyStore.getInstance().clear();
        TaskService service = new TaskService(repo);
        TaskApi api = new TaskApi(service, repo, new FakeLlmClient());
        TaskRequest valid = new TaskRequest("Pay rent", "Monthly rent", "false", "2026-12-01", "HIGH");

        ApiResponse created = api.createTask(valid, "key-create");
        check("createTask status", 201, created.getStatus());
        check("createTask idempotency replay", created.getStatus(),
                api.createTask(valid, "key-create").getStatus());
        check("idempotency replay creates one task", 1, repo.findAll().size());
        check("same key different request conflict", 422,
                api.createTask(new TaskRequest("Other", "Other", "false", null, "LOW"), "key-create").getStatus());

        ApiResponse invalid = api.createTask(new TaskRequest("", "", "maybe", "2026-02-30", "URGENT"), null);
        check("invalid input status", 400, invalid.getStatus());
        check("invalid input code", true, invalid.toJson().contains("\"code\":\"VALIDATION_ERROR\""));
        check("lowercase priority status", 400,
                api.createTask(new TaskRequest("Lower", "Priority", "false", null, "high"), null).getStatus());
        check("unknown task status", 404, api.getTask(999).getStatus());

        FakeLlmClient countingAi = new FakeLlmClient();
        TaskApi replayApi = new TaskApi(new TaskService(repo), repo, countingAi);
        ApiResponse replayAiFirst = replayApi.createTaskFromText("Use counted fake", "counted-ai");
        ApiResponse replayAiSecond = replayApi.createTaskFromText("Use counted fake", "counted-ai");
        check("repeated AI key reuses response", replayAiFirst.getStatus(), replayAiSecond.getStatus());
        check("repeated AI key calls fake once", 1, countingAi.getCalls());

        TaskApi malformedApi = new TaskApi(new TaskService(repo), repo,
                new FaultyLlmClient("This is not JSON"));
        check("malformed AI output status", 502,
                malformedApi.createTaskFromText("malformed", null).getStatus());

        TaskApi numericFieldApi = new TaskApi(new TaskService(repo), repo,
                new FaultyLlmClient("{\"title\":123,\"description\":\"bad\",\"dueDate\":20261201,\"recurrence\":null}"));
        check("numeric model fields status", 502,
                numericFieldApi.createTaskFromText("numeric fields", null).getStatus());

        TaskApi throwingApi = new TaskApi(new TaskService(repo), repo, new ThrowingLlmClient());
        check("throwing AI client status", 502,
                throwingApi.createTaskFromText("throw", null).getStatus());

        TaskApi missingKeyApi = new TaskApi(new TaskService(repo), repo,
                new parser.HttpLlmClient("", "anthropic", null));
        check("missing API key status", 503,
                missingKeyApi.createTaskFromText("missing key", null).getStatus());

        ApiResponse ai = api.createTaskFromText("Remind me to pay rent", "key-ai");
        check("fake AI task status", 201, ai.getStatus());
        check("AI task replay", ai.getStatus(),
                api.createTaskFromText("Remind me to pay rent", "key-ai").getStatus());

        ApiResponse badAi = new TaskApi(new TaskService(repo), repo,
                new FaultyLlmClient("{\"title\":\"  \",\"description\":\"bad\",\"dueDate\":\"2026-02-30\",\"recurrence\":null}"))
                .createTaskFromText("bad model output", null);
        check("bad AI output status", 502, badAi.getStatus());

        ApiResponse sub = api.createSubtask(created.getBody() instanceof java.util.Map
                ? (Integer) ((java.util.Map<String, Object>) created.getBody()).get("id")
                : 1, new TaskRequest("Write cheque", "Write rent cheque", "false", null, "MEDIUM"));
        check("create subtask status", 201, sub.getStatus());
        check("list subtasks status", 200, api.listSubtasks(1).getStatus());
        check("listing subtasks of a subtask returns empty list", true,
                api.listSubtasks(((Integer) ((java.util.Map<String, Object>) sub.getBody()).get("id")))
                        .toJson().contains("\"body\":[]"));

        ApiResponse breakdown = api.breakDownTask(1, "key-breakdown");
        check("breakdown status", 201, breakdown.getStatus());
        check("subtask cannot break down", 400,
                api.breakDownTask(((Integer) ((java.util.Map<String, Object>) sub.getBody()).get("id")), null).getStatus());

        ApiResponse filtered = api.listTasks(TaskQuery.builder().completed(false).priority(Priority.HIGH)
                .search("rent").sortBy("createdAt").order("desc").page(1).pageSize(10).build());
        check("filter search sort page status", 200, filtered.getStatus());
        check("invalid priority status", 400, api.listByPriority("URGENT").getStatus());
        check("page beyond last page is empty", true,
                api.listTasks(TaskQuery.builder().page(999).pageSize(10).build()).toJson().contains("\"items\":[]"));
        check("overflowing page status", 400,
                api.listTasks(TaskQuery.builder().page(Integer.MAX_VALUE).pageSize(Integer.MAX_VALUE).build()).getStatus());

        ApiResponse deleted = api.deleteTask(1);
        check("delete parent status", 200, deleted.getStatus());
        check("deleted parent missing", 404, api.getTask(1).getStatus());

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    /** call -> compare -> print. 'expected' and 'actual' can be any type; equals() decides. */
    static void check(String name, Object expected, Object actual) {
        if (Objects.equals(expected, actual)) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name + "  (expected: " + expected + ", actual: " + actual + ")");
        }
    }

    static class FakeLlmClient implements LlmClient {
        private int calls;
        @Override
        public String send(String prompt) throws IOException {
            calls++;
            if (prompt.contains("one subtask per line")) {
                return "Prepare payment|Prepare the payment details\nSend payment|Send the payment";
            }
            return "{\"title\":\"Pay rent\",\"description\":\"Monthly rent\",\"dueDate\":\"2026-12-01\",\"recurrence\":\"monthly\"}";
        }
        public int getCalls() { return calls; }
    }

    static class FaultyLlmClient implements LlmClient {
        private final String reply;
        FaultyLlmClient(String reply) { this.reply = reply; }
        @Override
        public String send(String prompt) throws IOException { return reply; }
    }

    static class ThrowingLlmClient implements LlmClient {
        @Override
        public String send(String prompt) throws IOException {
            throw new IOException("fake timeout");
        }
    }
}

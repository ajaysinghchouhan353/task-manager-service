package app;

import api.ApiResponse;
import api.TaskApi;
import model.Priority;
import model.TaskQuery;
import model.TaskRequest;
import repository.InMemoryTaskRepository;
import repository.IdempotencyStore;
import service.TaskService;
import event.ConsoleLogListener;
import event.AuditTrailListener;
import parser.LlmClient;
import java.io.IOException;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        InMemoryTaskRepository repository = InMemoryTaskRepository.getInstance();
        repository.clear();
        IdempotencyStore.getInstance().clear();

        TaskService service = new TaskService(repository);
        ConsoleLogListener console = new ConsoleLogListener();
        AuditTrailListener audit = new AuditTrailListener();
        service.addListener(console);
        service.addListener(audit);

        TaskApi api = new TaskApi(service, repository, new OfflineLlmClient());
        System.out.println("Task Manager started");

        TaskRequest rent = new TaskRequest(
                "Pay rent", "Rent for December", "false", "2026-12-01", "HIGH");
        ApiResponse created = api.createTask(rent, "demo-create");
        print("createTask", created);
        print("createTask replay", api.createTask(rent, "demo-create"));
        print("createTask conflict", api.createTask(
                new TaskRequest("Pay a bill", "Different request", "false", null, "LOW"),
                "demo-create"));
        print("validation error", api.createTask(
                new TaskRequest("", "", "maybe", "2026-02-30", "URGENT"), null));
        print("unknown task", api.getTask(999));

        int parentId = taskId(created);
        print("getTask", api.getTask(parentId));
        print("createSubtask", api.createSubtask(parentId,
                new TaskRequest("Write cheque", "Prepare rent cheque",
                        "false", null, "MEDIUM")));
        print("listSubtasks", api.listSubtasks(parentId));

        System.out.println("Repository before deletion:");
        for (var task : repository.findAll()) System.out.println(task.toMap());

        print("filtered list", api.listTasks(TaskQuery.builder()
                .completed(false).priority(Priority.HIGH).search("rent")
                .sortBy("createdAt").order("desc").page(1).pageSize(10).build()));
        print("listByPriority", api.listByPriority("HIGH"));
        print("invalid priority filter", api.listByPriority("URGENT"));
        print("page beyond last page", api.listTasks(
                TaskQuery.builder().page(999).pageSize(10).build()));

        print("createTaskFromText", api.createTaskFromText(
                "Remind me to pay rent on the 1st", "demo-ai"));
        print("createTaskFromText replay", api.createTaskFromText(
                "Remind me to pay rent on the 1st", "demo-ai"));
        print("breakDownTask", api.breakDownTask(parentId, "demo-breakdown"));
        print("breakDownTask replay", api.breakDownTask(parentId, "demo-breakdown"));

        System.out.println("Audit TASK_CREATED=" + audit.getCount("TASK_CREATED"));
        service.removeListener(console);

        print("updateTask", api.updateTask(parentId,
                new TaskRequest("Pay rent", "Rent paid", "true",
                        "2026-12-01", "HIGH")));
        ApiResponse firstSubtask = api.createSubtask(parentId,
                new TaskRequest("Second cheque", "Another cheque",
                        "false", null, "MEDIUM"));
        int subtaskId = taskId(firstSubtask);
        print("subtask cannot receive subtask", api.createSubtask(subtaskId,
                new TaskRequest("Nested cheque", "Not allowed",
                        "false", null, "MEDIUM")));
        print("subtask cannot be broken down", api.breakDownTask(subtaskId, null));
        print("deleteTask", api.deleteTask(parentId));

        System.out.println("Repository after deletion:");
        for (var task : repository.findAll()) System.out.println(task.toMap());
    }

    private static void print(String operation, ApiResponse response) {
        System.out.println(operation + ": " + response);
    }

    private static int taskId(ApiResponse response) {
        if (response.getBody() instanceof Map<?, ?> body) {
            Object id = body.get("id");
            if (id instanceof Integer integer) return integer;
        }
        return 1;
    }

    static class OfflineLlmClient implements LlmClient {
        @Override
        public String send(String prompt) throws IOException {
            if (prompt.contains("one subtask per line")) {
                return "Prepare cheque|Get the cheque ready\nSend cheque|Deliver the cheque";
            }
            return "{\"title\":\"Pay rent\",\"description\":\"Monthly rent\",\"dueDate\":\"2026-12-01\",\"recurrence\":\"monthly\"}";
        }
    }
}

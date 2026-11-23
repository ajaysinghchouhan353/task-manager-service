package assignment_task_manager.app;

import assignment_task_manager.api.TaskApi;
import assignment_task_manager.model.Priority;
import assignment_task_manager.model.TaskQuery;
import assignment_task_manager.model.TaskRequest;
import assignment_task_manager.repository.InMemoryTaskRepository;
import assignment_task_manager.repository.IdempotencyStore;
import assignment_task_manager.service.TaskService;
import assignment_task_manager.event.ConsoleLogListener;
import assignment_task_manager.event.AuditTrailListener;
import assignment_task_manager.parser.LlmClient;
import java.io.IOException;

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
        System.out.println(api.createTask(
                new TaskRequest("Pay rent", "Rent for December", "false", "2026-12-01", "HIGH"), "demo-create"));
        System.out.println(api.createTask(
                new TaskRequest("Pay rent", "Rent for December", "false", "2026-12-01", "HIGH"), "demo-create"));
        System.out.println(api.createSubtask(1,
                new TaskRequest("Write cheque", "Prepare rent cheque", "false", null, "MEDIUM")));
        System.out.println("Repository before deletion:");
        for (var task : repository.findAll()) System.out.println(task.toMap());
        System.out.println(api.listTasks(TaskQuery.builder().search("rent").sortBy("createdAt")
                .order("desc").page(1).pageSize(10).build()));
        System.out.println(api.createTaskFromText("Remind me to pay rent on the 1st", "demo-ai"));
        System.out.println(api.breakDownTask(1, "demo-breakdown"));
        System.out.println("Audit TASK_CREATED=" + audit.getCount("TASK_CREATED"));
        service.removeListener(console);
        System.out.println(api.updateTask(1, new TaskRequest("Pay rent", "Rent paid", "true", "2026-12-01", "HIGH")));
        System.out.println(api.deleteTask(1));
        System.out.println("Repository after deletion:");
        for (var task : repository.findAll()) System.out.println(task.toMap());
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

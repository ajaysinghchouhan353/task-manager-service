package api;

import exception.ApiException;
import exception.BadGatewayException;
import exception.ConflictException;
import exception.FieldIssue;
import exception.ServiceUnavailableException;
import exception.ValidationException;
import model.*;
import parser.*;
import repository.IdempotencyStore;
import repository.TaskRepository;
import service.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TaskApi {
    private final TaskService service;
    private final TaskRepository repository;
    private final IdempotencyStore idempotencyStore;
    private final TaskParser taskParser;
    private final BreakdownStrategy breakdownStrategy;
    private final LlmClient llmClient;

    public TaskApi(TaskService service, TaskRepository repository, LlmClient llmClient) {
        this.service = service;
        this.repository = repository;
        this.idempotencyStore = IdempotencyStore.getInstance();
        this.taskParser = ParserFactory.taskParser(llmClient);
        this.breakdownStrategy = ParserFactory.breakdownStrategy(llmClient);
        this.llmClient = llmClient;
    }

    public ApiResponse createTask(TaskRequest request, String idempotencyKey) {
        if (idempotencyKey == null) return createTaskLocked(request);
        synchronized (idempotencyStore) {
            return createTaskLocked(request, idempotencyKey);
        }
    }

    private ApiResponse createTaskLocked(TaskRequest request) {
        return createTaskLocked(request, null);
    }

    private ApiResponse createTaskLocked(TaskRequest request, String idempotencyKey) {
        String operation = "createTask";
        String fingerprint = operation + ":" + requestFingerprint(request);
        try {
            ApiResponse replay = checkIdempotency(idempotencyKey, fingerprint);
            if (replay != null) return replay;
            Task task = service.create(request, null);
            ApiResponse response = new ApiResponse(201, task.toMap());
            storeIdempotency(idempotencyKey, fingerprint, response);
            return response;
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.fromUnexpected(e);
        }
    }

    public ApiResponse getTask(int id) {
        try { return new ApiResponse(200, service.require(id).toMap()); }
        catch (ApiException e) { return ErrorResponseFactory.from(e); }
        catch (Exception e) { return ErrorResponseFactory.fromUnexpected(e); }
    }

    public ApiResponse listTasks(TaskQuery query) {
        try {
            if (query == null) query = TaskQuery.builder().build();
            if (query.getPage() < 1) throw new ValidationException("page", "must be at least 1");
            if (query.getPageSize() < 1) throw new ValidationException("pageSize", "must be at least 1");
            if (query.getSortBy() != null && !"createdAt".equalsIgnoreCase(query.getSortBy())
                    && !"dueDate".equalsIgnoreCase(query.getSortBy()))
                throw new ValidationException("sortBy", "must be createdAt or dueDate");
            if (query.getOrder() != null && !"asc".equalsIgnoreCase(query.getOrder())
                    && !"desc".equalsIgnoreCase(query.getOrder()))
                throw new ValidationException("order", "must be asc or desc");
            List<Task> matches = new ArrayList<>(repository.findAll());
            if (query.getCompleted() != null)
                matches = new CompletedFilter(query.getCompleted()).apply(matches);
            if (query.getPriority() != null)
                matches = new PriorityFilter(query.getPriority()).apply(matches);
            if (query.getSearch() != null && !query.getSearch().isBlank())
                matches = new SearchFilter(query.getSearch()).apply(matches);
            SortStrategy sorter = "dueDate".equalsIgnoreCase(query.getSortBy())
                    ? new DueDateSort() : new CreatedAtSort();
            sorter.sort(matches, query.getOrder());
            int start = (query.getPage() - 1) * query.getPageSize();
            int end = Math.min(start + query.getPageSize(), matches.size());
            List<Map<String, Object>> page = new ArrayList<>();
            if (start < matches.size()) {
                for (Task task : matches.subList(start, end)) page.add(task.toMap());
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("items", page);
            body.put("page", query.getPage());
            body.put("pageSize", query.getPageSize());
            body.put("total", matches.size());
            return new ApiResponse(200, body);
        } catch (ApiException e) { return ErrorResponseFactory.from(e); }
        catch (Exception e) { return ErrorResponseFactory.fromUnexpected(e); }
    }

    public ApiResponse updateTask(int id, TaskRequest request) {
        try { return new ApiResponse(200, service.update(id, request).toMap()); }
        catch (ApiException e) { return ErrorResponseFactory.from(e); }
        catch (Exception e) { return ErrorResponseFactory.fromUnexpected(e); }
    }

    public ApiResponse deleteTask(int id) {
        try {
            Task task = service.require(id);
            service.delete(id);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("deleted", true);
            body.put("id", task.getId());
            return new ApiResponse(200, body);
        } catch (ApiException e) { return ErrorResponseFactory.from(e); }
        catch (Exception e) { return ErrorResponseFactory.fromUnexpected(e); }
    }

    public ApiResponse listByPriority(String level) {
        try {
            Priority priority;
            try { priority = Priority.valueOf(level.trim()); }
            catch (Exception e) { throw new ValidationException("priority", "must be LOW, MEDIUM or HIGH"); }
            return listTasks(TaskQuery.builder().priority(priority).build());
        } catch (ApiException e) { return ErrorResponseFactory.from(e); }
        catch (Exception e) { return ErrorResponseFactory.fromUnexpected(e); }
    }

    public ApiResponse createSubtask(int parentId, TaskRequest request) {
        try {
            Task parent = service.require(parentId);
            if (parent.getParentId() != null)
                throw new ValidationException("parentId", "a subtask cannot have its own subtasks");
            Task task = service.create(request, parentId);
            return new ApiResponse(201, task.toMap());
        } catch (ApiException e) { return ErrorResponseFactory.from(e); }
        catch (Exception e) { return ErrorResponseFactory.fromUnexpected(e); }
    }

    public ApiResponse listSubtasks(int parentId) {
        try {
            Task parent = service.require(parentId);
            if (parent.getParentId() != null)
                throw new ValidationException("parentId", "a subtask cannot have subtasks");
            List<Map<String, Object>> items = new ArrayList<>();
            for (Task task : repository.findAll())
                if (Integer.valueOf(parentId).equals(task.getParentId())) items.add(task.toMap());
            return new ApiResponse(200, items);
        } catch (ApiException e) { return ErrorResponseFactory.from(e); }
        catch (Exception e) { return ErrorResponseFactory.fromUnexpected(e); }
    }

    public ApiResponse createTaskFromText(String text, String idempotencyKey) {
        if (idempotencyKey == null) return createTaskFromTextLocked(text, null);
        synchronized (idempotencyStore) {
            return createTaskFromTextLocked(text, idempotencyKey);
        }
    }

    private ApiResponse createTaskFromTextLocked(String text, String idempotencyKey) {
        String fingerprint = "createTaskFromText:" + lengthValue(text);
        try {
            ApiResponse replay = checkIdempotency(idempotencyKey, fingerprint);
            if (replay != null) return replay;
            String key = System.getenv("LLM_API_KEY");
            if (llmClientIsReal() && (key == null || key.isBlank()))
                throw new ServiceUnavailableException("LLM_API_KEY is not set");
            ParsedTask parsed = taskParser.parse(text, DateHelper.today());
            TaskRequest request = new TaskRequest(parsed.getTitle(), parsed.getDescription(),
                    "false", parsed.getDueDate(), "MEDIUM");
            Task task = service.create(request, null);
            ApiResponse response = new ApiResponse(201, task.toMap());
            storeIdempotency(idempotencyKey, fingerprint, response);
            return response;
        } catch (ServiceUnavailableException e) { return ErrorResponseFactory.from(e); }
        catch (ValidationException e) {
            List<FieldIssue> gatewayIssues = new ArrayList<>();
            for (FieldIssue issue : e.getIssues()) gatewayIssues.add(new FieldIssue(issue.getField(), issue.getIssue()));
            return ErrorResponseFactory.from(new BadGatewayException("LLM response did not pass validation", gatewayIssues));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ErrorResponseFactory.from(new BadGatewayException("LLM response or call failed"));
        } catch (ApiException e) { return ErrorResponseFactory.from(e); }
        catch (Exception e) { return ErrorResponseFactory.fromUnexpected(e); }
    }

    public ApiResponse breakDownTask(int id, String idempotencyKey) {
        if (idempotencyKey == null) return breakDownTaskLocked(id, null);
        synchronized (idempotencyStore) {
            return breakDownTaskLocked(id, idempotencyKey);
        }
    }

    private ApiResponse breakDownTaskLocked(int id, String idempotencyKey) {
        String fingerprint = "breakDownTask:" + id;
        try {
            ApiResponse replay = checkIdempotency(idempotencyKey, fingerprint);
            if (replay != null) return replay;
            Task parent = service.require(id);
            if (parent.getParentId() != null)
                throw new ValidationException("parentId", "a subtask cannot be broken down");
            String key = System.getenv("LLM_API_KEY");
            if (llmClientIsReal() && (key == null || key.isBlank()))
                throw new ServiceUnavailableException("LLM_API_KEY is not set");
            List<ParsedSubtask> parsed = breakdownStrategy.breakDown(parent.getTitle(), parent.getDescription());
            List<TaskRequest> requests = new ArrayList<>();
            for (ParsedSubtask subtask : parsed) {
                requests.add(new TaskRequest(subtask.getTitle(), subtask.getDescription(), "false", null, "MEDIUM"));
            }
            List<FieldIssue> issues = new ArrayList<>();
            for (TaskRequest request : requests) {
                try { service.validate(request); }
                catch (ValidationException e) { issues.addAll(e.getIssues()); }
            }
            if (!issues.isEmpty())
                throw new BadGatewayException("LLM breakdown did not pass validation", issues);
            List<Map<String, Object>> created = new ArrayList<>();
            for (TaskRequest request : requests)
                created.add(service.create(request, id).toMap());
            ApiResponse response = new ApiResponse(201, created);
            storeIdempotency(idempotencyKey, fingerprint, response);
            return response;
        } catch (ServiceUnavailableException e) { return ErrorResponseFactory.from(e); }
        catch (BadGatewayException e) { return ErrorResponseFactory.from(e); }
        catch (IllegalArgumentException | IllegalStateException e) {
            return ErrorResponseFactory.from(new BadGatewayException("LLM breakdown response or call failed"));
        } catch (ApiException e) { return ErrorResponseFactory.from(e); }
        catch (Exception e) { return ErrorResponseFactory.fromUnexpected(e); }
    }

    private ApiResponse checkIdempotency(String key, String fingerprint) {
        if (key == null) return null;
        IdempotencyStore.ReplayResult result = idempotencyStore.replayOrReserve(key, fingerprint);
        if (result.isConflict()) {
        throw new ConflictException("Idempotency key reused with a different request");
        }
        if (!result.isNewRequest()) return result.getResponse();
        return null;
    }

    private void storeIdempotency(String key, String fingerprint, ApiResponse response) {
        if (key != null) idempotencyStore.save(key, fingerprint, response);
    }

    private static String requestFingerprint(TaskRequest request) {
        return lengthValue(request == null ? null : request.getTitle())
                + lengthValue(request == null ? null : request.getDescription())
                + lengthValue(request == null ? null : request.getCompleted())
                + lengthValue(request == null ? null : request.getDueDate())
                + lengthValue(request == null ? null : request.getPriority());
    }

    private static String lengthValue(String value) {
        if (value == null) return "N;";
        return "V" + value.length() + ":" + value + ";";
    }

    private boolean llmClientIsReal() {
        return llmClient instanceof HttpLlmClient;
    }
}

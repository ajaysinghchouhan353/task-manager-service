# Task Manager Service

A framework-free Java 17 task manager with CRUD, subtasks, AI parsing, breakdown, idempotency, filtering, sorting, pagination, validation, and observer events.

## Packages

- `model` - immutable tasks, requests, queries, and parsed AI results
- `repository` - repository interfaces and singleton in-memory stores
- `service` - validation, business logic, filters, and sort strategies
- `parser` - LLM and rule-based task parsers and breakdown strategies
- `event` - observer listeners
- `exception` - API exception hierarchy
- `api` - public operations and consistent error responses
- `app` - runnable demos and offline tests

## Setup

Requires JDK 17 or newer.

Compile from the project directory:

```powershell
New-Item -ItemType Directory -Force out\classes | Out-Null
$files = Get-ChildItem -Recurse -File -Filter *.java
javac -d out\classes -encoding UTF-8 $files.FullName
```

Run:

```powershell
java -cp out\classes app.Main
java -cp out\classes app.TestRunner
```

Set a real provider key only for LLM calls:

```powershell
$env:LLM_API_KEY="..."
$env:LLM_PROVIDER="anthropic"
$env:LLM_MODEL=""
```

Supported providers are `anthropic`, `openai`, and `gemini`. Never commit a key.

## API operations

| Operation | Parameters | Success | Errors |
|---|---|---:|---|
| `createTask` | `TaskRequest`, optional idempotency key | 201 | 400, 422, 500 |
| `getTask` | id | 200 | 404, 500 |
| `listTasks` | `TaskQuery` | 200 | 400, 500 |
| `updateTask` | id, `TaskRequest` | 200 | 400, 404, 500 |
| `deleteTask` | id | 200 | 404, 500 |
| `createTaskFromText` | text, optional idempotency key | 201 | 422, 502, 503, 500 |
| `createSubtask` | parent id, `TaskRequest` | 201 | 400, 404, 500 |
| `listSubtasks` | parent id | 200 | 404, 500 |
| `breakDownTask` | id, optional idempotency key | 201 | 400, 404, 422, 502, 503, 500 |
| `listByPriority` | priority string | 200 | 400, 500 |

Examples:

```java
TaskRequest request = new TaskRequest(
    "Pay rent", "Monthly rent", "false", "2026-12-01", "HIGH");
System.out.println(api.createTask(request, "rent-key"));
System.out.println(api.getTask(1));
System.out.println(api.listTasks(TaskQuery.builder().completed(false)
    .priority(Priority.HIGH).search("rent").sortBy("createdAt")
    .order("desc").page(1).pageSize(10).build()));
System.out.println(api.updateTask(1,
    new TaskRequest("Pay rent", "Monthly rent", "true", "2026-12-01", "HIGH")));
System.out.println(api.deleteTask(1));
System.out.println(api.createTaskFromText(
    "Remind me to pay rent on the 1st of every month", "rent-ai-key"));
System.out.println(api.createSubtask(1,
    new TaskRequest("Write cheque", "Prepare the cheque", "false", null, "MEDIUM")));
System.out.println(api.listSubtasks(1));
System.out.println(api.breakDownTask(1, "rent-breakdown-key"));
System.out.println(api.listByPriority("HIGH"));
```

Example validation result:

```json
{"status":400,"body":{"error":{"code":"VALIDATION_ERROR","message":"Validation failed","details":[{"field":"title","issue":"must not be empty"}]}}}
```

## Patterns

- Builder: `Task` and `TaskQuery`
- Singleton: `InMemoryTaskRepository` and `IdempotencyStore`
- Factory: `ParserFactory` and `ErrorResponseFactory`
- Strategy: `TaskParser`, `BreakdownStrategy`, `TaskFilter`, and `SortStrategy`
- Observer: `TaskService` listener subject with console and audit listeners
- Dependency inversion: services and APIs depend on repository, parser, filter, and sort interfaces

## Prompts

Task prompt: asks for one JSON object, includes today and the exact field format, and forbids prose or code fences. This makes relative dates resolvable and limits parsing ambiguity.

Breakdown prompt: asks for exactly `title|description` on separate lines. This allows deterministic parsing while all returned items are validated before saving.

## Tests

`TestRunner` uses fake LLM clients and is offline. The fake checklist covers CRUD, validation, impossible dates, idempotency replay/conflict, unknown IDs, AI success/failure, subtasks, breakdown, filtering, paging, and deletion.

The AI-generated checklist missed a subtask-of-subtask case, a conflict between idempotent operations, and page-beyond-last-page behavior. These cases were added explicitly.

### Contract rules

- `completed` is required in `TaskRequest` and must be exactly `"true"` or `"false"`. The `Task` Builder's Java boolean default is not an API default.
- `priority` must be exactly `LOW`, `MEDIUM`, or `HIGH`. If omitted, it defaults to `MEDIUM`.
- Pages start at 1 and page sizes are at least 1. A page beyond the last page returns `200` with an empty `items` list.
- `breakDownTask` is successful when it creates all subtasks and returns `201`.
- `recurrence` is model metadata parsed for AI task creation; it is informational and is not a field on `Task`.
- Idempotency keys are scoped by operation and fingerprint. Reusing a key for another operation or different request returns `422`.
- With a real `HttpLlmClient`, a missing or blank `LLM_API_KEY` returns `503`. Fake and rule-based parsers may be used for offline tests and explicitly offline mode.

### Pagination and sorting

- `page` and `pageSize` default to `1` and `10`.
- Negative, zero, and overflowing page values return `400 VALIDATION_ERROR`.
- `sortBy=dueDate` places tasks without a due date after dated tasks in ascending order and before them in descending order.
- Pages beyond the last page return `200` with `items=[]` and the original requested page/pageSize.

### Observer demonstration

[app.Main.java](/c:/Users/Ajay%20chouhan/Workspace/Projects/Task-Manager/task-manager-service/app/Main.java) adds `ConsoleLogListener` and `AuditTrailListener`, prints the audit count, removes `ConsoleLogListener`, and verifies that subsequent updates/deletions no longer print console events.

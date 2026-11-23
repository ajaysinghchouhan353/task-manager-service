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
mkdir out\classes
javac -d out\classes -encoding UTF-8 (Get-ChildItem -Recurse -Filter *.java).FullName
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
| `createTaskFromText` | text, optional idempotency key | 201 | 400/502 model validation, 503 key missing, 422, 500 |
| `createSubtask` | parent id, `TaskRequest` | 201 | 400, 404, 500 |
| `listSubtasks` | parent id | 200 | 400, 404, 500 |
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

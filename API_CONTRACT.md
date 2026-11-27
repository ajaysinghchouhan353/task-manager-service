# Task Manager API Contract

Every `TaskApi` method returns an `ApiResponse` with an integer `status` and a JSON-style `body`.

## Conventions

- Success bodies are task objects, lists, or small confirmation objects.
- Every failure body uses the same error shape.
- `idempotencyKey` is optional and may be `null`.

## Operations

### createTask

| | |
|---|---|
| Java method | `ApiResponse createTask(TaskRequest request, String idempotencyKey)` |
| Purpose | Create one structured task |
| Success | `201` — the created task |
| Errors | `400 VALIDATION_ERROR`, `422 CONFLICT`, `500 INTERNAL_ERROR` |

Parameters:

| Name | Required | Rules |
|---|---|---|
| `title` | yes | not empty or blank |
| `description` | yes | not empty or blank |
| `completed` | yes | exactly `"true"` or `"false"` |
| `dueDate` | no | real `YYYY-MM-DD` date |
| `priority` | no | `LOW`, `MEDIUM`, or `HIGH`; defaults to `MEDIUM` |
| `idempotencyKey` | no | replay only exact matching request |

Example request: `title="Pay rent"`, `description="Monthly rent"`, `completed="false"`, `dueDate="2026-12-01"`, `priority="HIGH"`, `idempotencyKey="rent-001"`.

Example success: `{"status":201,"body":{"id":1,"title":"Pay rent","description":"Monthly rent","completed":false,"dueDate":"2026-12-01","priority":"HIGH","parentId":null,"createdAt":1788000000000}}`

Example validation error: `{"status":400,"body":{"error":{"code":"VALIDATION_ERROR","message":"Validation failed","details":[{"field":"title","issue":"must not be empty"}]}}}`

### getTask

| | |
|---|---|
| Java method | `ApiResponse getTask(int id)` |
| Success | `200` — the task |
| Errors | `404 NOT_FOUND`, `500 INTERNAL_ERROR` |

Example request: `id=1`.

Example success: `{"status":200,"body":{"id":1,"title":"Pay rent","completed":false,"priority":"HIGH","parentId":null}}`

Example error: `{"status":404,"body":{"error":{"code":"NOT_FOUND","message":"Task 99 not found","details":[]}}}`

### Observer behavior

Observers are notified after task creation, update/completion, and deletion. `removeListener` stops console notifications from the removed listener. This behavior is demonstrated in `app.Main`.

### listTasks

| | |
|---|---|
| Java method | `ApiResponse listTasks(TaskQuery query)` |
| Success | `200` — page containing `items`, `page`, `pageSize`, and `total` |
| Errors | `400 VALIDATION_ERROR`, `500 INTERNAL_ERROR` |

Parameters: optional `completed`, `priority`, `search`, `sortBy` (`createdAt` or `dueDate`), `order` (`asc` or `desc`), `page`, and `pageSize` (default `10`).

Example request: `completed=false`, `priority=HIGH`, `search="rent"`, `sortBy="createdAt"`, `order="desc"`, `page=1`, `pageSize=10`.

Example success: `{"status":200,"body":{"items":[{"id":1,"title":"Pay rent"}],"page":1,"pageSize":10,"total":1}}`

Example error: `{"status":400,"body":{"error":{"code":"VALIDATION_ERROR","message":"Validation failed","details":[{"field":"page","issue":"must be at least 1"}]}}}`

### updateTask

| | |
|---|---|
| Java method | `ApiResponse updateTask(int id, TaskRequest request)` |
| Success | `200` — the updated task |
| Errors | `400 VALIDATION_ERROR`, `404 NOT_FOUND`, `500 INTERNAL_ERROR` |

Example request: `id=1`, `title="Pay rent"`, `completed="true"`.

Example success: `{"status":200,"body":{"id":1,"title":"Pay rent","completed":true}}`

Example error: `{"status":404,"body":{"error":{"code":"NOT_FOUND","message":"Task 99 not found","details":[]}}}`

### deleteTask

| | |
|---|---|
| Java method | `ApiResponse deleteTask(int id)` |
| Success | `200` — deletion confirmation; direct subtasks are also deleted |
| Errors | `404 NOT_FOUND`, `500 INTERNAL_ERROR` |

Example request: `id=1`.

Example success: `{"status":200,"body":{"deleted":true,"id":1}}`

Example error: `{"status":404,"body":{"error":{"code":"NOT_FOUND","message":"Task 99 not found","details":[]}}}`

### createTaskFromText

| | |
|---|---|
| Java method | `ApiResponse createTaskFromText(String text, String idempotencyKey)` |
| Success | `201` — task parsed and validated from text |
| Errors | `422 CONFLICT`, `502 BAD_GATEWAY`, `503 SERVICE_UNAVAILABLE`, `500 INTERNAL_ERROR` |

Example request: `text="Remind me to pay rent on the 1st of every month"`, `idempotencyKey="rent-ai-001"`.

Example success: `{"status":201,"body":{"title":"Pay rent","description":"Remind me to pay rent on the 1st of every month","completed":false}}`

Example error: `{"status":502,"body":{"error":{"code":"BAD_GATEWAY","message":"LLM response did not pass validation","details":[{"field":"title","issue":"must not be empty"}]}}}`

If `LLM_API_KEY` is null or blank, return `503 SERVICE_UNAVAILABLE`.

### createSubtask

| | |
|---|---|
| Java method | `ApiResponse createSubtask(int parentId, TaskRequest request)` |
| Success | `201` — the created subtask |
| Errors | `400 VALIDATION_ERROR`, `404 NOT_FOUND`, `500 INTERNAL_ERROR` |

A subtask cannot itself receive subtasks.

Example request: `parentId=1`, `title="Write cheque"`, `description="Prepare rent cheque"`, `completed="false"`.

Example success: `{"status":201,"body":{"id":2,"title":"Write cheque","parentId":1}}`

Example error: `{"status":400,"body":{"error":{"code":"VALIDATION_ERROR","message":"Validation failed","details":[{"field":"parentId","issue":"a subtask cannot have its own subtasks"}]}}}`

### listSubtasks

| | |
|---|---|
| Java method | `ApiResponse listSubtasks(int parentId)` |
| Success | `200` — a list of direct subtasks |
| Errors | `404 NOT_FOUND`, `500 INTERNAL_ERROR` |

Example request: `parentId=1`.

Example success: `{"status":200,"body":[{"id":2,"title":"Write cheque","parentId":1}]}`

Example error: `{"status":404,"body":{"error":{"code":"NOT_FOUND","message":"Task 99 not found","details":[]}}}`

A subtask may be queried with `listSubtasks`; it has no direct subtasks and therefore returns `[]`.

### breakDownTask

| | |
|---|---|
| Java method | `ApiResponse breakDownTask(int id, String idempotencyKey)` |
| Success | `201` — a list of newly created subtasks |
| Errors | `400 VALIDATION_ERROR`, `404 NOT_FOUND`, `422 CONFLICT`, `502 BAD_GATEWAY`, `503 SERVICE_UNAVAILABLE`, `500 INTERNAL_ERROR` |

Example request: `id=1`, `idempotencyKey="breakdown-001"`.

Example success: `{"status":201,"body":[{"id":2,"title":"Prepare rent","parentId":1},{"id":3,"title":"Send rent","parentId":1}]}`

Example error: `{"status":502,"body":{"error":{"code":"BAD_GATEWAY","message":"LLM breakdown did not pass validation","details":[]}}}`

Validation succeeds for every model item before any item is saved.

### listByPriority

| | |
|---|---|
| Java method | `ApiResponse listByPriority(String level)` |
| Success | `200` — matching tasks |
| Errors | `400 VALIDATION_ERROR`, `500 INTERNAL_ERROR` |

Example request: `level="HIGH"`.

Example success: `{"status":200,"body":[{"id":1,"title":"Pay rent","priority":"HIGH"}]}`

Example error: `{"status":400,"body":{"error":{"code":"VALIDATION_ERROR","message":"Validation failed","details":[{"field":"priority","issue":"must be LOW, MEDIUM or HIGH"}]}}}`

## Error shape

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [{ "field": "title", "issue": "must not be empty" }]
  }
}
```

| Status | Code | When |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Input or subtask rule is invalid |
| 404 | `NOT_FOUND` | Task or parent id does not exist |
| 422 | `CONFLICT` | Idempotency key is reused with a different request |
| 502 | `BAD_GATEWAY` | LLM call failed or model output failed validation |
| 503 | `SERVICE_UNAVAILABLE` | `LLM_API_KEY` is missing or blank |
| 500 | `INTERNAL_ERROR` | Unexpected failure; no stack trace |

## Additional contract rules

- `completed` in `TaskRequest` is required and must be exactly `"true"` or `"false"`.
- `priority` must be exactly `LOW`, `MEDIUM`, or `HIGH`; omitted priority defaults to `MEDIUM`.
- Listing starts at page 1. `pageSize` defaults to 10 and must be at least 1. A page beyond the last page returns `200` with `items: []`.
- `breakDownTask` returns `201` after all subtasks are created. A failure saves no subtasks.
- `recurrence` is informational model metadata for AI task creation and is not stored as a `Task` field.
- Idempotency keys are scoped by operation and full request fingerprint. Reusing a key across operations or with different request contents returns `422`.

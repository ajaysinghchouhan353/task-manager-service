package assignment_task_manager.model;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Task {
    private final int id;
    private final String title;
    private final String description;
    private final boolean completed;
    private final String dueDate;
    private final Priority priority;
    private final Integer parentId;
    private final long createdAt;

    private Task(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.description = builder.description;
        this.completed = builder.completed;
        this.dueDate = builder.dueDate;
        this.priority = builder.priority;
        this.parentId = builder.parentId;
        this.createdAt = builder.createdAt;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public boolean isCompleted() { return completed; }
    public String getDueDate() { return dueDate; }
    public Priority getPriority() { return priority; }
    public Integer getParentId() { return parentId; }
    public long getCreatedAt() { return createdAt; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("title", title);
        map.put("description", description);
        map.put("completed", completed);
        map.put("dueDate", dueDate);
        map.put("priority", priority.name());
        map.put("parentId", parentId);
        map.put("createdAt", createdAt);
        return map;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int id;
        private String title;
        private String description;
        private boolean completed;
        private String dueDate;
        private Priority priority = Priority.MEDIUM;
        private Integer parentId;
        private long createdAt = System.currentTimeMillis();
        private boolean completedSet;

        public Builder id(int id) { this.id = id; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder completed(boolean completed) { this.completed = completed; this.completedSet = true; return this; }
        public Builder dueDate(String dueDate) { this.dueDate = dueDate; return this; }
        public Builder priority(Priority priority) { this.priority = priority; return this; }
        public Builder parentId(Integer parentId) { this.parentId = parentId; return this; }
        public Builder createdAt(long createdAt) { this.createdAt = createdAt; return this; }
        public Task build() {
            if (title == null || title.isBlank()) throw new IllegalArgumentException("Task title is required");
            if (description == null || description.isBlank()) throw new IllegalArgumentException("Task description is required");
            if (!completedSet) throw new IllegalArgumentException("Task completed is required");
            return new Task(this);
        }
    }
}

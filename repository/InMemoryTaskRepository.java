package repository;

import model.Task;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InMemoryTaskRepository implements TaskRepository {
    private static final InMemoryTaskRepository INSTANCE = new InMemoryTaskRepository();
    private final Map<Integer, Task> tasks = Collections.synchronizedMap(new LinkedHashMap<>());
    private int nextId = 1;

    private InMemoryTaskRepository() {}

    public static InMemoryTaskRepository getInstance() { return INSTANCE; }

    @Override
    public synchronized Task save(Task task) {
        Task stored = task;
        if (task.getId() == 0) {
            stored = Task.builder().id(nextId++).title(task.getTitle()).description(task.getDescription())
                    .completed(task.isCompleted()).dueDate(task.getDueDate()).priority(task.getPriority())
                    .parentId(task.getParentId()).createdAt(task.getCreatedAt()).build();
        }
        tasks.put(stored.getId(), stored);
        return stored;
    }

    @Override
    public Task findById(int id) {
        return tasks.get(id);
    }

    @Override
    public List<Task> findAll() {
        synchronized (tasks) {
            return new ArrayList<>(tasks.values());
        }
    }

    @Override
    public synchronized boolean delete(int id) {
        return tasks.remove(id) != null;
    }

    @Override
    public synchronized void clear() {
        tasks.clear();
        nextId = 1;
    }
}

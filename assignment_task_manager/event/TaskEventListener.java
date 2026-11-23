package assignment_task_manager.event;

import assignment_task_manager.model.Task;

public interface TaskEventListener {
    void onEvent(String eventType, Task task);
}

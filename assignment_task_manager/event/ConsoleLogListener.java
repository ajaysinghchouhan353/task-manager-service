package assignment_task_manager.event;

import assignment_task_manager.model.Task;

public class ConsoleLogListener implements TaskEventListener {
    @Override
    public void onEvent(String eventType, Task task) {
        System.out.println("EVENT " + eventType + ": " + task.getId() + " " + task.getTitle());
    }
}

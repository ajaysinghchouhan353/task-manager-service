package assignment_task_manager.parser;

import assignment_task_manager.model.ParsedSubtask;
import java.util.List;

public interface BreakdownStrategy {
    List<ParsedSubtask> breakDown(String taskTitle, String taskDescription);
}

package assignment_task_manager.parser;

import assignment_task_manager.model.ParsedTask;

public interface TaskParser {
    ParsedTask parse(String text, String today);
}

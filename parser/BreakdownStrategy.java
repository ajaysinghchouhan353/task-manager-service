package parser;

import model.ParsedSubtask;
import java.util.List;

public interface BreakdownStrategy {
    List<ParsedSubtask> breakDown(String taskTitle, String taskDescription);
}

package parser;

import model.ParsedSubtask;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LlmBreakdownStrategy implements BreakdownStrategy {
    private final LlmClient client;
    public LlmBreakdownStrategy(LlmClient client) { this.client = client; }

    @Override
    public List<ParsedSubtask> breakDown(String taskTitle, String taskDescription) {
        String prompt = "Break the task into actionable subtasks. Reply with one subtask per line only, "
                + "in fixed format title|description. Do not use JSON or extra text.\n"
                + "Task title: " + taskTitle + "\nTask description: " + taskDescription;
        try {
            String reply = client.send(prompt);
            List<ParsedSubtask> result = new ArrayList<>();
            for (String line : reply.split("\\R")) {
                line = line.trim();
                if (line.isEmpty()) continue;
                int separator = line.indexOf('|');
                if (separator < 1) throw new IllegalArgumentException("Invalid breakdown line");
                result.add(new ParsedSubtask(line.substring(0, separator).trim(),
                        line.substring(separator + 1).trim()));
            }
            if (result.isEmpty()) throw new IllegalArgumentException("Breakdown returned no subtasks");
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("LLM breakdown call failed", e);
        }
    }
}

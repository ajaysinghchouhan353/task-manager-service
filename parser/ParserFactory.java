package parser;

public class ParserFactory {
    public static TaskParser taskParser(LlmClient client) {
        if (client == null) return new RuleBasedTaskParser();
        return new LlmTaskParser(client);
    }

    public static BreakdownStrategy breakdownStrategy(LlmClient client) {
        if (client == null) return new RuleBasedBreakdownStrategy();
        return new LlmBreakdownStrategy(client);
    }
}

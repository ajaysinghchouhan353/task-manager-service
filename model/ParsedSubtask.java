package model;

public class ParsedSubtask {
    private final String title;
    private final String description;

    public ParsedSubtask(String title, String description) {
        this.title = title;
        this.description = description;
    }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
}

package se.devscout.achievements.server.data.model;

public class StringSegment {
    public String text;
    public SegmentType change;

    public StringSegment(String text, SegmentType change) {
        this.text = text;
        this.change = change;
    }
}

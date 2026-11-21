package classman.render;

import classman.model.v001.TextChangeModel_V001;

import java.util.ArrayList;
import java.util.List;

/** A text which is put together from parts, some of them changed by a change set. */
public class MarkedText {
    private final StringBuilder text = new StringBuilder();
    private final List<TextMark> marks = new ArrayList<>();
    private final List<TextMark> struck = new ArrayList<>();

    /** Appends a part; the change is null if no change set changed it. */
    public MarkedText append(String part, TextChangeModel_V001 change) {
        if (change != null) {
            marks.add(new TextMark(text.length(), text.length() + part.length(), change.changeSet));
        }
        text.append(part);
        return this;
    }

    /** Appends a part which a change set has added, with the mark of the change set. */
    public MarkedText appendAdded(String part, String changeSet) {
        marks.add(new TextMark(text.length(), text.length() + part.length(), changeSet));
        text.append(part);
        return this;
    }

    /** Appends a part which a change set has removed, with the mark of the change set and a line through it. */
    public MarkedText appendRemoved(String part, String changeSet) {
        TextMark mark = new TextMark(text.length(), text.length() + part.length(), changeSet);
        marks.add(mark);
        struck.add(mark);
        text.append(part);
        return this;
    }

    public MarkedText append(String part) {
        return append(part, null);
    }

    public int length() {
        return text.length();
    }

    /** Marks the characters from start up to end, in addition to the marks of the parts which were appended. */
    public MarkedText mark(int start, int end, String changeSet) {
        marks.add(new TextMark(start, end, changeSet));
        return this;
    }

    public String text() {
        return text.toString();
    }

    public List<TextMark> marks() {
        return List.copyOf(marks);
    }

    /** The parts which have a line through them. */
    public List<TextMark> struck() {
        return List.copyOf(struck);
    }
}

package classman.render;

import classman.model.v001.TextChangeModel_V001;

import java.util.ArrayList;
import java.util.List;

/** A text which is put together from parts, some of them changed by a change set. */
public class MarkedText {
    private final StringBuilder text = new StringBuilder();
    private final List<TextMark> marks = new ArrayList<>();

    /** Appends a part; the change is null if no change set changed it. */
    public MarkedText append(String part, TextChangeModel_V001 change) {
        if (change != null) {
            marks.add(new TextMark(text.length(), text.length() + part.length(), change.changeSet));
        }
        text.append(part);
        return this;
    }

    public MarkedText append(String part) {
        return append(part, null);
    }

    public String text() {
        return text.toString();
    }

    public List<TextMark> marks() {
        return List.copyOf(marks);
    }
}

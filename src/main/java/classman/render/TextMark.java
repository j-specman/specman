package classman.render;

/** A part of a text which a change set has changed: the characters from start up to end. */
public class TextMark {
    public final int start;
    public final int end;
    public final String changeSet;

    public TextMark(int start, int end, String changeSet) {
        this.start = start;
        this.end = end;
        this.changeSet = changeSet;
    }
}

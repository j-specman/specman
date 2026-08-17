package specman.model.v002;

import specman.editarea.markups.MarkupType;
import specman.editarea.markups.TextMarkup;

import java.util.Objects;

public class Markup_V002 {
    public final int from;
    public final int to;
    public final MarkupType type;
    public final String changeset;

    @Deprecated public Markup_V002() { // For Jackson only
        from = to = 0;
        type = null;
        changeset = null;
    }

    public Markup_V002(int from, int to, TextMarkup markup) {
        this.from = from;
        this.to = to;
        this.type = markup.type;
        this.changeset = markup.changeSetName();
    }

    public Markup_V002(int from, int to, MarkupType type, String changeset) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.changeset = changeset;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Markup_V002 that = (Markup_V002) o;
        return from == that.from && to == that.to;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public String toString() {
        return from + ".." + to;
    }
}

package classman.model.v001.change;

import classman.model.v001.TextChangeModel_V001;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * One text of a diagram together with the mark which says that a change set changed it. A mandatory text always
 * exists, an optional one may be absent: a label, type, stereotype, multiplicity, info item or tagged value.
 */
final class TextSlot_V001 {
    private final Supplier<String> textGetter;
    private final Consumer<String> textSetter;
    private final Supplier<TextChangeModel_V001> markGetter;
    private final Consumer<TextChangeModel_V001> markSetter;
    private final boolean optional;

    private TextSlot_V001(Supplier<String> textGetter, Consumer<String> textSetter,
                          Supplier<TextChangeModel_V001> markGetter, Consumer<TextChangeModel_V001> markSetter,
                          boolean optional) {
        this.textGetter = textGetter;
        this.textSetter = textSetter;
        this.markGetter = markGetter;
        this.markSetter = markSetter;
        this.optional = optional;
    }

    static TextSlot_V001 mandatory(Supplier<String> textGetter, Consumer<String> textSetter,
                                   Supplier<TextChangeModel_V001> markGetter, Consumer<TextChangeModel_V001> markSetter) {
        return new TextSlot_V001(textGetter, textSetter, markGetter, markSetter, false);
    }

    static TextSlot_V001 optional(Supplier<String> textGetter, Consumer<String> textSetter,
                                  Supplier<TextChangeModel_V001> markGetter, Consumer<TextChangeModel_V001> markSetter) {
        return new TextSlot_V001(textGetter, textSetter, markGetter, markSetter, true);
    }

    String text() {
        return textGetter.get();
    }

    void setText(String text) {
        textSetter.accept(text);
    }

    /** Null if no change set changed the text. */
    TextChangeModel_V001 mark() {
        return markGetter.get();
    }

    void setMark(TextChangeModel_V001 mark) {
        markSetter.accept(mark);
    }

    boolean isOptional() {
        return optional;
    }
}

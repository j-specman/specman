package classman.render;

import java.util.ArrayList;
import java.util.List;

/** The texts of a relation as placed: the multiplicities, which can be missing, and the lines of the label. */
public class RelationTexts {
    /** Null if the relation has no multiplicity at its source. */
    public final SceneText fromMultiplicity;
    /** Null if the relation has no multiplicity at its target. */
    public final SceneText toMultiplicity;
    public final List<SceneText> labelLines;

    public RelationTexts(SceneText fromMultiplicity, SceneText toMultiplicity, List<SceneText> labelLines) {
        this.fromMultiplicity = fromMultiplicity;
        this.toMultiplicity = toMultiplicity;
        this.labelLines = List.copyOf(labelLines);
    }

    /** @return the same texts, laid out as they are, in the color and font */
    public RelationTexts restyled(String color, TextFont font) {
        return new RelationTexts(restyled(fromMultiplicity, color, font), restyled(toMultiplicity, color, font),
            labelLines.stream().map(line -> line.restyled(color, font)).toList());
    }

    private static SceneText restyled(SceneText text, String color, TextFont font) {
        return text == null ? null : text.restyled(color, font);
    }

    /** All texts: source multiplicity, target multiplicity, label lines. */
    public List<SceneText> all() {
        List<SceneText> all = new ArrayList<>();
        if (fromMultiplicity != null) {
            all.add(fromMultiplicity);
        }
        if (toMultiplicity != null) {
            all.add(toMultiplicity);
        }
        all.addAll(labelLines);
        return all;
    }
}

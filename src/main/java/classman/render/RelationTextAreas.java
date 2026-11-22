package classman.render;

import classman.model.v001.AbstractRelationModel_V001;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Tells where the texts of a relation are, so that the user can double click them to change them, and where the
 * texts which it doesn't have yet would stand, so that the user can create them. */
public class RelationTextAreas {
    private final TextMeasurer measurer;

    public RelationTextAreas(TextMeasurer measurer) {
        this.measurer = measurer;
    }

    /** @return the areas of the texts which the relation has */
    public List<SceneTextArea> of(AbstractRelationModel_V001 relation, RelationTexts texts) {
        return areas(relation, texts, false);
    }

    /** @return the areas where the texts would stand which the relation doesn't have, marked as placeholders */
    public List<SceneTextArea> placeholdersOf(AbstractRelationModel_V001 relation, RelationTexts placeholders) {
        return areas(relation, placeholders, true);
    }

    private List<SceneTextArea> areas(AbstractRelationModel_V001 relation, RelationTexts texts, boolean placeholders) {
        List<SceneTextArea> areas = new ArrayList<>();
        add(areas, relation, TextKind.FROM_MULTIPLICITY, singleLine(texts.fromMultiplicity), placeholders);
        add(areas, relation, TextKind.TO_MULTIPLICITY, singleLine(texts.toMultiplicity), placeholders);
        add(areas, relation, TextKind.RELATION_LABEL, texts.labelLines, placeholders);
        return areas;
    }

    private static List<SceneText> singleLine(SceneText text) {
        return text == null ? List.of() : List.of(text);
    }

    private void add(List<SceneTextArea> areas, AbstractRelationModel_V001 relation, TextKind kind,
                     List<SceneText> lines, boolean placeholder) {
        Rectangle2D.Double area = TextBlocks.areaOf(lines, measurer);
        if (area != null) {
            areas.add(placeholder ? SceneTextArea.placeholder(relation, kind, area)
                : new SceneTextArea(relation, kind, area));
        }
    }
}

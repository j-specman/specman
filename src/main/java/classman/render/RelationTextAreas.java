package classman.render;

import classman.model.v001.AbstractRelationModel_V001;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Tells where the texts of a relation are, so that the user can double click them to change them. */
public class RelationTextAreas {
    private final TextMeasurer measurer;

    public RelationTextAreas(TextMeasurer measurer) {
        this.measurer = measurer;
    }

    /** @return the areas of the texts which the relation has */
    public List<SceneTextArea> of(AbstractRelationModel_V001 relation, RelationTexts texts) {
        List<SceneTextArea> areas = new ArrayList<>();
        add(areas, relation, TextKind.FROM_MULTIPLICITY, texts.fromMultiplicity == null ? List.of() : List.of(texts.fromMultiplicity));
        add(areas, relation, TextKind.TO_MULTIPLICITY, texts.toMultiplicity == null ? List.of() : List.of(texts.toMultiplicity));
        add(areas, relation, TextKind.RELATION_LABEL, texts.labelLines);
        return areas;
    }

    private void add(List<SceneTextArea> areas, AbstractRelationModel_V001 relation, TextKind kind,
                     List<SceneText> lines) {
        Rectangle2D.Double area = areaOf(lines);
        if (area != null) {
            areas.add(new SceneTextArea(relation, kind, area));
        }
    }

    private Rectangle2D.Double areaOf(List<SceneText> lines) {
        Rectangle2D.Double area = null;
        for (SceneText line : lines) {
            Rectangle2D.Double lineArea = new Rectangle2D.Double(line.left(), line.y - measurer.ascent(),
                line.width, measurer.lineHeight());
            if (area == null) {
                area = lineArea;
            }
            else {
                area.add(lineArea);
            }
        }
        return area;
    }
}

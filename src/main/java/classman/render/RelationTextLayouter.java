package classman.render;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AssociationRelationModel_V001;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import static classman.render.DiagramStyle.LABEL_GAP;
import static classman.render.DiagramStyle.MULTIPLICITY_ALONG_GAP;
import static classman.render.DiagramStyle.MULTIPLICITY_BORDER_GAP;
import static classman.render.DiagramStyle.MULTIPLICITY_SIDE_GAP;

/**
 * Places the texts of a relation: the multiplicities next to the borders where the line starts and ends, and the
 * label at the middle of the longest part of the line. It doesn't avoid other texts or lines.
 */
public class RelationTextLayouter {
    private final TextMeasurer measurer;

    public RelationTextLayouter(TextMeasurer measurer) {
        this.measurer = measurer;
    }

    public List<SceneText> texts(AbstractRelationModel_V001 relation, List<Point2D.Double> path) {
        List<SceneText> texts = new ArrayList<>();
        if (relation instanceof AssociationRelationModel_V001 association) {
            addMultiplicity(texts, association.fromMultiplicity, path.get(0), path.get(1));
            addMultiplicity(texts, association.toMultiplicity, path.get(path.size() - 1), path.get(path.size() - 2));
        }
        if (relation.label != null) {
            texts.addAll(label(relation.label, path));
        }
        return texts;
    }

    // ---- multiplicities ----

    /**
     * Beside a vertical line the text stands right of the line, at the border. Beside a horizontal line it stands
     * above the line, next to the border.
     * @param border where the line touches the box
     * @param inside the next point on the line, away from the box
     */
    private void addMultiplicity(List<SceneText> texts, String multiplicity, Point2D.Double border, Point2D.Double inside) {
        if (multiplicity == null) {
            return;
        }
        double width = measurer.width(multiplicity, false);
        double dx = inside.x - border.x;
        double dy = inside.y - border.y;
        if (Math.abs(dx) > Math.abs(dy)) {
            double x = dx > 0 ? border.x + MULTIPLICITY_ALONG_GAP : border.x - MULTIPLICITY_ALONG_GAP;
            texts.add(new SceneText(x, border.y - MULTIPLICITY_SIDE_GAP, multiplicity, false,
                dx > 0 ? TextAnchor.START : TextAnchor.END, width));
        } else {
            double baseline = dy > 0 ? border.y + measurer.ascent() : border.y - MULTIPLICITY_BORDER_GAP;
            texts.add(new SceneText(border.x + MULTIPLICITY_SIDE_GAP, baseline, multiplicity, false,
                TextAnchor.START, width));
        }
    }

    // ---- label ----

    private List<SceneText> label(String label, List<Point2D.Double> path) {
        int longest = PathGeometry.longestSegment(path);
        Point2D.Double from = path.get(longest);
        Point2D.Double to = path.get(longest + 1);
        Point2D.Double middle = PathGeometry.middle(from, to);
        double middleX = middle.x;
        double middleY = middle.y;
        String[] lines = label.split("\n", -1);
        List<SceneText> texts = new ArrayList<>();
        if (PathGeometry.isHorizontal(from, to)) {
            double firstBaseline = middleY - LABEL_GAP - (lines.length - 1) * measurer.lineHeight();
            for (int i = 0; i < lines.length; i++) {
                texts.add(new SceneText(middleX, firstBaseline + i * measurer.lineHeight(), lines[i], false,
                    TextAnchor.MIDDLE, measurer.width(lines[i], false)));
            }
        } else {
            double firstBaseline = middleY - lines.length * measurer.lineHeight() / 2 + measurer.ascent();
            for (int i = 0; i < lines.length; i++) {
                texts.add(new SceneText(middleX + LABEL_GAP, firstBaseline + i * measurer.lineHeight(), lines[i], false,
                    TextAnchor.START, measurer.width(lines[i], false)));
            }
        }
        return texts;
    }
}

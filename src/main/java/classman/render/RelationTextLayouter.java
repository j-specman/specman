package classman.render;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.Side_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Places the texts of a relation: the multiplicities next to the borders where the line starts and ends, and the
 * label at the middle of the longest part of the line. A text which would cover a box, the line of its relation or
 * another text of it goes to the next free place: the other side of the line, a label then the same on the next
 * places along the line, then on the other parts of the line. Where nothing is free it stays at the first place. It
 * doesn't avoid the texts and lines of other relations.
 */
public class RelationTextLayouter {
    /** Where on a part of the line the label is tried, as a fraction of its length. */
    private static final double[] LABEL_PLACES_ALONG = {0.5, 0.35, 0.65, 0.2, 0.8};
    /** A line which leaves a box this much more along its side than away from it counts as running along the side. */
    private static final double ALONG_SIDE_FACTOR = 2;

    private final TextMeasurer measurer;
    private final LabelPlacement labelPlacement;
    private final MultiplicityPlacement multiplicityPlacement;

    public RelationTextLayouter(TextMeasurer measurer) {
        this.measurer = measurer;
        this.labelPlacement = new LabelPlacement(measurer);
        this.multiplicityPlacement = new MultiplicityPlacement(measurer);
    }

    /** @param boxes the areas of all boxes of the diagram, which the texts keep clear of */
    public RelationTexts texts(AbstractRelationModel_V001 relation, List<Point2D.Double> path,
                               Collection<Rectangle2D.Double> boxes) {
        return placeAll(new TextObstacles(boxes, path), path, fromMultiplicity(relation), toMultiplicity(relation),
            relation.label);
    }

    /** @return where the texts would stand which the relation doesn't have yet, laid out with sample texts: a label
     * for every relation, multiplicities for those which can have them. They keep clear of the texts which the
     * relation has. */
    public RelationTexts placeholders(AbstractRelationModel_V001 relation, List<Point2D.Double> path,
                                      Collection<Rectangle2D.Double> boxes) {
        TextObstacles obstacles = new TextObstacles(boxes, path);
        RelationTexts real = texts(relation, path, boxes);
        obstacles = with(obstacles, listOf(real.fromMultiplicity));
        obstacles = with(obstacles, listOf(real.toMultiplicity));
        obstacles = with(obstacles, real.labelLines);
        boolean hasMultiplicities = relation instanceof AssociationRelationModel_V001;
        return placeAll(obstacles, path,
            hasMultiplicities && isMissing(fromMultiplicity(relation)) ? DiagramStyle.MULTIPLICITY_PLACEHOLDER : null,
            hasMultiplicities && isMissing(toMultiplicity(relation)) ? DiagramStyle.MULTIPLICITY_PLACEHOLDER : null,
            isMissing(relation.label) ? DiagramStyle.LABEL_PLACEHOLDER : null);
    }

    private static String fromMultiplicity(AbstractRelationModel_V001 relation) {
        return relation instanceof AssociationRelationModel_V001 association ? association.fromMultiplicity : null;
    }

    private static String toMultiplicity(AbstractRelationModel_V001 relation) {
        return relation instanceof AssociationRelationModel_V001 association ? association.toMultiplicity : null;
    }

    private static boolean isMissing(String text) {
        return text == null || text.isEmpty();
    }

    /** Places the multiplicities, which are closest to the ends of the line, first, then the label. A text which is
     * null or empty isn't placed. */
    private RelationTexts placeAll(TextObstacles start, List<Point2D.Double> path, String fromMultiplicity,
                                   String toMultiplicity, String label) {
        TextObstacles obstacles = start;
        SceneText from = multiplicity(fromMultiplicity, path.get(0), path.get(1), obstacles);
        obstacles = with(obstacles, listOf(from));
        SceneText to = multiplicity(toMultiplicity, path.get(path.size() - 1), path.get(path.size() - 2), obstacles);
        obstacles = with(obstacles, listOf(to));
        List<SceneText> labelLines = isMissing(label) ? List.of() : label(label, path, obstacles);
        return new RelationTexts(from, to, labelLines);
    }

    private TextObstacles with(TextObstacles obstacles, List<SceneText> lines) {
        Rectangle2D.Double area = TextBlocks.areaOf(lines, measurer);
        return area == null ? obstacles : obstacles.with(area);
    }

    private static List<SceneText> listOf(SceneText text) {
        return text == null ? List.of() : List.of(text);
    }

    private SceneText multiplicity(String multiplicity, Point2D.Double border, Point2D.Double inside,
                                   TextObstacles obstacles) {
        if (isMissing(multiplicity)) {
            return null;
        }
        double width = measurer.width(multiplicity, false);
        boolean leavesSideways = leavesSideways(border, inside, obstacles, width);
        List<List<SceneText>> candidates = new ArrayList<>();
        for (LineSide side : sidesStartingWith(hugsOtherSide(border, inside, leavesSideways))) {
            candidates.add(List.of(multiplicityPlacement.atEnd(multiplicity, border, inside, side, leavesSideways)));
        }
        return firstFree(candidates, obstacles).get(0);
    }

    /** The line leaves a box at its left or right side: sideways. Where the border is on no side of a box, on a
     * corner, or where the line runs along the side more than away from it, the text decides: it stands where it has
     * to be moved least to clear the slanted line. */
    private boolean leavesSideways(Point2D.Double border, Point2D.Double inside, TextObstacles obstacles,
                                   double width) {
        Side_V001 side = obstacles.boxSideAt(border);
        if (side != null) {
            boolean leftOrRight = side == Side_V001.LEFT || side == Side_V001.RIGHT;
            double away = Math.abs(leftOrRight ? inside.x - border.x : inside.y - border.y);
            double along = Math.abs(leftOrRight ? inside.y - border.y : inside.x - border.x);
            if (away * ALONG_SIDE_FACTOR >= along) {
                return leftOrRight;
            }
        }
        return PathGeometry.runsSideways(border, inside, width, measurer.lineHeight());
    }

    /** A text next to a box stands closest to the border on the side where the line goes away from the text: below
     * a line which rises away from the box, above one which falls, left of one which goes right, right of one which
     * goes left. On the other side the text has to be moved to clear the line.
     * @return true if that is the other side than the usual one: below or left */
    private static boolean hugsOtherSide(Point2D.Double border, Point2D.Double inside, boolean leavesSideways) {
        return leavesSideways ? inside.y < border.y : inside.x > border.x;
    }

    private static List<LineSide> sidesStartingWith(boolean other) {
        return other ? List.of(LineSide.OTHER, LineSide.USUAL) : List.of(LineSide.USUAL, LineSide.OTHER);
    }

    private List<SceneText> label(String label, List<Point2D.Double> path, TextObstacles obstacles) {
        List<List<SceneText>> candidates = new ArrayList<>();
        for (int segment : PathGeometry.segmentsByLength(path)) {
            for (double along : LABEL_PLACES_ALONG) {
                for (LineSide side : LineSide.values()) {
                    candidates.add(labelPlacement.beside(label, path.get(segment), path.get(segment + 1), along, side));
                }
            }
        }
        return firstFree(candidates, obstacles);
    }

    /** @return the first candidate which covers nothing, the first one if all do */
    private List<SceneText> firstFree(List<List<SceneText>> candidates, TextObstacles obstacles) {
        for (List<SceneText> candidate : candidates) {
            if (!obstacles.covers(TextBlocks.areaOf(candidate, measurer))) {
                return candidate;
            }
        }
        return candidates.get(0);
    }
}

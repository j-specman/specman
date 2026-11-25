package classman.render;

import classman.model.v001.TextPlaceModel_V001;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/** Places the texts of a relation which the user has put somewhere, relative to the line of the relation: the middle of
 * the text stands where the place says. It does not avoid anything, the user has chosen the place. */
class ManualTextPlacement {
    private final TextMeasurer measurer;

    ManualTextPlacement(TextMeasurer measurer) {
        this.measurer = measurer;
    }

    /** @return the lines of a label which stands at a percent of the length of the route */
    List<SceneText> label(String label, TextPlaceModel_V001 place, List<Point2D.Double> path) {
        double distance = PathGeometry.length(path) * place.along / 100;
        return around(PathGeometry.pointBeside(path, distance, place.across), label.split("\n", -1));
    }

    /** @return a multiplicity which stands a distance along the route from the end where it belongs
     * @param atStart true for the multiplicity at the source of the relation, false for the one at its target */
    SceneText multiplicity(String multiplicity, TextPlaceModel_V001 place, List<Point2D.Double> path, boolean atStart) {
        double distance = atStart ? place.along : PathGeometry.length(path) - place.along;
        return around(PathGeometry.pointBeside(path, distance, place.across), new String[] {multiplicity}).get(0);
    }

    private List<SceneText> around(Point2D.Double middle, String[] lines) {
        double top = middle.y - lines.length * measurer.lineHeight() / 2;
        List<SceneText> texts = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            texts.add(new SceneText(middle.x, top + measurer.ascent() + i * measurer.lineHeight(), lines[i], false,
                TextAnchor.MIDDLE, measurer.width(lines[i], false)));
        }
        return texts;
    }
}

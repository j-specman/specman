package classman.render;

import classman.model.v001.NoteModel_V001;

import java.util.ArrayList;
import java.util.List;

import static classman.render.DiagramStyle.MIN_BOX_WIDTH;
import static classman.render.DiagramStyle.NOTE_FILL;
import static classman.render.DiagramStyle.NOTE_FOLD;
import static classman.render.DiagramStyle.NOTE_MAX_AUTO_WIDTH;
import static classman.render.DiagramStyle.PADDING_X;
import static classman.render.DiagramStyle.PADDING_Y;
import static classman.render.DiagramStyle.STROKE;

/** Lays out one note: a box with a folded corner and left aligned, wrapped text. */
public class NoteBoxLayouter {
    private final TextMeasurer measurer;
    private final TextWrapper wrapper;

    public NoteBoxLayouter(TextMeasurer measurer) {
        this.measurer = measurer;
        this.wrapper = new TextWrapper(measurer);
    }

    public BoxLayout layout(NoteModel_V001 model) {
        double contentWidth = contentWidth(model);
        double width = contentWidth + 2 * PADDING_X;
        List<String> lines = wrapper.wrap(model.text, contentWidth, contentWidth, false);
        double height = 2 * PADDING_Y + lines.size() * measurer.lineHeight();

        List<AbstractSceneElement> elements = new ArrayList<>();
        elements.add(new ScenePolygon(
            new double[]{0, width - NOTE_FOLD, width, width, 0},
            new double[]{0, 0, NOTE_FOLD, height, height}, NOTE_FILL, STROKE));
        elements.add(new ScenePolygon(
            new double[]{width - NOTE_FOLD, width - NOTE_FOLD, width},
            new double[]{0, NOTE_FOLD, NOTE_FOLD}, null, STROKE));
        double lineY = PADDING_Y;
        for (String line : lines) {
            elements.add(new SceneText(PADDING_X, lineY + measurer.ascent(), line, false, TextAnchor.START,
                measurer.width(line, false)));
            lineY += measurer.lineHeight();
        }
        return new BoxLayout(width, height, elements);
    }

    private double contentWidth(NoteModel_V001 model) {
        double longestWord = wrapper.longestWord(model.text, false);
        if (model.width != null) {
            return Math.max(model.width - 2 * PADDING_X, longestWord);
        }
        double natural = Math.min(wrapper.widestLine(model.text, false), NOTE_MAX_AUTO_WIDTH - 2 * PADDING_X);
        return Math.max(Math.max(natural, longestWord), MIN_BOX_WIDTH - 2 * PADDING_X);
    }
}

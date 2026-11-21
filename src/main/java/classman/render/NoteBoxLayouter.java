package classman.render;

import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.NoteModel_V001;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static classman.render.DiagramStyle.FRAME_STROKE_WIDTH;
import static classman.render.DiagramStyle.MIN_BOX_WIDTH;
import static classman.render.DiagramStyle.NOTE_FILL;
import static classman.render.DiagramStyle.NOTE_FOLD;
import static classman.render.DiagramStyle.NOTE_MAX_AUTO_WIDTH;
import static classman.render.DiagramStyle.NOTE_RIGHT_PADDING;
import static classman.render.DiagramStyle.PADDING_X;
import static classman.render.DiagramStyle.PADDING_Y;
import static classman.render.DiagramStyle.STROKE;
import static classman.render.DiagramStyle.STROKE_WIDTH;
import static classman.render.DiagramStyle.VEIL_SHARE;
import static classman.render.DiagramStyle.VEILED_STROKE;
import static classman.render.DiagramStyle.VEILED_MARKED_TEXT;
import static classman.render.DiagramStyle.WHITE;

/**
 * Lays out one note: a box with a folded corner and left aligned, wrapped text. An added or removed note gets a frame
 * around its outline, a removed one is veiled and its text struck through. A changed text gets a colored background.
 */
public class NoteBoxLayouter {
    private final TextMeasurer measurer;
    private static final double HORIZONTAL_PADDING = PADDING_X + NOTE_RIGHT_PADDING;

    private final TextWrapper wrapper;
    private final ChangePalette palette;
    private final TextHighlighter highlighter;

    public NoteBoxLayouter(TextMeasurer measurer, ChangePalette palette) {
        this.measurer = measurer;
        this.wrapper = new TextWrapper(measurer);
        this.palette = palette;
        this.highlighter = new TextHighlighter(measurer, palette);
    }

    public BoxLayout layout(NoteModel_V001 model) {
        ChangeModel_V001 change = model.change;
        boolean isRemoved = change != null && change.type == ChangeType_V001.REMOVED;
        String lineColor = isRemoved ? VEILED_STROKE : STROKE;
        double contentWidth = contentWidth(model);
        double width = contentWidth + HORIZONTAL_PADDING;
        List<String> lines = wrapper.wrap(model.text, contentWidth, contentWidth, false);
        double height = 2 * PADDING_Y + lines.size() * measurer.lineHeight();

        double[] outlineXs = {0, width - NOTE_FOLD, width, width, 0};
        double[] outlineYs = {0, 0, NOTE_FOLD, height, height};
        List<AbstractSceneElement> elements = new ArrayList<>();
        if (change != null) {
            elements.add(new ScenePolygon(outlineXs, outlineYs, null, palette.strong(change.changeSet), FRAME_STROKE_WIDTH)
                .keepingVisible());
        }
        elements.add(new ScenePolygon(outlineXs, outlineYs, isRemoved ? ColorMix.blend(NOTE_FILL, WHITE, VEIL_SHARE) : NOTE_FILL,
            lineColor));
        elements.add(new ScenePolygon(
            new double[]{width - NOTE_FOLD, width - NOTE_FOLD, width},
            new double[]{0, NOTE_FOLD, NOTE_FOLD}, null, lineColor, STROKE_WIDTH));
        List<SceneText> placed = new ArrayList<>();
        double lineY = PADDING_Y;
        for (String line : lines) {
            placed.add(new SceneText(PADDING_X, lineY + measurer.ascent(), line, false, TextAnchor.START,
                measurer.width(line, false), isRemoved ? VEILED_MARKED_TEXT : null));
            lineY += measurer.lineHeight();
        }
        if (change == null) {
            elements.addAll(highlighter.wholeHighlights(model.textChange, placed));
        }
        if (isRemoved) {
            elements.addAll(highlighter.wholeHighlights(change.changeSet, placed));
        }
        elements.addAll(placed);
        if (isRemoved) {
            elements.addAll(highlighter.strikes(placed));
        }
        return new BoxLayout(width, height, elements, Map.of(), change == null ? 0 : FRAME_STROKE_WIDTH / 2, null);
    }

    private double contentWidth(NoteModel_V001 model) {
        double longestWord = wrapper.longestWord(model.text, false);
        if (model.width != null) {
            return Math.max(model.width - HORIZONTAL_PADDING, longestWord);
        }
        double natural = Math.min(wrapper.widestLine(model.text, false), NOTE_MAX_AUTO_WIDTH - HORIZONTAL_PADDING);
        return Math.max(Math.max(natural, longestWord), MIN_BOX_WIDTH - HORIZONTAL_PADDING);
    }
}

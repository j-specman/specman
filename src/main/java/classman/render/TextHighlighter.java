package classman.render;

import classman.model.v001.TextChangeModel_V001;

import java.util.ArrayList;
import java.util.List;

import static classman.render.DiagramStyle.HIGHLIGHT_MARGIN;
import static classman.render.DiagramStyle.STRIKE_COLOR;
import static classman.render.DiagramStyle.STRIKE_HEIGHT_PART;
import static classman.render.DiagramStyle.STRIKE_WIDTH;

/** Makes the marks which show that a text is changed or removed: a colored background, or a line through the text. */
public class TextHighlighter {
    private final TextMeasurer measurer;
    private final ChangePalette palette;
    private final TextDiffMarks diffMarks = new TextDiffMarks();

    public TextHighlighter(TextMeasurer measurer, ChangePalette palette) {
        this.measurer = measurer;
        this.palette = palette;
    }

    /** The backgrounds for all marks of a text which is shown in the given lines, in the order of the lines. */
    public List<SceneRectangle> highlights(MarkedText marked, List<SceneText> lines) {
        int[] lineStarts = new int[lines.size()];
        int lineStart = 0;
        for (int i = 0; i < lines.size(); i++) {
            lineStarts[i] = lineStart;
            lineStart += lines.get(i).text.length() + 1;
        }
        return highlights(marked.marks(), lineStarts, lines);
    }

    /**
     * The backgrounds for a text which a change set changed, shown in the given lines. They cover what differs from
     * the backup, or all of the text if the difference isn't worth showing in detail. None if the change is null.
     */
    public List<SceneRectangle> changeHighlights(TextChangeModel_V001 change, String text, List<SceneText> lines) {
        if (change == null) {
            return List.of();
        }
        List<TextMark> marks = diffMarks.of(change.backup, text == null ? "" : text, change.changeSet);
        return highlights(marks, lineStartsIn(text == null ? "" : text, lines), lines);
    }

    /** Where each line starts in the text. The lines are the text without the blanks and line breaks at which it was
     * broken, so each line is looked for after the end of the one before. */
    private int[] lineStartsIn(String text, List<SceneText> lines) {
        int[] lineStarts = new int[lines.size()];
        int position = 0;
        for (int i = 0; i < lines.size(); i++) {
            int found = text.indexOf(lines.get(i).text, position);
            lineStarts[i] = found >= 0 ? found : position;
            position = lineStarts[i] + lines.get(i).text.length();
        }
        return lineStarts;
    }

    private List<SceneRectangle> highlights(List<TextMark> marks, int[] lineStarts, List<SceneText> lines) {
        List<SceneRectangle> highlights = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            SceneText line = lines.get(i);
            int lineStart = lineStarts[i];
            int lineEnd = lineStart + line.text.length();
            for (TextMark mark : marks) {
                int from = Math.max(mark.start, lineStart) - lineStart;
                int to = Math.min(mark.end, lineEnd) - lineStart;
                if (from < to) {
                    highlights.add(partBackground(line, from, to, palette.strong(mark.changeSet)).keepingVisible());
                }
            }
        }
        return highlights;
    }

    /** The backgrounds for the lines of a text which is changed as a whole, none if the change is null. */
    public List<SceneRectangle> wholeHighlights(TextChangeModel_V001 change, List<SceneText> lines) {
        return change == null ? List.of() : keptVisible(backgrounds(lines, palette.strong(change.changeSet)));
    }

    /** The backgrounds for the lines of a text in the strong color of the change set. */
    public List<SceneRectangle> wholeHighlights(String changeSet, List<SceneText> lines) {
        return keptVisible(backgrounds(lines, palette.strong(changeSet)));
    }

    /** The highlights in the strong color are the marks which painters keep recognizable when the diagram is small. */
    private List<SceneRectangle> keptVisible(List<SceneRectangle> highlights) {
        return highlights.stream().map(SceneRectangle::keepingVisible).toList();
    }

    /** A background in the given color behind each line which isn't empty. */
    public List<SceneRectangle> backgrounds(List<SceneText> lines, String color) {
        List<SceneRectangle> backgrounds = new ArrayList<>();
        for (SceneText line : lines) {
            if (!line.text.isEmpty()) {
                backgrounds.add(partBackground(line, 0, line.text.length(), color));
            }
        }
        return backgrounds;
    }

    /** The lines through the texts. */
    public List<SceneLine> strikes(List<SceneText> lines) {
        List<SceneLine> strikes = new ArrayList<>();
        for (SceneText line : lines) {
            double y = line.y - measurer.ascent() * STRIKE_HEIGHT_PART;
            strikes.add(new SceneLine(line.left() - 1, y, line.left() + line.width + 1, y, false, STRIKE_COLOR,
                STRIKE_WIDTH));
        }
        return strikes;
    }

    private SceneRectangle partBackground(SceneText line, int from, int to, String color) {
        double left = line.left() + measurer.width(line.text.substring(0, from), line.bold);
        double right = line.left() + measurer.width(line.text.substring(0, to), line.bold);
        return new SceneRectangle(left - HIGHLIGHT_MARGIN, line.y - measurer.ascent() + 1,
            right - left + 2 * HIGHLIGHT_MARGIN, measurer.lineHeight() - 2, color, null, DiagramStyle.STROKE_WIDTH);
    }
}

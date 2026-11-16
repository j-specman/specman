package classman.render;

import java.util.ArrayList;
import java.util.List;

/** Breaks text into lines at spaces, and measures words. */
public class TextWrapper {
    private final TextMeasurer measurer;

    public TextWrapper(TextMeasurer measurer) {
        this.measurer = measurer;
    }

    /**
     * Wraps every line of the text at spaces. A word which is too wide gets a line for itself.
     * @param firstLineWidth the room for the first line of the text
     * @param otherLinesWidth the room for all following lines
     */
    public List<String> wrap(String text, double firstLineWidth, double otherLinesWidth, boolean bold) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            wrapParagraph(paragraph, firstLineWidth, otherLinesWidth, bold, lines);
        }
        return lines;
    }

    private void wrapParagraph(String paragraph, double firstLineWidth, double otherLinesWidth, boolean bold,
                               List<String> lines) {
        String line = "";
        double available = lines.isEmpty() ? firstLineWidth : otherLinesWidth;
        for (String word : paragraph.split(" ", -1)) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (line.isEmpty() || measurer.width(candidate, bold) <= available) {
                line = candidate;
            } else {
                lines.add(line);
                line = word;
                available = otherLinesWidth;
            }
        }
        lines.add(line);
    }

    /** The width of the widest word in the text, which no wrapping can make smaller. */
    public double longestWord(String text, boolean bold) {
        double longest = 0;
        for (String word : text.split("[ \n]")) {
            longest = Math.max(longest, measurer.width(word, bold));
        }
        return longest;
    }

    /** The width of the widest line of the text, if it isn't wrapped. */
    public double widestLine(String text, boolean bold) {
        double widest = 0;
        for (String line : text.split("\n", -1)) {
            widest = Math.max(widest, measurer.width(line, bold));
        }
        return widest;
    }
}

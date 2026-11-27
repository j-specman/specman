package classman.render;

import java.util.List;

/** The picture shown instead of a linked diagram which can't be shown: the red exclamation mark, the link in the color
 * of a link, and what is wrong. */
final class LinkPlaceholderScene {
    private static final String LINK_COLOR = "#0066cc";
    private static final double ICON_SIZE = 24;
    private static final double PADDING = 8;
    private static final double GAP = 6;

    private final TextMeasurer measurer;

    LinkPlaceholderScene(TextMeasurer measurer) {
        this.measurer = measurer;
    }

    Scene of(String link, String problem) {
        double linkWidth = measurer.width(link, false);
        double problemWidth = measurer.width(problem, false);
        double textHeight = 2 * measurer.lineHeight();
        double height = Math.max(ICON_SIZE, textHeight) + 2 * PADDING;
        double textX = PADDING + ICON_SIZE + GAP;
        double linkBaseline = (height - textHeight) / 2 + measurer.ascent();
        List<AbstractSceneElement> elements = List.of(
            new SceneVectorImage(PADDING, (height - ICON_SIZE) / 2, ICON_SIZE, ICON_SIZE, ProblemIcon.get()),
            new SceneText(textX, linkBaseline, link, false, TextAnchor.START, linkWidth, LINK_COLOR),
            new SceneText(textX, linkBaseline + measurer.lineHeight(), problem, false, TextAnchor.START, problemWidth));
        return new Scene(elements, 0, 0, textX + Math.max(linkWidth, problemWidth) + PADDING, height);
    }
}

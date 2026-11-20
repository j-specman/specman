package classman.render;

import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.InfoModel_V001;
import classman.model.v001.TaggedValueModel_V001;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static classman.render.DiagramStyle.ATTRIBUTE_ATTACH_GAP;
import static classman.render.DiagramStyle.CLASS_FILL;
import static classman.render.DiagramStyle.CONTINUATION_INDENT;
import static classman.render.DiagramStyle.FRAME_STROKE_WIDTH;
import static classman.render.DiagramStyle.MIN_BOX_WIDTH;
import static classman.render.DiagramStyle.PADDING_X;
import static classman.render.DiagramStyle.PADDING_Y;
import static classman.render.DiagramStyle.STROKE;
import static classman.render.DiagramStyle.STROKE_WIDTH;
import static classman.render.DiagramStyle.VEIL_SHARE;
import static classman.render.DiagramStyle.VEILED_STROKE;
import static classman.render.DiagramStyle.VEILED_TEXT;
import static classman.render.DiagramStyle.WHITE;

/**
 * Lays out one class box: header with stereotype, name and tagged values, then the attributes. An added or removed
 * class gets a frame, a removed one is veiled and its name struck through. The texts and attributes of such a class
 * are not marked on their own. In any other class a changed text gets a colored background, an added or removed
 * attribute a colored row.
 */
public class ClassBoxLayouter {
    private final TextMeasurer measurer;
    private final TextWrapper wrapper;
    private final ChangePalette palette;
    private final TextHighlighter highlighter;

    public ClassBoxLayouter(TextMeasurer measurer, ChangePalette palette) {
        this.measurer = measurer;
        this.wrapper = new TextWrapper(measurer);
        this.palette = palette;
        this.highlighter = new TextHighlighter(measurer, palette);
    }

    public BoxLayout layout(ClassModel_V001 model) {
        return layout(model, CLASS_FILL);
    }

    /** Lays out the class with the given fill color for its box. */
    public BoxLayout layout(ClassModel_V001 model, String fill) {
        ChangeModel_V001 change = model.change;
        boolean isRemoved = change != null && change.type == ChangeType_V001.REMOVED;
        String textColor = isRemoved ? VEILED_TEXT : null;
        String lineColor = isRemoved ? VEILED_STROKE : STROKE;
        String stereotype = model.stereotype == null ? null : "<<" + model.stereotype + ">>";
        MarkedText taggedValues = taggedValues(model);
        List<MarkedText> attributes = attributeTexts(model);
        double contentWidth = contentWidth(model, stereotype, taggedValues, attributes);
        double width = contentWidth + 2 * PADDING_X;

        List<AbstractSceneElement> backgrounds = new ArrayList<>();
        List<AbstractSceneElement> elements = new ArrayList<>();
        List<AbstractSceneElement> overlays = new ArrayList<>();
        double y = PADDING_Y;
        List<SceneText> stereotypeLines = new ArrayList<>();
        y = addCenteredText(elements, stereotypeLines, stereotype, false, contentWidth, width, y, textColor);
        List<SceneText> nameLines = new ArrayList<>();
        y = addCenteredText(elements, nameLines, model.name, true, contentWidth, width, y, textColor);
        List<SceneText> taggedLines = new ArrayList<>();
        y = addCenteredText(elements, taggedLines, taggedValues == null ? null : taggedValues.text(), false,
            contentWidth, width, y, textColor);
        y += PADDING_Y;
        elements.add(new SceneLine(0, y, width, y, false, lineColor, STROKE_WIDTH));
        y += PADDING_Y;
        Map<String, Rectangle2D.Double> attributeAreas = new LinkedHashMap<>();
        y = addAttributes(elements, backgrounds, overlays, model, attributes, width, y, attributeAreas, textColor);
        y += PADDING_Y;

        if (change == null) {
            addHeaderMarks(backgrounds, model, stereotypeLines, nameLines, taggedValues, taggedLines);
        } else if (isRemoved) {
            overlays.addAll(highlighter.strikes(nameLines));
        }
        List<AbstractSceneElement> all = new ArrayList<>();
        if (change != null) {
            all.add(frame(width, y, change));
        }
        all.add(new SceneRectangle(0, 0, width, y, isRemoved ? veiled(fill) : fill, lineColor));
        all.addAll(backgrounds);
        all.addAll(elements);
        all.addAll(overlays);
        return new BoxLayout(width, y, all, attributeAreas, change == null ? 0 : FRAME_STROKE_WIDTH / 2);
    }

    private SceneRectangle frame(double width, double height, ChangeModel_V001 change) {
        return new SceneRectangle(0, 0, width, height, null, palette.strong(change.changeSet), FRAME_STROKE_WIDTH)
            .keepingVisible();
    }

    private String veiled(String fill) {
        return ColorMix.blend(fill, WHITE, VEIL_SHARE);
    }

    private void addHeaderMarks(List<AbstractSceneElement> backgrounds, ClassModel_V001 model,
                                List<SceneText> stereotypeLines, List<SceneText> nameLines, MarkedText taggedValues,
                                List<SceneText> taggedLines) {
        backgrounds.addAll(highlighter.wholeHighlights(model.stereotypeChange, stereotypeLines));
        backgrounds.addAll(highlighter.wholeHighlights(model.nameChange, nameLines));
        if (taggedValues != null) {
            backgrounds.addAll(highlighter.highlights(taggedValues, taggedLines));
        }
    }

    // ---- texts ----

    private MarkedText taggedValues(ClassModel_V001 model) {
        if (model.taggedValues.isEmpty()) {
            return null;
        }
        MarkedText text = new MarkedText().append("{");
        boolean first = true;
        for (TaggedValueModel_V001 tag : model.taggedValues) {
            if (!first) {
                text.append(", ");
            }
            first = false;
            text.append(tag.key, tag.keyChange).append("=").append(tag.value, tag.valueChange);
        }
        return text.append("}");
    }

    private List<MarkedText> attributeTexts(ClassModel_V001 model) {
        List<MarkedText> texts = new ArrayList<>();
        for (AttributeModel_V001 attribute : model.attributes) {
            texts.add(attributeText(attribute));
        }
        return texts;
    }

    private MarkedText attributeText(AttributeModel_V001 attribute) {
        MarkedText text = new MarkedText();
        if (attribute.hiddenAttributesMarker) {
            return text.append("...");
        }
        if (attribute.visibility != null) {
            text.append(String.valueOf(attribute.visibility.sign));
        }
        text.append(attribute.name, attribute.nameChange);
        if (attribute.type != null) {
            text.append(" : ").append(attribute.type, attribute.typeChange);
        }
        if (!attribute.infos.isEmpty()) {
            text.append(" [");
            boolean first = true;
            for (InfoModel_V001 info : attribute.infos) {
                if (!first) {
                    text.append(", ");
                }
                first = false;
                text.append(info.text, info.change);
            }
            text.append("]");
        }
        return text;
    }

    // ---- width ----

    private double contentWidth(ClassModel_V001 model, String stereotype, MarkedText taggedValues,
                                List<MarkedText> attributes) {
        if (model.width == null) {
            return Math.max(naturalWidth(model, stereotype, taggedValues, attributes), MIN_BOX_WIDTH - 2 * PADDING_X);
        }
        return Math.max(model.width - 2 * PADDING_X, longestWord(model, stereotype, taggedValues, attributes));
    }

    private double naturalWidth(ClassModel_V001 model, String stereotype, MarkedText taggedValues,
                                List<MarkedText> attributes) {
        double widest = measurer.width(model.name, true);
        if (stereotype != null) {
            widest = Math.max(widest, measurer.width(stereotype, false));
        }
        if (taggedValues != null) {
            widest = Math.max(widest, measurer.width(taggedValues.text(), false));
        }
        for (MarkedText attribute : attributes) {
            widest = Math.max(widest, measurer.width(attribute.text(), false));
        }
        return widest;
    }

    private double longestWord(ClassModel_V001 model, String stereotype, MarkedText taggedValues,
                               List<MarkedText> attributes) {
        double longest = wrapper.longestWord(model.name, true);
        if (stereotype != null) {
            longest = Math.max(longest, wrapper.longestWord(stereotype, false));
        }
        if (taggedValues != null) {
            longest = Math.max(longest, wrapper.longestWord(taggedValues.text(), false));
        }
        for (MarkedText attribute : attributes) {
            longest = Math.max(longest, wrapper.longestWord(attribute.text(), false));
        }
        return longest;
    }

    // ---- parts ----

    /**
     * Adds the text as centered lines, wrapped if needed, and notes the lines in placed. Returns the y below the
     * last line.
     */
    private double addCenteredText(List<AbstractSceneElement> elements, List<SceneText> placed, String text,
                                   boolean bold, double contentWidth, double boxWidth, double y, String textColor) {
        if (text == null) {
            return y;
        }
        double lineY = y;
        for (String line : wrapper.wrap(text, contentWidth, contentWidth, bold)) {
            SceneText sceneText = new SceneText(boxWidth / 2, lineY + measurer.ascent(), line, bold, TextAnchor.MIDDLE,
                measurer.width(line, bold), textColor);
            elements.add(sceneText);
            placed.add(sceneText);
            lineY += measurer.lineHeight();
        }
        return lineY;
    }

    /**
     * An empty class still gets one blank attribute line. The box around the text of each attribute with an ID is
     * noted in attributeAreas. The attributes are marked unless the class as a whole is added or removed.
     * Returns the y below the last line.
     */
    private double addAttributes(List<AbstractSceneElement> elements, List<AbstractSceneElement> backgrounds,
                                 List<AbstractSceneElement> overlays, ClassModel_V001 model, List<MarkedText> attributes,
                                 double boxWidth, double y, Map<String, Rectangle2D.Double> attributeAreas,
                                 String textColor) {
        if (attributes.isEmpty()) {
            return y + measurer.lineHeight();
        }
        double contentWidth = boxWidth - 2 * PADDING_X;
        double lineY = y;
        for (int i = 0; i < attributes.size(); i++) {
            double top = lineY;
            double textRight = PADDING_X;
            boolean first = true;
            List<SceneText> lines = new ArrayList<>();
            for (String line : wrapper.wrap(attributes.get(i).text(), contentWidth, contentWidth - CONTINUATION_INDENT, false)) {
                double x = first ? PADDING_X : PADDING_X + CONTINUATION_INDENT;
                double lineWidth = measurer.width(line, false);
                SceneText sceneText = new SceneText(x, lineY + measurer.ascent(), line, false, TextAnchor.START,
                    lineWidth, textColor);
                elements.add(sceneText);
                lines.add(sceneText);
                textRight = Math.max(textRight, x + lineWidth);
                lineY += measurer.lineHeight();
                first = false;
            }
            AttributeModel_V001 attribute = model.attributes.get(i);
            if (model.change == null) {
                markAttribute(backgrounds, overlays, attribute, attributes.get(i), lines, boxWidth, top, lineY);
            }
            if (attribute.id != null) {
                attributeAreas.put(attribute.id, new Rectangle2D.Double(PADDING_X - ATTRIBUTE_ATTACH_GAP, top,
                    textRight - PADDING_X + 2 * ATTRIBUTE_ATTACH_GAP, lineY - top));
            }
        }
        return lineY;
    }

    /** An added or removed attribute gets a colored row, the other attributes the backgrounds of their changed texts. */
    private void markAttribute(List<AbstractSceneElement> backgrounds, List<AbstractSceneElement> overlays,
                               AttributeModel_V001 attribute, MarkedText text, List<SceneText> lines, double boxWidth,
                               double top, double bottom) {
        if (attribute.change == null) {
            backgrounds.addAll(highlighter.highlights(text, lines));
            return;
        }
        String changeSet = attribute.change.changeSet;
        backgrounds.add(new SceneRectangle(STROKE_WIDTH, top, boxWidth - 2 * STROKE_WIDTH, bottom - top,
            palette.pale(changeSet), null));
        backgrounds.addAll(highlighter.wholeHighlights(changeSet, lines));
        if (attribute.change.type == ChangeType_V001.REMOVED) {
            overlays.addAll(highlighter.strikes(lines));
        }
    }
}

package classman.render;

import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.TaggedValueModel_V001;

import java.util.ArrayList;
import java.util.List;

import static classman.render.DiagramStyle.CLASS_FILL;
import static classman.render.DiagramStyle.CONTINUATION_INDENT;
import static classman.render.DiagramStyle.MIN_BOX_WIDTH;
import static classman.render.DiagramStyle.PADDING_X;
import static classman.render.DiagramStyle.PADDING_Y;
import static classman.render.DiagramStyle.STROKE;

/** Lays out one class box: header with stereotype, name and tagged values, then the attributes. */
public class ClassBoxLayouter {
    private final TextMeasurer measurer;
    private final TextWrapper wrapper;

    public ClassBoxLayouter(TextMeasurer measurer) {
        this.measurer = measurer;
        this.wrapper = new TextWrapper(measurer);
    }

    public BoxLayout layout(ClassModel_V001 model) {
        return layout(model, CLASS_FILL);
    }

    /** Lays out the class with the given fill color for its box. */
    public BoxLayout layout(ClassModel_V001 model, String fill) {
        String stereotype = model.stereotype == null ? null : "<<" + model.stereotype + ">>";
        String taggedValues = taggedValuesText(model);
        List<String> attributes = attributeTexts(model);
        double contentWidth = contentWidth(model, stereotype, taggedValues, attributes);
        double width = contentWidth + 2 * PADDING_X;

        List<AbstractSceneElement> elements = new ArrayList<>();
        double y = PADDING_Y;
        y = addCenteredText(elements, stereotype, false, contentWidth, width, y);
        y = addCenteredText(elements, model.name, true, contentWidth, width, y);
        y = addCenteredText(elements, taggedValues, false, contentWidth, width, y);
        y += PADDING_Y;
        elements.add(new SceneLine(0, y, width, y, false));
        y += PADDING_Y;
        y = addAttributes(elements, attributes, contentWidth, y);
        y += PADDING_Y;
        elements.add(0, new SceneRectangle(0, 0, width, y, fill, STROKE));
        return new BoxLayout(width, y, elements);
    }

    private String taggedValuesText(ClassModel_V001 model) {
        if (model.taggedValues.isEmpty()) {
            return null;
        }
        List<String> pairs = new ArrayList<>();
        for (TaggedValueModel_V001 tag : model.taggedValues) {
            pairs.add(tag.key + "=" + tag.value);
        }
        return "{" + String.join(", ", pairs) + "}";
    }

    private List<String> attributeTexts(ClassModel_V001 model) {
        List<String> texts = new ArrayList<>();
        for (AttributeModel_V001 attribute : model.attributes) {
            texts.add(attributeText(attribute));
        }
        return texts;
    }

    private String attributeText(AttributeModel_V001 attribute) {
        if (attribute.hiddenAttributesMarker) {
            return "...";
        }
        StringBuilder text = new StringBuilder();
        if (attribute.visibility != null) {
            text.append(attribute.visibility.sign);
        }
        text.append(attribute.name);
        if (attribute.type != null) {
            text.append(" : ").append(attribute.type);
        }
        if (!attribute.infos.isEmpty()) {
            text.append(" [").append(String.join(", ", attribute.infos)).append(']');
        }
        return text.toString();
    }

    // ---- width ----

    private double contentWidth(ClassModel_V001 model, String stereotype, String taggedValues, List<String> attributes) {
        if (model.width == null) {
            return Math.max(naturalWidth(model, stereotype, taggedValues, attributes), MIN_BOX_WIDTH - 2 * PADDING_X);
        }
        return Math.max(model.width - 2 * PADDING_X, longestWord(model, stereotype, taggedValues, attributes));
    }

    private double naturalWidth(ClassModel_V001 model, String stereotype, String taggedValues, List<String> attributes) {
        double widest = measurer.width(model.name, true);
        if (stereotype != null) {
            widest = Math.max(widest, measurer.width(stereotype, false));
        }
        if (taggedValues != null) {
            widest = Math.max(widest, measurer.width(taggedValues, false));
        }
        for (String attribute : attributes) {
            widest = Math.max(widest, measurer.width(attribute, false));
        }
        return widest;
    }

    private double longestWord(ClassModel_V001 model, String stereotype, String taggedValues, List<String> attributes) {
        double longest = wrapper.longestWord(model.name, true);
        if (stereotype != null) {
            longest = Math.max(longest, wrapper.longestWord(stereotype, false));
        }
        if (taggedValues != null) {
            longest = Math.max(longest, wrapper.longestWord(taggedValues, false));
        }
        for (String attribute : attributes) {
            longest = Math.max(longest, wrapper.longestWord(attribute, false));
        }
        return longest;
    }

    // ---- parts ----

    /** Adds the text as centered lines, wrapped if needed. Returns the y below the last line. */
    private double addCenteredText(List<AbstractSceneElement> elements, String text, boolean bold, double contentWidth,
                                   double boxWidth, double y) {
        if (text == null) {
            return y;
        }
        double lineY = y;
        for (String line : wrapper.wrap(text, contentWidth, contentWidth, bold)) {
            elements.add(new SceneText(boxWidth / 2, lineY + measurer.ascent(), line, bold, TextAnchor.MIDDLE,
                measurer.width(line, bold)));
            lineY += measurer.lineHeight();
        }
        return lineY;
    }

    /** An empty class still gets one blank attribute line. Returns the y below the last line. */
    private double addAttributes(List<AbstractSceneElement> elements, List<String> attributes, double contentWidth, double y) {
        if (attributes.isEmpty()) {
            return y + measurer.lineHeight();
        }
        double lineY = y;
        for (String attribute : attributes) {
            boolean first = true;
            for (String line : wrapper.wrap(attribute, contentWidth, contentWidth - CONTINUATION_INDENT, false)) {
                double x = first ? PADDING_X : PADDING_X + CONTINUATION_INDENT;
                elements.add(new SceneText(x, lineY + measurer.ascent(), line, false, TextAnchor.START,
                    measurer.width(line, false)));
                lineY += measurer.lineHeight();
                first = false;
            }
        }
        return lineY;
    }
}

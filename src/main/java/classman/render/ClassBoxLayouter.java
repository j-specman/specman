package classman.render;

import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.TaggedValueModel_V001;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static classman.render.DiagramStyle.ATTRIBUTE_ATTACH_GAP;
import static classman.render.DiagramStyle.CLASS_FILL;
import static classman.render.DiagramStyle.FRAME_STROKE_WIDTH;
import static classman.render.DiagramStyle.MIN_BOX_WIDTH;
import static classman.render.DiagramStyle.PADDING_X;
import static classman.render.DiagramStyle.PADDING_Y;
import static classman.render.DiagramStyle.STROKE;
import static classman.render.DiagramStyle.STROKE_WIDTH;
import static classman.render.DiagramStyle.VEIL_SHARE;
import static classman.render.DiagramStyle.VEILED_STROKE;
import static classman.render.DiagramStyle.VEILED_MARKED_TEXT;
import static classman.render.DiagramStyle.VEILED_TEXT;
import static classman.render.DiagramStyle.WHITE;

/**
 * Lays out one class box: header with stereotype, name and tagged values, then the attributes. An interface shows
 * <<interface>> above its other stereotype and has no attributes. An added or removed
 * class gets a frame, a removed one is veiled and its name struck through. The texts and attributes of such a class
 * are not marked on their own. In any other class a changed text gets a colored background, an added or removed
 * attribute a colored row.
 */
public class ClassBoxLayouter {
    private final TextMeasurer measurer;
    private final TextWrapper wrapper;
    private final ChangePalette palette;
    private final TextHighlighter highlighter;
    private final AttributeTextBuilder attributeTextBuilder = new AttributeTextBuilder();

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
        String derivedStereotype = model.kind.derivedStereotype == null ? null
            : stereotypeText(model.kind.derivedStereotype);
        String stereotype = model.stereotype == null ? null : stereotypeText(model.stereotype);
        List<MarkedText> taggedValues = taggedValueTexts(model);
        List<MarkedText> attributes = attributeTexts(model);
        double contentWidth = contentWidth(model, derivedStereotype, stereotype, taggedValues, attributes);
        double width = contentWidth + 2 * PADDING_X;

        List<AbstractSceneElement> backgrounds = new ArrayList<>();
        List<AbstractSceneElement> elements = new ArrayList<>();
        List<AbstractSceneElement> overlays = new ArrayList<>();
        double y = PADDING_Y;
        y = addClippedCenteredText(elements, new ArrayList<>(), derivedStereotype, contentWidth, width, y, textColor);
        double stereotypeTop = y;
        List<SceneText> stereotypeLines = new ArrayList<>();
        y = addClippedCenteredText(elements, stereotypeLines, stereotype, contentWidth, width, y, textColor);
        double nameTop = y;
        List<SceneText> nameLines = new ArrayList<>();
        y = addCenteredText(elements, nameLines, model.name, true, contentWidth, width, y,
            isRemoved ? VEILED_MARKED_TEXT : null);
        double nameBottom = y;
        List<List<SceneText>> taggedLines = new ArrayList<>();
        Map<TaggedValueModel_V001, Rectangle2D.Double> taggedAreas = new LinkedHashMap<>();
        for (int i = 0; i < taggedValues.size(); i++) {
            double rowTop = y;
            List<SceneText> rowLines = new ArrayList<>();
            y = addClippedCenteredText(elements, rowLines, taggedValues.get(i).text(), contentWidth, width, y, textColor);
            taggedLines.add(rowLines);
            TaggedValueModel_V001 tag = model.taggedValues.get(i);
            if (tag.change == null || tag.change.type != ChangeType_V001.REMOVED) {
                taggedAreas.put(tag, new Rectangle2D.Double(PADDING_X, rowTop, contentWidth, y - rowTop));
            }
        }
        y += PADDING_Y;
        Map<String, Rectangle2D.Double> attributeAreas = new LinkedHashMap<>();
        Map<AttributeModel_V001, Rectangle2D.Double> lineAreas = new LinkedHashMap<>();
        if (model.kind.hasAttributes()) {
            elements.add(new SceneLine(0, y, width, y, false, lineColor, STROKE_WIDTH));
            y += PADDING_Y;
            y = addAttributes(elements, backgrounds, overlays, model, attributes, width, y, attributeAreas, lineAreas,
                textColor);
            y += PADDING_Y;
        }

        if (change == null) {
            addHeaderMarks(backgrounds, overlays, model, stereotypeLines, nameLines, taggedValues, taggedLines);
        } else if (isRemoved) {
            backgrounds.addAll(highlighter.wholeHighlights(change.changeSet, nameLines));
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
        Map<TextKind, Rectangle2D.Double> textAreas = new LinkedHashMap<>();
        if (stereotype != null) {
            textAreas.put(TextKind.STEREOTYPE,
                new Rectangle2D.Double(PADDING_X, stereotypeTop, contentWidth, nameTop - stereotypeTop));
        }
        textAreas.put(TextKind.CLASS_NAME, new Rectangle2D.Double(PADDING_X, nameTop, contentWidth, nameBottom - nameTop));
        return new BoxLayout(width, y, all, attributeAreas, change == null ? 0 : FRAME_STROKE_WIDTH / 2, textAreas,
            new AttributeLineAreas(lineAreas), new TaggedValueAreas(taggedAreas));
    }

    private SceneRectangle frame(double width, double height, ChangeModel_V001 change) {
        return new SceneRectangle(0, 0, width, height, null, palette.strong(change.changeSet), FRAME_STROKE_WIDTH)
            .keepingVisible();
    }

    private String veiled(String fill) {
        return ColorMix.blend(fill, WHITE, VEIL_SHARE);
    }

    private void addHeaderMarks(List<AbstractSceneElement> backgrounds, List<AbstractSceneElement> overlays,
                                ClassModel_V001 model, List<SceneText> stereotypeLines, List<SceneText> nameLines,
                                List<MarkedText> taggedValues, List<List<SceneText>> taggedLines) {
        markStereotype(backgrounds, overlays, model, stereotypeLines);
        backgrounds.addAll(highlighter.changeHighlights(model.nameChange, model.name, nameLines));
        for (int i = 0; i < taggedValues.size(); i++) {
            markTaggedValue(backgrounds, overlays, model.taggedValues.get(i), taggedValues.get(i), taggedLines.get(i));
        }
    }

    /** An added tagged value is marked, a removed one keeps its text and has a line through it, the text marks of any
     * other one cover the changed parts. */
    private void markTaggedValue(List<AbstractSceneElement> backgrounds, List<AbstractSceneElement> overlays,
                                 TaggedValueModel_V001 tag, MarkedText text, List<SceneText> lines) {
        if (tag.change == null) {
            backgrounds.addAll(highlighter.highlights(text, lines));
            return;
        }
        backgrounds.addAll(highlighter.wholeHighlights(tag.change.changeSet, lines));
        if (tag.change.type == ChangeType_V001.REMOVED) {
            overlays.addAll(highlighter.strikes(lines));
        }
    }

    /** An added stereotype is marked, a removed one keeps its text and has a line through it, the text mark of any
     * other one covers the whole text. */
    private void markStereotype(List<AbstractSceneElement> backgrounds, List<AbstractSceneElement> overlays,
                                ClassModel_V001 model, List<SceneText> stereotypeLines) {
        ChangeModel_V001 stereotypeChange = model.stereotypeElementChange;
        if (stereotypeChange == null) {
            backgrounds.addAll(highlighter.wholeHighlights(model.stereotypeChange, stereotypeLines));
            return;
        }
        backgrounds.addAll(highlighter.wholeHighlights(stereotypeChange.changeSet, stereotypeLines));
        if (stereotypeChange.type == ChangeType_V001.REMOVED) {
            overlays.addAll(highlighter.strikes(stereotypeLines));
        }
    }

    private static String stereotypeText(String stereotype) {
        return TextKind.STEREOTYPE.prefix + stereotype + TextKind.STEREOTYPE.suffix;
    }

    // ---- texts ----

    /** One text for each tagged value, {key=value}, or {key} if it has no value. The marks of a tagged value which is added or removed are ignored. */
    private List<MarkedText> taggedValueTexts(ClassModel_V001 model) {
        List<MarkedText> texts = new ArrayList<>();
        for (TaggedValueModel_V001 tag : model.taggedValues) {
            boolean hasTextMarks = tag.change == null;
            MarkedText text = new MarkedText().append("{").append(tag.key, hasTextMarks ? tag.keyChange : null);
            if (tag.value != null && !tag.value.isEmpty()) {
                text.append("=").append(tag.value, hasTextMarks ? tag.valueChange : null);
            }
            texts.add(text.append("}"));
        }
        return texts;
    }

    private List<MarkedText> attributeTexts(ClassModel_V001 model) {
        List<MarkedText> texts = new ArrayList<>();
        for (AttributeModel_V001 attribute : model.attributes) {
            texts.add(attributeText(attribute));
        }
        return texts;
    }

    private MarkedText attributeText(AttributeModel_V001 attribute) {
        return attributeTextBuilder.build(attribute);
    }

    // ---- width ----

    private double contentWidth(ClassModel_V001 model, String derivedStereotype, String stereotype,
                                List<MarkedText> taggedValues, List<MarkedText> attributes) {
        if (model.width == null) {
            return Math.max(naturalWidth(model, derivedStereotype, stereotype, taggedValues, attributes), MIN_BOX_WIDTH - 2 * PADDING_X);
        }
        return Math.max(model.width - 2 * PADDING_X, wrapper.longestWord(model.name, true));
    }

    private double naturalWidth(ClassModel_V001 model, String derivedStereotype, String stereotype,
                                List<MarkedText> taggedValues, List<MarkedText> attributes) {
        double widest = measurer.width(model.name, true);
        if (derivedStereotype != null) {
            widest = Math.max(widest, measurer.width(derivedStereotype, false));
        }
        if (stereotype != null) {
            widest = Math.max(widest, measurer.width(TextWrapper.oneLine(stereotype), false));
        }
        for (MarkedText tag : taggedValues) {
            widest = Math.max(widest, measurer.width(TextWrapper.oneLine(tag.text()), false));
        }
        for (MarkedText attribute : attributes) {
            widest = Math.max(widest, measurer.width(TextWrapper.oneLine(attribute.text()), false));
        }
        return widest;
    }

    // ---- parts ----

    /**
     * Adds the text as centered lines, wrapped at spaces if needed, and notes the lines in placed. Returns the y
     * below the last line.
     */
    private double addCenteredText(List<AbstractSceneElement> elements, List<SceneText> placed, String text,
                                   boolean bold, double contentWidth, double boxWidth, double y, String textColor) {
        if (text == null) {
            return y;
        }
        return addCenteredLines(elements, placed, wrapper.wrap(text, contentWidth, contentWidth, bold), bold, boxWidth,
            y, textColor);
    }

    /**
     * Adds the text as one centered line which is cut off where it doesn't fit, and notes it in placed. Returns the
     * y below the line.
     */
    private double addClippedCenteredText(List<AbstractSceneElement> elements, List<SceneText> placed, String text,
                                          double contentWidth, double boxWidth, double y, String textColor) {
        if (text == null) {
            return y;
        }
        return addCenteredLines(elements, placed, List.of(wrapper.clip(text, contentWidth, false)), false, boxWidth, y,
            textColor);
    }

    private double addCenteredLines(List<AbstractSceneElement> elements, List<SceneText> placed, List<String> lines,
                                    boolean bold, double boxWidth, double y, String textColor) {
        double lineY = y;
        for (String line : lines) {
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
     * noted in attributeAreas, the whole line of each attribute which can be changed in lineAreas, the blank line of
     * an empty class under the key null. The attributes are marked unless the class as a whole is added or removed.
     * Returns the y below the last line.
     */
    private double addAttributes(List<AbstractSceneElement> elements, List<AbstractSceneElement> backgrounds,
                                 List<AbstractSceneElement> overlays, ClassModel_V001 model, List<MarkedText> attributes,
                                 double boxWidth, double y, Map<String, Rectangle2D.Double> attributeAreas,
                                 Map<AttributeModel_V001, Rectangle2D.Double> lineAreas, String textColor) {
        double contentWidth = boxWidth - 2 * PADDING_X;
        if (attributes.isEmpty()) {
            lineAreas.put(null, new Rectangle2D.Double(PADDING_X, y, contentWidth, measurer.lineHeight()));
            return y + measurer.lineHeight();
        }
        double lineY = y;
        for (int i = 0; i < attributes.size(); i++) {
            double top = lineY;
            String line = wrapper.clip(attributes.get(i).text(), contentWidth, false);
            double lineWidth = measurer.width(line, false);
            SceneText sceneText = new SceneText(PADDING_X, lineY + measurer.ascent(), line, false, TextAnchor.START,
                lineWidth, textColor);
            elements.add(sceneText);
            List<SceneText> lines = List.of(sceneText);
            double textRight = PADDING_X + lineWidth;
            lineY += measurer.lineHeight();
            AttributeModel_V001 attribute = model.attributes.get(i);
            if (model.change == null) {
                markAttribute(backgrounds, overlays, attribute, attributes.get(i), lines, boxWidth, top, lineY);
            }
            if (attribute.id != null) {
                attributeAreas.put(attribute.id, new Rectangle2D.Double(PADDING_X - ATTRIBUTE_ATTACH_GAP, top,
                    textRight - PADDING_X + 2 * ATTRIBUTE_ATTACH_GAP, lineY - top));
            }
            if (!attribute.hiddenAttributesMarker) {
                lineAreas.put(attribute, new Rectangle2D.Double(PADDING_X, top, contentWidth, lineY - top));
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
            overlays.addAll(highlighter.strikes(text, lines));
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

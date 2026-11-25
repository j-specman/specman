package classman.render;

import classman.model.v001.AbstractMemberModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.Compartment_V001;
import classman.model.v001.ElementStyleModel_V001;
import classman.model.v001.StereotypeReference_V001;
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
import static classman.render.DiagramStyle.HIGHLIGHT_MARGIN;
import static classman.render.DiagramStyle.STEREOTYPE_ICON_GAP;
import static classman.render.DiagramStyle.STRIKE_COLOR;
import static classman.render.DiagramStyle.STRIKE_WIDTH;
import static classman.render.DiagramStyle.STROKE;
import static classman.render.DiagramStyle.STROKE_WIDTH;
import static classman.render.DiagramStyle.VEILED_STROKE;
import static classman.render.DiagramStyle.VEILED_MARKED_TEXT;
import static classman.render.DiagramStyle.VEILED_TEXT;

/**
 * Lays out one class box: header with stereotype, name and tagged values, then the attributes. An interface shows
 * <<interface>> in place of a stereotype, as long as it has no stereotype of its own, also not a removed one, so
 * there is never more than one stereotype to see. It has no attributes. An added or removed
 * class gets a frame, a removed one is veiled and its name struck through. The texts and attributes of such a class
 * are not marked on their own. In any other class a changed text gets a colored background, an added or removed
 * attribute a colored row.
 */
public class ClassBoxLayouter {
    private final TextMeasurer measurer;
    private final TextWrapper wrapper;
    private final ChangePalette palette;
    private final TextHighlighter highlighter;
    private final TextFont font;
    private final StereotypeResolver stereotypes;
    private final MemberTextBuilder memberTextBuilder = new MemberTextBuilder();

    /** @param measurer measures texts in the font
     * @param stereotypes tells what a stereotype looks like which refers to a configuration of the host */
    public ClassBoxLayouter(TextMeasurer measurer, ChangePalette palette, TextFont font, StereotypeResolver stereotypes) {
        this.measurer = measurer;
        this.wrapper = new TextWrapper(measurer);
        this.palette = palette;
        this.highlighter = new TextHighlighter(measurer, palette);
        this.font = font;
        this.stereotypes = stereotypes;
    }

    public BoxLayout layout(ClassModel_V001 model) {
        ChangeModel_V001 change = model.change;
        boolean isRemoved = change != null && change.type == ChangeType_V001.REMOVED;
        ElementStyleModel_V001 style = model.style;
        StyleColors colors = new StyleColors(style, CLASS_FILL, STROKE, VEILED_STROKE);
        String textColor = colors.text(isRemoved, VEILED_TEXT);
        String nameColor = colors.markedText(isRemoved, VEILED_MARKED_TEXT);
        String lineColor = colors.line(isRemoved);
        String stereotype = stereotypeText(model);
        boolean showsDerivedStereotype = model.kind.derivedStereotype != null && stereotype == null;
        String derivedStereotype = showsDerivedStereotype ? bracketed(model.kind.derivedStereotype) : null;
        List<MarkedText> taggedValues = taggedValueTexts(model);
        CompartmentLines attributes = CompartmentLines.of(model, Compartment_V001.ATTRIBUTES, memberTextBuilder);
        CompartmentLines methods = CompartmentLines.of(model, Compartment_V001.METHODS, memberTextBuilder);
        VectorImage icon = iconOf(model);
        double iconSize = measurer.lineHeight();
        double iconReserve = icon != null ? iconSize + STEREOTYPE_ICON_GAP : 0;
        double contentWidth = contentWidth(model, iconReserve, derivedStereotype, stereotype, taggedValues, attributes,
            methods);
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
        y = addCenteredText(elements, nameLines, model.name, true, contentWidth - iconReserve, width - iconReserve, y,
            nameColor);
        double nameBottom = y;
        SceneVectorImage iconElement = addIcon(elements, icon, nameLines, nameTop, iconSize, width - iconReserve);
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
        Map<String, Rectangle2D.Double> memberAreas = new LinkedHashMap<>();
        Map<AbstractMemberModel_V001, Rectangle2D.Double> attributeLineAreas = new LinkedHashMap<>();
        Map<AbstractMemberModel_V001, Rectangle2D.Double> methodLineAreas = new LinkedHashMap<>();
        for (CompartmentLines compartment : List.of(attributes, methods)) {
            if (compartment.isShown) {
                elements.add(new SceneLine(0, y, width, y, false, lineColor, STROKE_WIDTH));
                y += PADDING_Y;
                y = addMembers(elements, backgrounds, overlays, model, compartment, width, y, memberAreas,
                    compartment == attributes ? attributeLineAreas : methodLineAreas, textColor);
                y += PADDING_Y;
            }
        }

        if (change == null) {
            addHeaderMarks(backgrounds, overlays, model, stereotypeLines, iconElement, nameLines, taggedValues,
                taggedLines);
        } else if (isRemoved) {
            backgrounds.addAll(highlighter.wholeHighlights(change.changeSet, nameLines));
            overlays.addAll(highlighter.strikes(nameLines));
        }
        List<AbstractSceneElement> all = new ArrayList<>();
        if (change != null) {
            all.add(frame(width, y, change));
        }
        all.add(new SceneRectangle(0, 0, width, y, colors.fill(isRemoved), lineColor));
        all.addAll(backgrounds);
        all.addAll(elements);
        all.addAll(overlays);
        Map<TextKind, Rectangle2D.Double> textAreas = new LinkedHashMap<>();
        if (stereotype != null) {
            textAreas.put(TextKind.STEREOTYPE,
                new Rectangle2D.Double(PADDING_X, stereotypeTop, contentWidth, nameTop - stereotypeTop));
        }
        else if (iconElement != null) {
            textAreas.put(TextKind.STEREOTYPE,
                new Rectangle2D.Double(iconElement.x, iconElement.y, iconElement.width, iconElement.height));
        }
        textAreas.put(TextKind.CLASS_NAME, new Rectangle2D.Double(PADDING_X, nameTop, contentWidth, nameBottom - nameTop));
        return new BoxLayout(width, y, all, memberAreas, change == null ? 0 : FRAME_STROKE_WIDTH / 2, textAreas,
            new MemberLineAreas(TextKind.ATTRIBUTE_LINE, attributeLineAreas),
            new MemberLineAreas(TextKind.METHOD_LINE, methodLineAreas), new TaggedValueAreas(taggedAreas));
    }

    private SceneRectangle frame(double width, double height, ChangeModel_V001 change) {
        return frameOf(width, height, palette.strong(change.changeSet));
    }

    /** The frame of an added or removed class, in the strong color of its change set. */
    public static SceneRectangle frameOf(double width, double height, String strongColor) {
        return new SceneRectangle(0, 0, width, height, null, strongColor, FRAME_STROKE_WIDTH).keepingVisible();
    }

    private void addHeaderMarks(List<AbstractSceneElement> backgrounds, List<AbstractSceneElement> overlays,
                                ClassModel_V001 model, List<SceneText> stereotypeLines, SceneVectorImage icon,
                                List<SceneText> nameLines, List<MarkedText> taggedValues,
                                List<List<SceneText>> taggedLines) {
        markStereotype(backgrounds, overlays, model, stereotypeLines);
        markStereotypeIcon(backgrounds, overlays, model, icon);
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

    /** @return the text of the stereotype row with its angle brackets, null if there is nothing to show: no stereotype,
     * or one which refers to a configuration without a label */
    private String stereotypeText(ClassModel_V001 model) {
        if (model.stereotype == null) {
            return null;
        }
        if (!StereotypeReference_V001.isReference(model.stereotype)) {
            return bracketed(model.stereotype);
        }
        String name = StereotypeReference_V001.nameOf(model.stereotype);
        StereotypeLook look = stereotypes.lookOf(name);
        String label = look != null ? look.label : name;
        return label.isEmpty() ? null : bracketed(label);
    }

    /** @return the icon of the configuration which the stereotype refers to, null if there is none */
    private VectorImage iconOf(ClassModel_V001 model) {
        if (!StereotypeReference_V001.isReference(model.stereotype)) {
            return null;
        }
        StereotypeLook look = stereotypes.lookOf(StereotypeReference_V001.nameOf(model.stereotype));
        return look != null ? look.icon : null;
    }

    /** Puts the icon right of the first line of the name, in the height of the line. The name and the icon together
     * are centered in the box, see the width which the name is centered in.
     * @return the icon in the scene, null if there is none */
    private SceneVectorImage addIcon(List<AbstractSceneElement> elements, VectorImage icon, List<SceneText> nameLines,
                                     double nameTop, double iconSize, double nameCenteringWidth) {
        if (icon == null) {
            return null;
        }
        double x = nameLines.isEmpty() ? nameCenteringWidth / 2 + STEREOTYPE_ICON_GAP
            : nameLines.get(0).left() + nameLines.get(0).width + STEREOTYPE_ICON_GAP;
        SceneVectorImage placed = new SceneVectorImage(x, nameTop, iconSize, iconSize, icon);
        elements.add(placed);
        return placed;
    }

    /** The icon of the stereotype is marked like its text: a background if the stereotype is added or changed, and a
     * line through it if it is removed. */
    private void markStereotypeIcon(List<AbstractSceneElement> backgrounds, List<AbstractSceneElement> overlays,
                                    ClassModel_V001 model, SceneVectorImage icon) {
        if (icon == null) {
            return;
        }
        ChangeModel_V001 elementChange = model.stereotypeElementChange;
        String changeSet = elementChange != null ? elementChange.changeSet
            : model.stereotypeChange != null ? model.stereotypeChange.changeSet : null;
        if (changeSet == null) {
            return;
        }
        backgrounds.add(new SceneRectangle(icon.x - HIGHLIGHT_MARGIN, icon.y, icon.width + 2 * HIGHLIGHT_MARGIN,
            icon.height, palette.strong(changeSet), null, STROKE_WIDTH).keepingVisible());
        if (elementChange != null && elementChange.type == ChangeType_V001.REMOVED) {
            double y = icon.y + icon.height / 2;
            overlays.add(new SceneLine(icon.x - 1, y, icon.x + icon.width + 1, y, false, STRIKE_COLOR, STRIKE_WIDTH));
        }
    }

    private static String bracketed(String text) {
        return TextKind.STEREOTYPE.prefix + text + TextKind.STEREOTYPE.suffix;
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

    // ---- width ----

    /** @param iconReserve the width which the icon next to the name takes away from the name, 0 if there is no icon */
    private double contentWidth(ClassModel_V001 model, double iconReserve, String derivedStereotype, String stereotype,
                                List<MarkedText> taggedValues, CompartmentLines... compartments) {
        if (model.width == null) {
            return Math.max(naturalWidth(model, iconReserve, derivedStereotype, stereotype, taggedValues, compartments),
                MIN_BOX_WIDTH - 2 * PADDING_X);
        }
        return Math.max(model.width - 2 * PADDING_X, wrapper.longestWord(model.name, true) + iconReserve);
    }

    private double naturalWidth(ClassModel_V001 model, double iconReserve, String derivedStereotype, String stereotype,
                                List<MarkedText> taggedValues, CompartmentLines... compartments) {
        double widest = measurer.width(model.name, true) + iconReserve;
        if (derivedStereotype != null) {
            widest = Math.max(widest, measurer.width(derivedStereotype, false));
        }
        if (stereotype != null) {
            widest = Math.max(widest, measurer.width(TextWrapper.oneLine(stereotype), false));
        }
        for (MarkedText tag : taggedValues) {
            widest = Math.max(widest, measurer.width(TextWrapper.oneLine(tag.text()), false));
        }
        for (CompartmentLines compartment : compartments) {
            for (MarkedText line : compartment.texts) {
                widest = Math.max(widest, measurer.width(TextWrapper.oneLine(line.text()), false));
            }
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
                measurer.width(line, bold), textColor, font);
            elements.add(sceneText);
            placed.add(sceneText);
            lineY += measurer.lineHeight();
        }
        return lineY;
    }

    /**
     * An empty compartment still gets one blank line. The box around the text of each member with an ID is noted in
     * memberAreas (the box of the line "..." also for the hidden members with an ID), the whole line of each member
     * which can be changed in lineAreas, the blank line of an empty compartment under the key null. The members are
     * marked unless the class as a whole is added or removed. Returns the y below the last line.
     */
    private double addMembers(List<AbstractSceneElement> elements, List<AbstractSceneElement> backgrounds,
                              List<AbstractSceneElement> overlays, ClassModel_V001 model, CompartmentLines compartment,
                              double boxWidth, double y, Map<String, Rectangle2D.Double> memberAreas,
                              Map<AbstractMemberModel_V001, Rectangle2D.Double> lineAreas, String textColor) {
        double contentWidth = boxWidth - 2 * PADDING_X;
        if (compartment.members.isEmpty()) {
            lineAreas.put(null, new Rectangle2D.Double(PADDING_X, y, contentWidth, measurer.lineHeight()));
            return y + measurer.lineHeight();
        }
        double lineY = y;
        for (int i = 0; i < compartment.members.size(); i++) {
            double top = lineY;
            String line = wrapper.clip(compartment.texts.get(i).text(), contentWidth, false);
            double lineWidth = measurer.width(line, false);
            SceneText sceneText = new SceneText(PADDING_X, lineY + measurer.ascent(), line, false, TextAnchor.START,
                lineWidth, textColor, font);
            elements.add(sceneText);
            List<SceneText> lines = List.of(sceneText);
            double textRight = PADDING_X + lineWidth;
            lineY += measurer.lineHeight();
            AbstractMemberModel_V001 member = compartment.members.get(i);
            if (model.change == null) {
                markMember(backgrounds, overlays, member, compartment.texts.get(i), lines, boxWidth, top, lineY);
            }
            Rectangle2D.Double textBox = new Rectangle2D.Double(PADDING_X - ATTRIBUTE_ATTACH_GAP, top,
                textRight - PADDING_X + 2 * ATTRIBUTE_ATTACH_GAP, lineY - top);
            if (member.id != null) {
                memberAreas.put(member.id, textBox);
            }
            if (member.hiddenMarker) {
                hiddenMembersLeadToMarker(model, compartment, textBox, memberAreas);
            }
            else {
                lineAreas.put(member, new Rectangle2D.Double(PADDING_X, top, contentWidth, lineY - top));
            }
        }
        return lineY;
    }

    /** What is attached to a hidden member is attached to the box of the line "...". */
    private void hiddenMembersLeadToMarker(ClassModel_V001 model, CompartmentLines compartment,
                                           Rectangle2D.Double markerBox, Map<String, Rectangle2D.Double> memberAreas) {
        for (AbstractMemberModel_V001 member : model.members(compartment.compartment)) {
            if (member.hidden && member.id != null) {
                memberAreas.put(member.id, markerBox);
            }
        }
    }

    /** An added or removed member gets a colored row, the other members the backgrounds of their changed texts. */
    private void markMember(List<AbstractSceneElement> backgrounds, List<AbstractSceneElement> overlays,
                            AbstractMemberModel_V001 member, MarkedText text, List<SceneText> lines, double boxWidth,
                            double top, double bottom) {
        if (member.change == null) {
            backgrounds.addAll(highlighter.highlights(text, lines));
            overlays.addAll(highlighter.strikes(text, lines));
            return;
        }
        String changeSet = member.change.changeSet;
        backgrounds.add(new SceneRectangle(STROKE_WIDTH, top, boxWidth - 2 * STROKE_WIDTH, bottom - top,
            palette.pale(changeSet), null));
        backgrounds.addAll(highlighter.wholeHighlights(changeSet, lines));
        if (member.change.type == ChangeType_V001.REMOVED) {
            overlays.addAll(highlighter.strikes(lines));
        }
    }
}

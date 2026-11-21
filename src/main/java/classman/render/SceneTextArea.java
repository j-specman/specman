package classman.render;

import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.AttributeModel_V001;

import java.awt.geom.Rectangle2D;

/** Where a text of a diagram element is drawn: the area the user can double click to change it. */
public class SceneTextArea {
    public final AbstractDiagramElementModel_V001 element;
    public final TextKind kind;
    /** Exactly where the text is. */
    public final Rectangle2D.Double area;
    /** The attribute of the class if the text is an attribute line, null for the blank line of a class without
     * attributes and for all other texts. */
    public final AttributeModel_V001 attribute;

    public SceneTextArea(AbstractDiagramElementModel_V001 element, TextKind kind, Rectangle2D.Double area) {
        this(element, kind, area, null);
    }

    public SceneTextArea(AbstractDiagramElementModel_V001 element, TextKind kind, Rectangle2D.Double area,
                         AttributeModel_V001 attribute) {
        this.element = element;
        this.kind = kind;
        this.area = area;
        this.attribute = attribute;
    }

    /** The area where a double click counts as one on the text: the text, and for short texts a margin around it. */
    public Rectangle2D hitArea() {
        double padding = kind.hitPadding;
        return new Rectangle2D.Double(area.x - padding, area.y - padding,
            area.width + 2 * padding, area.height + 2 * padding);
    }
}

package classman.render;

import classman.model.v001.AbstractDiagramElementModel_V001;

import java.awt.geom.Rectangle2D;

/** Where a text of a diagram element is drawn: the area the user can double click to change it. */
public class SceneTextArea {
    public final AbstractDiagramElementModel_V001 element;
    public final TextKind kind;
    /** Exactly where the text is. */
    public final Rectangle2D.Double area;

    public SceneTextArea(AbstractDiagramElementModel_V001 element, TextKind kind, Rectangle2D.Double area) {
        this.element = element;
        this.kind = kind;
        this.area = area;
    }

    /** The area where a double click counts as one on the text: the text, and for short texts a margin around it. */
    public Rectangle2D hitArea() {
        double padding = kind.hitPadding;
        return new Rectangle2D.Double(area.x - padding, area.y - padding,
            area.width + 2 * padding, area.height + 2 * padding);
    }
}

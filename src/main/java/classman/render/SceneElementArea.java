package classman.render;

import classman.model.v001.AbstractDiagramElementModel_V001;

import java.awt.geom.Rectangle2D;

/** The area which a box of the diagram takes, and the model element it shows. */
public class SceneElementArea {
    public final AbstractDiagramElementModel_V001 element;
    public final Rectangle2D.Double area;
    /** Where the name of a class is, across the width of its box. Null for a box which has no name. */
    public final Rectangle2D.Double nameArea;

    public SceneElementArea(AbstractDiagramElementModel_V001 element, Rectangle2D.Double area) {
        this(element, area, null);
    }

    public SceneElementArea(AbstractDiagramElementModel_V001 element, Rectangle2D.Double area,
                            Rectangle2D.Double nameArea) {
        this.element = element;
        this.area = area;
        this.nameArea = nameArea;
    }
}

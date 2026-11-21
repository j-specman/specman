package classman.render;

import classman.model.v001.AbstractDiagramElementModel_V001;

import java.awt.geom.Rectangle2D;

/** The area which a box of the diagram takes, and the model element it shows. */
public class SceneElementArea {
    public final AbstractDiagramElementModel_V001 element;
    public final Rectangle2D.Double area;

    public SceneElementArea(AbstractDiagramElementModel_V001 element, Rectangle2D.Double area) {
        this.element = element;
        this.area = area;
    }
}

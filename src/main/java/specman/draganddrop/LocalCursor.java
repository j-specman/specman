package specman.draganddrop;

import javax.swing.*;
import java.awt.*;

/** A cursor position in the local coordinate system of a specific reference
 * component. {@link #isIn(Component)} works for child components of that
 * component (any depth) and for itself. */
public class LocalCursor {
    private final Point point;
    private final JComponent reference;

    public LocalCursor(Point point, JComponent reference) {
        this.point = point;
        this.reference = reference;
    }

    public boolean isIn(Component component) {
        if (component == null) {
            return false;
        }
        if (component == reference) {
            return reference.getVisibleRect().contains(point);
        }
        Rectangle boundsInReference = boundsRelativeToReference(component);
        return boundsInReference.contains(point);
    }

    /**
     * Computes the bounds of {@code component} in the coordinate system of
     * {@code reference} by walking up the parent chain and accumulating the
     * x/y offsets of each ancestor's {@code getBounds()}. Throws IllegalArgumentException
     * if {@code reference} is not an ancestor of {@code component}.
     */
    private Rectangle boundsRelativeToReference(Component component) {
        int offsetX = 0, offsetY = 0;
        Component c = component;
        while (c != reference) {
            checkForWrongParent(component, c);
            Rectangle bounds = c.getBounds();
            offsetX += bounds.x;
            offsetY += bounds.y;
            c = c.getParent();
        }
        return new Rectangle(offsetX, offsetY, component.getWidth(), component.getHeight());
    }

    private static void checkForWrongParent(Component component, Component c) {
        if (c == null) {
            throw new IllegalArgumentException(
                "Component " + component.getClass().getSimpleName() +
                " is not in the hierarchy of the reference component");
        }
    }
}

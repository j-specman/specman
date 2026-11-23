package classman.render;

import java.util.HashMap;
import java.util.Map;

/** The things notes can be attached to, found by their IDs. */
public class AttachTargetIndex {
    private final Map<String, AttachTarget> targets = new HashMap<>();

    /** Does nothing for an element without an ID. @throws IllegalArgumentException if the ID is already taken */
    public void add(String id, AttachTarget target) {
        if (id == null) {
            return;
        }
        if (targets.put(id, target) != null) {
            throw new IllegalArgumentException("The ID " + id + " is used by more than one element of the diagram");
        }
    }

    /** @throws IllegalArgumentException if no class, attribute or relation of the diagram has this ID */
    public AttachTarget find(String id) {
        AttachTarget target = targets.get(id);
        if (target == null) {
            throw new IllegalArgumentException("The diagram has no class, attribute or relation with the ID " + id
                + ", a note is attached to it");
        }
        return target;
    }

    /** @param attributesToo true if an attribute is a valid end as well, as for a dependency
     * @return the class, or if allowed the attribute, which a relation starts or ends at
     * @throws IllegalArgumentException if the diagram has no such class or attribute with this ID */
    public AttachTarget findEnd(String id, boolean attributesToo) {
        AttachTarget target = targets.get(id);
        boolean isClass = target instanceof ClassAttachTarget;
        boolean isAttribute = target instanceof AttributeAttachTarget;
        if (!isClass && !(attributesToo && isAttribute)) {
            throw new IllegalArgumentException("The diagram has no class" + (attributesToo ? " or attribute" : "")
                + " with the ID " + id);
        }
        return target;
    }
}

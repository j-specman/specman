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

    /** @throws IllegalArgumentException if no class, attribute, method or relation of the diagram has this ID */
    public AttachTarget find(String id) {
        AttachTarget target = targets.get(id);
        if (target == null) {
            throw new IllegalArgumentException("The diagram has no class, attribute, method or relation with the ID " + id
                + ", a note is attached to it");
        }
        return target;
    }

    /** @param membersToo true if an attribute or method is a valid end as well, as for a dependency
     * @return the class, or if allowed the attribute or method, which a relation starts or ends at
     * @throws IllegalArgumentException if the diagram has no such class, attribute or method with this ID */
    public AttachTarget findEnd(String id, boolean membersToo) {
        AttachTarget target = targets.get(id);
        boolean isClass = target instanceof ClassAttachTarget;
        boolean isMember = target instanceof MemberAttachTarget;
        if (!isClass && !(membersToo && isMember)) {
            throw new IllegalArgumentException("The diagram has no class" + (membersToo ? ", attribute or method" : "")
                + " with the ID " + id);
        }
        return target;
    }
}

package classman.model.v001.change;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.NoteModel_V001;

import java.util.HashSet;
import java.util.Set;

/**
 * Knows which elements go away when the marks of a change set are accepted or rejected: the marked ones, and
 * everything which hangs on them. A class takes its relations along, a class, an attribute or a relation takes the
 * notes along which are attached to it.
 */
class VanishingElements_V001 {
    private final String changeSet;
    private final ChangeResolution_V001 resolution;
    private final Set<String> classIds = new HashSet<>();
    private final Set<String> attributeIds = new HashSet<>();
    private final Set<String> relationIds = new HashSet<>();

    VanishingElements_V001(ClassDiagramModel_V001 model, String changeSet, ChangeResolution_V001 resolution) {
        this.changeSet = changeSet;
        this.resolution = resolution;
        collectClassAndAttributeIds(model);
        collectRelationIds(model);
    }

    /** True if the mark of a class, attribute, association class, relation or note makes it go away. */
    boolean vanishes(ChangeModel_V001 change) {
        return change != null && change.changeSet.equals(changeSet) && resolution.removesElementWith(change.type);
    }

    boolean relationVanishes(AbstractRelationModel_V001 relation) {
        return vanishes(relation.change) || classIds.contains(relation.fromId) || classIds.contains(relation.toId);
    }

    boolean noteVanishes(NoteModel_V001 note) {
        String target = note.attachedToId;
        return vanishes(note.change)
            || classIds.contains(target) || attributeIds.contains(target) || relationIds.contains(target);
    }

    private void collectClassAndAttributeIds(ClassDiagramModel_V001 model) {
        for (ClassModel_V001 classModel : model.classes()) {
            boolean classVanishes = vanishes(classModel.change);
            if (classVanishes && classModel.id != null) {
                classIds.add(classModel.id);
            }
            collectAttributeIds(classModel, classVanishes);
        }
    }

    private void collectAttributeIds(ClassModel_V001 classModel, boolean classVanishes) {
        for (AttributeModel_V001 attribute : classModel.attributes) {
            if (attribute.id != null && (classVanishes || vanishes(attribute.change))) {
                attributeIds.add(attribute.id);
            }
        }
    }

    private void collectRelationIds(ClassDiagramModel_V001 model) {
        for (AbstractRelationModel_V001 relation : model.relations()) {
            if (relation.id != null && relationVanishes(relation)) {
                relationIds.add(relation.id);
            }
        }
    }
}

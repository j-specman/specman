package classman.model.v001.change;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AnchorLineModel_V001;
import classman.model.v001.AttachLineModel_V001;
import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.NoteModel_V001;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Knows which elements go away when the marks of a change set are accepted or rejected: the marked ones, and
 * everything which hangs on them. A class takes its relations along, a class, an attribute or a relation takes the
 * notes along which are attached to it, a note takes its anchor lines along, and so does the class, attribute or
 * relation an anchor line leads to. An association class link goes with its class and with its relation, the class
 * itself stays where only the relation goes. An anchor line which a change set removed doesn't attach its note any more, unless
 * it was removed together with the note, by the same change set: then it still counts, so that the note goes with the
 * target which the deletion of the note's lines was a consequence of.
 */
class VanishingElements_V001 {
    private final String changeSet;
    private final ChangeResolution_V001 resolution;
    private final Set<String> classIds = new HashSet<>();
    private final Set<String> attributeIds = new HashSet<>();
    private final Set<String> relationIds = new HashSet<>();
    private final Set<NoteModel_V001> notes = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<String> noteIds = new HashSet<>();

    VanishingElements_V001(ClassDiagramModel_V001 model, String changeSet, ChangeResolution_V001 resolution) {
        this.changeSet = changeSet;
        this.resolution = resolution;
        collectClassAndAttributeIds(model);
        collectRelationIds(model);
        collectNotes(model);
    }

    /** True if the mark of a class, attribute, relation, note, anchor line or association class link makes it go away. */
    boolean vanishes(ChangeModel_V001 change) {
        return change != null && change.changeSet.equals(changeSet) && resolution.removesElementWith(change.type);
    }

    boolean relationVanishes(AbstractRelationModel_V001 relation) {
        return vanishes(relation.change) || classIds.contains(relation.fromId) || classIds.contains(relation.toId);
    }

    boolean noteVanishes(NoteModel_V001 note) {
        return notes.contains(note);
    }

    boolean attachLineVanishes(AttachLineModel_V001 line) {
        return vanishes(line.change) || noteIds.contains(line.startId) || classIds.contains(line.startId)
            || leadsToVanishing(line);
    }

    private boolean leadsToVanishing(AttachLineModel_V001 line) {
        String target = line.targetId;
        return classIds.contains(target) || attributeIds.contains(target) || relationIds.contains(target);
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

    /** A note goes if its own mark says so, or if a line which attaches it leads to something which goes. */
    private void collectNotes(ClassDiagramModel_V001 model) {
        for (NoteModel_V001 note : model.notes()) {
            boolean attachedToVanishing = model.anchorLines().stream()
                .anyMatch(line -> attaches(line, note) && leadsToVanishing(line));
            if (vanishes(note.change) || attachedToVanishing) {
                notes.add(note);
                if (note.id != null) {
                    noteIds.add(note.id);
                }
            }
        }
    }

    /** True if the line starts at the note and no change set removed it, or the change set which removed it removed
     * the note too. */
    private boolean attaches(AnchorLineModel_V001 anchorLine, NoteModel_V001 note) {
        if (note.id == null || !note.id.equals(anchorLine.startId)) {
            return false;
        }
        return !anchorLine.isRemoved() || removedTogether(anchorLine, note);
    }

    private boolean removedTogether(AnchorLineModel_V001 anchorLine, NoteModel_V001 note) {
        return note.change != null && note.change.type == ChangeType_V001.REMOVED
            && note.change.changeSet.equals(anchorLine.change.changeSet);
    }
}

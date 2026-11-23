package classman.render;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AnchorLineModel_V001;
import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.RelationType_V001;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Knows what is not drawn because the attribute compartment of a class is hidden: the attributes in it, the
 * dependencies which start or end at them, the anchor lines which lead to them or to such a dependency, and the notes
 * which hang on nothing else which is shown. A note which has no anchor line is always shown. An attribute which is
 * hidden on its own is not part of this, it is stood for by the line ". . .".
 */
class HiddenElements {
    private final ClassDiagramModel_V001 model;
    private final Set<String> hiddenTargetIds = new HashSet<>();
    private final Set<AbstractRelationModel_V001> hiddenDependencies = Collections.newSetFromMap(new IdentityHashMap<>());

    HiddenElements(ClassDiagramModel_V001 model) {
        this.model = model;
        for (ClassModel_V001 classModel : model.classes()) {
            if (!classModel.showsAttributes()) {
                for (AttributeModel_V001 attribute : classModel.attributes) {
                    addIfGiven(attribute.id);
                }
            }
        }
        Set<String> hiddenAttributeIds = new HashSet<>(hiddenTargetIds);
        for (AbstractRelationModel_V001 relation : model.relations()) {
            boolean isHiddenDependency = relation.type == RelationType_V001.DEPENDENCY
                && (hiddenAttributeIds.contains(relation.fromId) || hiddenAttributeIds.contains(relation.toId));
            if (isHiddenDependency) {
                hiddenDependencies.add(relation);
                addIfGiven(relation.id);
            }
        }
    }

    /** Attributes and relations may have no ID, and none of them is hidden by a null in the set. */
    private void addIfGiven(String id) {
        if (id != null) {
            hiddenTargetIds.add(id);
        }
    }

    /** True for a dependency which starts or ends at an attribute of a hidden compartment. */
    boolean isRelationHidden(AbstractRelationModel_V001 relation) {
        return hiddenDependencies.contains(relation);
    }

    boolean isTargetHidden(String targetId) {
        return targetId != null && hiddenTargetIds.contains(targetId);
    }

    /** True if the note has anchor lines and all of them lead to hidden elements. */
    boolean isNoteHidden(NoteModel_V001 note) {
        List<AnchorLineModel_V001> lines = model.anchorLines().stream()
            .filter(line -> note.id != null && note.id.equals(line.startId)).toList();
        return !lines.isEmpty() && lines.stream().allMatch(line -> isTargetHidden(line.targetId));
    }
}

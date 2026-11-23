package classman.render;

import classman.model.v001.AnchorLineModel_V001;
import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.NoteModel_V001;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Knows what is not drawn because the attribute compartment of a class is hidden: the attributes in it, the anchor
 * lines which lead to them, and the notes which hang on nothing else which is shown. A note which has no anchor line
 * is always shown. An attribute which is hidden on its own is not part of this, it is stood for by the line ". . .".
 */
class HiddenElements {
    private final ClassDiagramModel_V001 model;
    private final Set<String> hiddenAttributeIds = new HashSet<>();

    HiddenElements(ClassDiagramModel_V001 model) {
        this.model = model;
        for (ClassModel_V001 classModel : model.classes()) {
            if (!classModel.showsAttributes()) {
                for (AttributeModel_V001 attribute : classModel.attributes) {
                    hiddenAttributeIds.add(attribute.id);
                }
            }
        }
    }

    boolean isTargetHidden(String targetId) {
        return hiddenAttributeIds.contains(targetId);
    }

    /** True if the note has anchor lines and all of them lead to hidden attributes. */
    boolean isNoteHidden(NoteModel_V001 note) {
        List<AnchorLineModel_V001> lines = model.anchorLines().stream()
            .filter(line -> note.id != null && note.id.equals(line.startId)).toList();
        return !lines.isEmpty() && lines.stream().allMatch(line -> isTargetHidden(line.targetId));
    }
}

package classman.model.v001.change;

import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.AbstractMemberModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.InfoModel_V001;
import classman.model.v001.TaggedValueModel_V001;

import java.util.List;
import java.util.function.UnaryOperator;

/** Knows every element mark (added or removed) of a diagram, so that nobody else has to. */
final class ElementMarks_V001 {

    private ElementMarks_V001() {
    }

    /** Replaces the mark of every class, attribute, relation, note and dashed line by what the operator makes of it. */
    static void replaceAll(ClassDiagramModel_V001 model, UnaryOperator<ChangeModel_V001> replacement) {
        for (AbstractDiagramElementModel_V001 element : model.elements) {
            element.change = replacement.apply(element.change);
        }
        for (ClassModel_V001 classModel : model.classes()) {
            classModel.change = replacement.apply(classModel.change);
            classModel.stereotypeElementChange = replacement.apply(classModel.stereotypeElementChange);
            for (TaggedValueModel_V001 tag : classModel.taggedValues) {
                tag.change = replacement.apply(tag.change);
            }
            for (AbstractMemberModel_V001 member : classModel.allMembers()) {
                member.change = replacement.apply(member.change);
                for (List<InfoModel_V001> entries : member.entryLists()) {
                    for (InfoModel_V001 entry : entries) {
                        entry.change = replacement.apply(entry.change);
                    }
                }
            }
        }
    }
}

package classman.model.v001.change;

import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;

import java.util.function.UnaryOperator;

/** Knows every element mark (added or removed) of a diagram, so that nobody else has to. */
final class ElementMarks_V001 {

    private ElementMarks_V001() {
    }

    /** Replaces the mark of every class, association class, attribute, relation and note by what the operator makes of it. */
    static void replaceAll(ClassDiagramModel_V001 model, UnaryOperator<ChangeModel_V001> replacement) {
        for (AbstractDiagramElementModel_V001 element : model.elements) {
            element.change = replacement.apply(element.change);
        }
        for (ClassModel_V001 classModel : ClassBoxes_V001.of(model)) {
            classModel.change = replacement.apply(classModel.change);
            for (AttributeModel_V001 attribute : classModel.attributes) {
                attribute.change = replacement.apply(attribute.change);
            }
        }
    }
}

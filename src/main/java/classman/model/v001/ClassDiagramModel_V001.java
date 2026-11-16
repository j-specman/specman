package classman.model.v001;

import java.util.ArrayList;
import java.util.List;

/** Model of a class diagram as written in a classdiagram source. The elements keep their source order. */
public class ClassDiagramModel_V001 {
    /** Null for a diagram without a name. */
    public String name;
    public final List<AbstractDiagramElementModel_V001> elements = new ArrayList<>();

    public ClassDiagramModel_V001(String name) {
        this.name = name;
    }

    public List<ClassModel_V001> classes() {
        return elementsOfType(ClassModel_V001.class);
    }

    public List<AbstractRelationModel_V001> relations() {
        return elementsOfType(AbstractRelationModel_V001.class);
    }

    public List<NoteModel_V001> notes() {
        return elementsOfType(NoteModel_V001.class);
    }

    private <T extends AbstractDiagramElementModel_V001> List<T> elementsOfType(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (AbstractDiagramElementModel_V001 element : elements) {
            if (type.isInstance(element)) {
                result.add(type.cast(element));
            }
        }
        return result;
    }
}

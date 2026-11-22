package classman.model.v001;

import java.util.ArrayList;
import java.util.List;

/** Model of a class diagram as written in a classdiagram source. The elements keep their source order. */
public class ClassDiagramModel_V001 {
    /** Null for a diagram without a name. */
    public String name;
    /** Null if no change set changed the name. */
    public TextChangeModel_V001 nameChange;
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

    /** The classes and notes in the order they are drawn, the last one on top. */
    public List<AbstractBoxModel_V001> boxes() {
        return elementsOfType(AbstractBoxModel_V001.class);
    }

    public List<NoteModel_V001> notes() {
        return elementsOfType(NoteModel_V001.class);
    }

    public List<AnchorLineModel_V001> anchorLines() {
        return elementsOfType(AnchorLineModel_V001.class);
    }

    /** The anchor lines which start at the note and which no change set removed. A note without an ID has none. */
    public List<AnchorLineModel_V001> liveAnchorLinesOf(NoteModel_V001 note) {
        List<AnchorLineModel_V001> lines = new ArrayList<>();
        for (AnchorLineModel_V001 line : anchorLines()) {
            if (note.id != null && note.id.equals(line.noteId) && !line.isRemoved()) {
                lines.add(line);
            }
        }
        return lines;
    }

    /** @return the note with the ID, null if the diagram has none */
    public NoteModel_V001 noteWithId(String id) {
        for (NoteModel_V001 note : notes()) {
            if (note.id != null && note.id.equals(id)) {
                return note;
            }
        }
        return null;
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

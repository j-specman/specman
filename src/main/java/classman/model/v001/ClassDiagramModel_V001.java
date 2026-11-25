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

    public List<AssocClassLinkModel_V001> assocClassLinks() {
        return elementsOfType(AssocClassLinkModel_V001.class);
    }

    /** The dashed lines which attach a note or a class to something: anchor lines and association class links. */
    public List<AttachLineModel_V001> attachLines() {
        return elementsOfType(AttachLineModel_V001.class);
    }

    /** The anchor lines which start at the note and which no change set removed. A note without an ID has none. */
    public List<AnchorLineModel_V001> liveAnchorLinesOf(NoteModel_V001 note) {
        List<AnchorLineModel_V001> lines = new ArrayList<>();
        for (AnchorLineModel_V001 line : anchorLines()) {
            if (note.id != null && note.id.equals(line.startId) && !line.isRemoved()) {
                lines.add(line);
            }
        }
        return lines;
    }

    /** @return the association class link which no change set removed and which starts at the class, null if there is
     * none. A class without an ID has none. */
    public AssocClassLinkModel_V001 liveAssocClassLinkOf(ClassModel_V001 classModel) {
        for (AssocClassLinkModel_V001 link : assocClassLinks()) {
            if (classModel.id != null && classModel.id.equals(link.startId) && !link.isRemoved()) {
                return link;
            }
        }
        return null;
    }

    /** @return the association class link which no change set removed and which leads to the relation, null if there is
     * none. A relation without an ID has none. */
    public AssocClassLinkModel_V001 liveAssocClassLinkTo(AbstractRelationModel_V001 relation) {
        for (AssocClassLinkModel_V001 link : assocClassLinks()) {
            if (relation.id != null && relation.id.equals(link.targetId) && !link.isRemoved()) {
                return link;
            }
        }
        return null;
    }

    /** @return the class or note with the ID, null if the diagram has none */
    public AbstractBoxModel_V001 boxWithId(String id) {
        for (AbstractBoxModel_V001 box : boxes()) {
            if (box.id != null && box.id.equals(id)) {
                return box;
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

package classman.model.v001;

/**
 * Model of the dashed line from a note to the class, attribute or relation it is about, the anchor of the note. A note
 * can have several.
 */
public class AnchorLineModel_V001 extends AttachLineModel_V001 {
    /** @param noteId the ID of the note the line starts at
     * @param targetId the ID of the class, attribute or relation the line leads to */
    public AnchorLineModel_V001(String noteId, String targetId) {
        super(noteId, targetId);
    }
}

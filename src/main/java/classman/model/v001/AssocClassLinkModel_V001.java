package classman.model.v001;

/**
 * Model of the dashed line from a class to the relation it is the association class of. It looks like an anchor line
 * but means something else: the class stays an ordinary class, the line makes it the association class of the
 * relation. A relation has at most one, and a class belongs to at most one relation.
 */
public class AssocClassLinkModel_V001 extends AttachLineModel_V001 {
    /** @param classId the ID of the class which is the association class
     * @param relationId the ID of the association, aggregation or composition */
    public AssocClassLinkModel_V001(String classId, String relationId) {
        super(classId, relationId);
    }
}

package classman.model.v001;

/**
 * Model of a dependency: the source depends on the target. Both ends are classes, interfaces, enums or attributes
 * (enum values included), referred to by their IDs. No multiplicities, the target has the arrowhead.
 */
public class DependencyRelationModel_V001 extends AbstractRelationModel_V001 {

    public DependencyRelationModel_V001(String fromId, String toId) {
        super(RelationType_V001.DEPENDENCY, fromId, toId);
    }
}

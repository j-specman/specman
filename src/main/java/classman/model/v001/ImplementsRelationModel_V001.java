package classman.model.v001;

/** Model of an implements relation: the source class implements the target interface. No multiplicities. */
public class ImplementsRelationModel_V001 extends AbstractRelationModel_V001 {

    public ImplementsRelationModel_V001(String from, String to) {
        super(RelationType_V001.IMPLEMENTS, from, to);
    }
}

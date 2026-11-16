package classman.model.v001;

/** Model of an extends relation: the source class is a subclass of the target class. No multiplicities. */
public class ExtendsRelationModel_V001 extends AbstractRelationModel_V001 {

    public ExtendsRelationModel_V001(String from, String to) {
        super(RelationType_V001.EXTENDS, from, to);
    }
}

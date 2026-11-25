package classman.model.v001.change;

import classman.model.v001.AttributeModel_V001;

/** Gives an attribute the line which the user has typed, or makes a new attribute of it, see {@link AbstractMemberChanger_V001}. */
public class AttributeChanger_V001 extends AbstractMemberChanger_V001<AttributeModel_V001, AttributeLine_V001> {

    @Override
    protected AttributeModel_V001 newMember(AttributeLine_V001 line) {
        return new AttributeModel_V001(line.visibility, line.name, line.type, entriesOf(line.infos));
    }
}

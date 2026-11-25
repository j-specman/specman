package classman.model.v001.change;

import classman.model.v001.MethodModel_V001;

/**
 * Gives a method the line which the user has typed, or makes a new method of it, see {@link AbstractMemberChanger_V001}.
 * The parameters are list entries like the infos: the same ones stay, the others are changed, added or removed, each
 * with its mark while changes are tracked.
 */
public class MethodChanger_V001 extends AbstractMemberChanger_V001<MethodModel_V001, MethodLine_V001> {

    @Override
    protected void applyOwnParts(MethodModel_V001 method, MethodLine_V001 line, String changeSet, boolean markable) {
        entriesChanger.apply(method.parameters, line.parameters, changeSet, markable);
    }

    @Override
    protected MethodModel_V001 newMember(MethodLine_V001 line) {
        return new MethodModel_V001(line.visibility, line.name, entriesOf(line.parameters), line.type,
            entriesOf(line.infos));
    }
}

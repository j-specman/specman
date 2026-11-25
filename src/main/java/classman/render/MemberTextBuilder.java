package classman.render;

import classman.model.v001.AbstractMemberModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.InfoModel_V001;
import classman.model.v001.MethodModel_V001;

import java.util.List;

/**
 * Puts together the text of an attribute or method line as the diagram shows it: +name : type [info, info], or
 * +name(parameter, parameter) : type [info, info]. The marks of changes go with the parts. An info or parameter which a
 * change set has added is marked, one which it has removed keeps its text and has a line through it. A type, or an
 * info of a source which writes it so, which a change set has removed is the empty text with a mark and is not shown,
 * so the characters next to the place where it was are marked, with the rule which marks the place of a gap in a
 * changed text: the nearest ones on both sides, at the start or end only the one side.
 */
class MemberTextBuilder {

    /** The dots have blanks between them, so that the line takes more room. */
    static final String TEXT_OF_MARKER = ". . .";

    MarkedText build(AbstractMemberModel_V001 member) {
        MarkedText text = new MarkedText();
        if (member.hiddenMarker) {
            return text.append(TEXT_OF_MARKER, member.nameChange);
        }
        if (member.visibility != null) {
            text.append(String.valueOf(member.visibility.sign), member.visibilityChange);
        }
        text.append(member.name, member.nameChange);
        if (member instanceof MethodModel_V001 method && !isEmpty(member.name)) {
            appendParameters(text, method.parameters);
        }
        appendType(text, member);
        appendInfos(text, member);
        return text;
    }

    /** The parameters in parentheses, which are shown also if there are none. */
    private void appendParameters(MarkedText text, List<InfoModel_V001> parameters) {
        text.append("(");
        boolean first = true;
        for (InfoModel_V001 parameter : parameters) {
            if (isEmpty(parameter.text)) {
                continue;
            }
            text.append(first ? "" : ", ");
            appendEntry(text, parameter);
            first = false;
        }
        text.append(")");
    }

    private void appendType(MarkedText text, AbstractMemberModel_V001 member) {
        if (isEmpty(member.type)) {
            if (member.type != null && member.typeChange != null) {
                markLastCharacter(text, member.typeChange.changeSet);
            }
            return;
        }
        text.append(" : ").append(member.type, member.typeChange);
    }

    private void appendInfos(MarkedText text, AbstractMemberModel_V001 member) {
        int previousEnd = -1;
        String gapChangeSet = null;
        for (InfoModel_V001 info : member.infos) {
            if (isEmptyWithMark(info)) {
                gapChangeSet = info.textChange.changeSet;
                continue;
            }
            if (isEmpty(info.text)) {
                continue;
            }
            text.append(previousEnd < 0 ? " [" : ", ");
            int start = text.length();
            appendEntry(text, info);
            if (gapChangeSet != null) {
                if (previousEnd >= 0) {
                    text.mark(previousEnd - 1, previousEnd, gapChangeSet);
                }
                text.mark(start, start + 1, gapChangeSet);
                gapChangeSet = null;
            }
            previousEnd = text.length();
        }
        if (previousEnd >= 0) {
            text.append("]");
        }
        if (gapChangeSet != null) {
            if (previousEnd >= 0) {
                text.mark(previousEnd - 1, previousEnd, gapChangeSet);
            }
            else {
                markLastCharacter(text, gapChangeSet);
            }
        }
    }

    /** An entry which a change set has added is marked, one which it has removed keeps its text and has a line through
     * it, the text mark of any other one goes with the text. */
    private void appendEntry(MarkedText text, InfoModel_V001 entry) {
        if (entry.change == null) {
            text.append(entry.text, entry.textChange);
        }
        else if (entry.change.type == ChangeType_V001.REMOVED) {
            text.appendRemoved(entry.text, entry.change.changeSet);
        }
        else {
            text.appendAdded(entry.text, entry.change.changeSet);
        }
    }

    private void markLastCharacter(MarkedText text, String changeSet) {
        text.mark(text.length() - 1, text.length(), changeSet);
    }

    /** An info from a source which marks a removed info as the empty text with a mark of its old text: it is not
     * shown, the place where it was is marked. */
    private boolean isEmptyWithMark(InfoModel_V001 info) {
        return isEmpty(info.text) && info.textChange != null;
    }

    private boolean isEmpty(String text) {
        return text == null || text.isEmpty();
    }
}

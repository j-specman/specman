package classman.render;

import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.InfoModel_V001;

/**
 * Puts together the text of an attribute line as the diagram shows it: +name : type [info, info]. The marks of changes
 * go with the parts. An info which a change set has added is marked, one which it has removed keeps its text and has
 * a line through it. A type, or an info of a source which writes it so, which a change set has removed is the empty
 * text with a mark and is not shown, so the characters next to the place where it was are marked, with the rule which
 * marks the place of a gap in a changed text: the nearest ones on both sides, at the start or end only the one side.
 */
class AttributeTextBuilder {

    /** The dots have blanks between them, so that the line takes more room. */
    static final String TEXT_OF_MARKER = ". . .";

    MarkedText build(AttributeModel_V001 attribute) {
        MarkedText text = new MarkedText();
        if (attribute.hiddenAttributesMarker) {
            return text.append(TEXT_OF_MARKER, attribute.nameChange);
        }
        if (attribute.visibility != null) {
            text.append(String.valueOf(attribute.visibility.sign), attribute.visibilityChange);
        }
        text.append(attribute.name, attribute.nameChange);
        appendType(text, attribute);
        appendInfos(text, attribute);
        return text;
    }

    private void appendType(MarkedText text, AttributeModel_V001 attribute) {
        if (isEmpty(attribute.type)) {
            if (attribute.type != null && attribute.typeChange != null) {
                markLastCharacter(text, attribute.typeChange.changeSet);
            }
            return;
        }
        text.append(" : ").append(attribute.type, attribute.typeChange);
    }

    private void appendInfos(MarkedText text, AttributeModel_V001 attribute) {
        int previousEnd = -1;
        String gapChangeSet = null;
        for (InfoModel_V001 info : attribute.infos) {
            if (isEmptyWithMark(info)) {
                gapChangeSet = info.textChange.changeSet;
                continue;
            }
            if (isEmpty(info.text)) {
                continue;
            }
            text.append(previousEnd < 0 ? " [" : ", ");
            int start = text.length();
            appendInfo(text, info);
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

    /** An info which a change set has added is marked, one which it has removed keeps its text and has a line through
     * it, the text mark of any other one goes with the text. */
    private void appendInfo(MarkedText text, InfoModel_V001 info) {
        if (info.change == null) {
            text.append(info.text, info.textChange);
        }
        else if (info.change.type == ChangeType_V001.REMOVED) {
            text.appendRemoved(info.text, info.change.changeSet);
        }
        else {
            text.appendAdded(info.text, info.change.changeSet);
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

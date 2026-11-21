package classman.model.v001.change;

import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.InfoModel_V001;

import java.util.ArrayList;
import java.util.List;

/**
 * Gives an attribute new infos. The infos which stay the same stay, in the gaps between them the old infos are
 * changed into the new ones one by one, and what is left over is removed, or added after the last info of the gap.
 * <p>
 * With a change set the changes get their marks, like list entries elsewhere: an info which is added is marked as
 * added, one which is removed is marked as removed and keeps its text, one which is changed gets a text mark with its
 * old text. An info which a change set has added is simply gone if it is removed; one which the change set has removed
 * and which is typed again is not removed any more.
 */
class AttributeInfosChanger_V001 {

    /** @param changeSet null if changes are not tracked
     * @param markable false if the marks inside the attribute are ignored in the display, then there are no marks */
    void apply(AttributeModel_V001 attribute, List<String> newInfos, String changeSet, boolean markable) {
        List<InfoModel_V001> current = currentInfos(attribute);
        int[][] matches = longestCommonSubsequence(current, newInfos);
        int oldStart = 0;
        int newStart = 0;
        InfoModel_V001 previous = null;
        for (int[] match : matches) {
            previous = changeGap(attribute, current.subList(oldStart, match[0]), newInfos.subList(newStart, match[1]),
                previous, changeSet, markable);
            previous = current.get(match[0]);
            oldStart = match[0] + 1;
            newStart = match[1] + 1;
        }
        changeGap(attribute, current.subList(oldStart, current.size()), newInfos.subList(newStart, newInfos.size()),
            previous, changeSet, markable);
        attribute.infos.removeIf(info -> info.text == null
            || (info.text.isEmpty() && info.change == null && info.textChange == null));
    }

    /** The infos as the user sees them: those which have a text and aren't removed. */
    static List<InfoModel_V001> currentInfos(AttributeModel_V001 attribute) {
        List<InfoModel_V001> current = new ArrayList<>();
        for (InfoModel_V001 info : attribute.infos) {
            if (info.text != null && !info.text.isEmpty() && !isRemoved(info)) {
                current.add(info);
            }
        }
        return current;
    }

    private static boolean isRemoved(InfoModel_V001 info) {
        return info.change != null && info.change.type == ChangeType_V001.REMOVED;
    }

    /** @return the last info of the gap, or the info which came before the gap if the gap has none */
    private InfoModel_V001 changeGap(AttributeModel_V001 attribute, List<InfoModel_V001> olds, List<String> news,
                                     InfoModel_V001 previous, String changeSet, boolean markable) {
        InfoModel_V001 last = olds.isEmpty() ? previous : olds.get(olds.size() - 1);
        int common = Math.min(olds.size(), news.size());
        for (int i = 0; i < common; i++) {
            InfoModel_V001 old = olds.get(i);
            TextTracking_V001.set(TextSlots_V001.infoOf(old), news.get(i), changeSet, markable && old.change == null);
        }
        for (int i = common; i < olds.size(); i++) {
            remove(attribute, olds.get(i), changeSet, markable);
        }
        for (int i = common; i < news.size(); i++) {
            last = add(attribute, news.get(i), last, changeSet, markable);
        }
        return last;
    }

    private void remove(AttributeModel_V001 attribute, InfoModel_V001 info, String changeSet, boolean markable) {
        if (changeSet == null || !markable || (info.change != null && info.change.type == ChangeType_V001.ADDED)) {
            attribute.infos.remove(info);
        }
        else {
            info.change = new ChangeModel_V001(ChangeType_V001.REMOVED, changeSet);
        }
    }

    /** @return the info which is there for the text now, to insert the next one after it */
    private InfoModel_V001 add(AttributeModel_V001 attribute, String text, InfoModel_V001 after, String changeSet,
                               boolean markable) {
        InfoModel_V001 removedBefore = removedByChangeSet(attribute, text, changeSet);
        if (removedBefore != null) {
            removedBefore.change = null;
            return removedBefore;
        }
        InfoModel_V001 added = new InfoModel_V001(text);
        if (changeSet != null && markable) {
            added.change = new ChangeModel_V001(ChangeType_V001.ADDED, changeSet);
        }
        attribute.infos.add(after == null ? 0 : attribute.infos.indexOf(after) + 1, added);
        return added;
    }

    private InfoModel_V001 removedByChangeSet(AttributeModel_V001 attribute, String text, String changeSet) {
        if (changeSet == null) {
            return null;
        }
        for (InfoModel_V001 info : attribute.infos) {
            if (isRemoved(info) && info.change.changeSet.equals(changeSet) && text.equals(info.text)) {
                return info;
            }
        }
        return null;
    }

    /** The pairs (index in the old infos, index in the new infos) of infos with the same text which keep their order. */
    private int[][] longestCommonSubsequence(List<InfoModel_V001> olds, List<String> news) {
        int[][] length = new int[olds.size() + 1][news.size() + 1];
        for (int i = olds.size() - 1; i >= 0; i--) {
            for (int j = news.size() - 1; j >= 0; j--) {
                length[i][j] = olds.get(i).text.equals(news.get(j))
                    ? length[i + 1][j + 1] + 1
                    : Math.max(length[i + 1][j], length[i][j + 1]);
            }
        }
        List<int[]> matches = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < olds.size() && j < news.size()) {
            if (olds.get(i).text.equals(news.get(j))) {
                matches.add(new int[] {i, j});
                i++;
                j++;
            }
            else if (length[i + 1][j] >= length[i][j + 1]) {
                i++;
            }
            else {
                j++;
            }
        }
        return matches.toArray(new int[0][]);
    }
}

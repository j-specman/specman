package classman.render;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Diff;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Operation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static classman.render.DiagramStyle.DIFF_MAX_RUNS;
import static classman.render.DiagramStyle.DIFF_MERGE_GAP;
import static classman.render.DiagramStyle.DIFF_MIN_TEXT_LENGTH;
import static classman.render.DiagramStyle.DIFF_WHOLE_SHARE;

/**
 * Tells which characters of a changed text show how it differs from its backup, without making the text longer: text
 * which is new is marked, and where text is gone, the nearest characters on both sides of the gap are marked, at the
 * start or end of the text only the one side. Text which is replaced is shown by the new text alone. Too many separate
 * passages are joined, the closest first. Short texts, and texts where the marks would cover most of the text anyway,
 * are marked as a whole.
 */
public class TextDiffMarks {
    private final DiffMatchPatch diffMatchPatch = new DiffMatchPatch();

    /** @return the marks on the current text, one covering all of it if the difference isn't worth showing in detail */
    public List<TextMark> of(String backup, String current, String changeSet) {
        List<TextMark> marks = new ArrayList<>();
        for (CharRange range : rangesOf(backup, current)) {
            marks.add(new TextMark(range.start, range.end, changeSet));
        }
        return marks;
    }

    private List<CharRange> rangesOf(String backup, String current) {
        if (backup == null || backup.isEmpty() || current.length() < DIFF_MIN_TEXT_LENGTH) {
            return wholeText(current);
        }
        List<CharRange> ranges = mergedNearRanges(differences(backup, current));
        if (ranges.isEmpty()) {
            return wholeText(current);
        }
        ranges = joinedUntilFew(ranges);
        return coveredLength(ranges) > DIFF_WHOLE_SHARE * current.length() ? wholeText(current) : ranges;
    }

    /** While there are more passages than {@link DiagramStyle#DIFF_MAX_RUNS}, the two which are closest together
     * become one. */
    private List<CharRange> joinedUntilFew(List<CharRange> ranges) {
        List<CharRange> joined = new ArrayList<>(ranges);
        while (joined.size() > DIFF_MAX_RUNS) {
            int closest = 0;
            for (int i = 1; i < joined.size() - 1; i++) {
                if (joined.get(i).gapTo(joined.get(i + 1)) < joined.get(closest).gapTo(joined.get(closest + 1))) {
                    closest = i;
                }
            }
            joined.set(closest, joined.get(closest).coveringWith(joined.get(closest + 1)));
            joined.remove(closest + 1);
        }
        return joined;
    }

    private int coveredLength(List<CharRange> ranges) {
        int covered = 0;
        for (CharRange range : ranges) {
            covered += range.end - range.start;
        }
        return covered;
    }

    private List<CharRange> wholeText(String current) {
        return List.of(new CharRange(0, current.length()));
    }

    private List<CharRange> differences(String backup, String current) {
        LinkedList<Diff> diffs = diffMatchPatch.diffMain(backup, current);
        diffMatchPatch.diffCleanupSemantic(diffs);
        List<CharRange> ranges = new ArrayList<>();
        int position = 0;
        for (int i = 0; i < diffs.size(); i++) {
            Diff diff = diffs.get(i);
            int length = diff.text.length();
            switch (diff.operation) {
                case EQUAL:
                    position += length;
                    break;
                case INSERT:
                    ranges.add(new CharRange(position, position + length));
                    position += length;
                    break;
                case DELETE:
                    if (!isReplaced(diffs, i)) {
                        ranges.add(aroundGap(current, position));
                    }
                    break;
            }
        }
        return ranges;
    }

    /** Text which is replaced by other text is shown by the new text, which is marked anyway. */
    private boolean isReplaced(List<Diff> diffs, int deletion) {
        boolean insertBefore = deletion > 0 && diffs.get(deletion - 1).operation == Operation.INSERT;
        boolean insertAfter = deletion + 1 < diffs.size() && diffs.get(deletion + 1).operation == Operation.INSERT;
        return insertBefore || insertAfter;
    }

    /** The nearest characters before and after the place where text is gone, which aren't blank, and the blanks
     * between them. At the start or end of the text there is only the side which has characters. */
    private CharRange aroundGap(String current, int position) {
        int before = position - 1;
        while (before >= 0 && Character.isWhitespace(current.charAt(before))) {
            before--;
        }
        int after = position;
        while (after < current.length() && Character.isWhitespace(current.charAt(after))) {
            after++;
        }
        int start = before >= 0 ? before : position;
        int end = after < current.length() ? after + 1 : position;
        return start < end ? new CharRange(start, end) : new CharRange(0, current.length());
    }

    private List<CharRange> mergedNearRanges(List<CharRange> ranges) {
        List<CharRange> merged = new ArrayList<>();
        for (CharRange range : ranges) {
            int last = merged.size() - 1;
            if (last >= 0 && merged.get(last).gapTo(range) <= DIFF_MERGE_GAP) {
                merged.set(last, merged.get(last).coveringWith(range));
            }
            else {
                merged.add(range);
            }
        }
        return merged;
    }
}

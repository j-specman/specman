package specman.editarea.markups;

import specman.ChangeSet;
import specman.editarea.document.WrappedPosition;

import java.util.ArrayList;
import java.util.List;

import static specman.ChangeSet.changeset;

public class MarkedCharSequence {
  final List<MarkedChar> chars;

  private MarkedCharSequence(List<MarkedChar> chars) {
    this.chars = chars;
  }

  public MarkedCharSequence() { this(new ArrayList<>()); }

  public void add(final MarkedChar mc) {
    chars.add(mc);
  }

  public Integer findRight(int pos, CharType charType) {
    while (pos < chars.size()) {
      MarkedChar mc = chars.get(pos);
      if (charType.is(mc.c)) {
        return pos;
      }
      pos++;
    }
    return null;
  }

  public MarkedChar get(int pos) {
    return chars.get(pos);
  }

  public TextMarkup type(int pos) {
    return get(pos).markup;
  }

  public boolean isVisibleChar(int pos) {
    char c = get(pos).c;
    return CharType.NonWhitespace.is(c);
  }

  public void insertParagraphBoundaryAt(WrappedPosition pos, boolean changed) {
    int modelPos = pos.toModel();
    TextMarkup markup = changed
      ? new TextMarkup(MarkupType.Changed, changeset())
      : null;
    chars.add(modelPos, new MarkedChar('\n', markup));
  }

  public void append(MarkedCharSequence changemarks) {
    chars.addAll(changemarks.chars);
  }

  public int size() { return chars.size(); }

  public MarkedCharSequence subsequence(int fromIndex, int toIndex) {
    return new MarkedCharSequence(chars.subList(fromIndex, toIndex));
  }
}

package specman.search;

import specman.metatag.generic.GenericMetaTag;
import specman.undo.UndoableMetaTagFreetextReplaceAll;
import specman.undo.UndoableMetaTagFreetextReplaceAll.FreetextChange;

import javax.swing.SwingUtilities;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import static specman.Specman.editor;

/**
 * Search and replace in the freetexts of the meta tags. A hit is shown by opening the freetext
 * editor of the tag with the hit selected. The search continues behind the last hit (in front of it
 * when searching up) and wraps around.
 */
public class MetaTagFreetextSearch {
  private GenericMetaTag lastHitTag;
  private int lastHitStart;
  private int lastHitEnd;

  /** Forgets the last hit, so the next search starts at the beginning (end when searching up). */
  public void reset() {
    lastHitTag = null;
  }

  /** @return whether there was a hit */
  public boolean findNext(String key, TextMatcher matcher) {
    if (!findNextHit(key, matcher)) {
      return false;
    }
    show(lastHitTag);
    return true;
  }

  /** Replaces the next hit and shows the replacement.
   * @return whether there was a hit */
  public boolean replace(String key, String replacement, TextMatcher matcher) {
    if (!findNextHit(key, matcher)) {
      return false;
    }
    GenericMetaTag tag = lastHitTag;
    String freetext = tag.getFreetext();
    tag.replaceFreetext(freetext.substring(0, lastHitStart) + replacement + freetext.substring(lastHitEnd));
    lastHitEnd = lastHitStart + replacement.length();
    show(tag);
    return true;
  }

  /** Replaces all hits in all freetexts as one undoable action.
   * @return the number of replacements */
  public int replaceAll(String key, String replacement, TextMatcher matcher) {
    TextMatcher forwardMatcher = matcher.searchingForwards();
    int replacements = 0;
    List<FreetextChange> changes = new ArrayList<>();
    for (GenericMetaTag tag : editor().queryTagsWithFreetext()) {
      String freetext = tag.getFreetext();
      StringBuilder replaced = new StringBuilder();
      int copiedUntil = 0;
      for (int hit = forwardMatcher.indexOfHit(freetext, key, 0); hit >= 0; hit = forwardMatcher.indexOfHit(freetext, key, copiedUntil)) {
        replaced.append(freetext, copiedUntil, hit).append(replacement);
        copiedUntil = hit + key.length();
        replacements++;
      }
      if (copiedUntil > 0) {
        String newFreetext = replaced.append(freetext, copiedUntil, freetext.length()).toString();
        tag.setFreetext(newFreetext);
        changes.add(new FreetextChange(tag, freetext, newFreetext));
      }
    }
    if (!changes.isEmpty()) {
      editor().addEdit(new UndoableMetaTagFreetextReplaceAll(changes, replacements));
    }
    reset();
    return replacements;
  }

  /** Sets the last hit to the next one. */
  private boolean findNextHit(String key, TextMatcher matcher) {
    boolean searchUp = matcher.isSearchUp();
    List<GenericMetaTag> tags = editor().queryTagsWithFreetext();
    int tagCount = tags.size();
    if (tagCount == 0) {
      return false;
    }
    int startTagIndex = tags.indexOf(lastHitTag);
    if (startTagIndex < 0) {
      lastHitTag = null;
      startTagIndex = searchUp ? tagCount - 1 : 0;
    }

    // One step more than there are tags: the last one searches the start tag again, but the
    // part of it which lies on the other side of the last hit.
    for (int step = 0; step <= tagCount; step++) {
      int tagIndex = Math.floorMod(startTagIndex + (searchUp ? -step : step), tagCount);
      GenericMetaTag tag = tags.get(tagIndex);
      String freetext = tag.getFreetext();
      int pos;
      if (step == 0 && lastHitTag != null) {
        pos = searchUp ? lastHitStart : lastHitEnd;
      } else {
        pos = searchUp ? freetext.length() : 0;
      }
      int index = matcher.indexOfHit(freetext, key, pos);
      if (index >= 0) {
        lastHitTag = tag;
        lastHitStart = index;
        lastHitEnd = index + key.length();
        return true;
      }
    }
    return false;
  }

  private void show(GenericMetaTag tag) {
    int start = lastHitStart;
    int end = lastHitEnd;
    editor().unfoldToShow(tag);
    // Unfolding is only effective after the layout pass it has triggered
    SwingUtilities.invokeLater(() -> {
      tag.scrollRectToVisible(new Rectangle(tag.getSize()));
      tag.editFreetext(start, end);
    });
  }
}

package specman.editarea.image;

import specman.ChangeSet;

/** The source of a diagram from before a change set changed it. Like the markups of a text, it is the content's own
 * change tracking: The area as a whole stays unchanged as far as its change info is concerned. */
public class SourceBackup {
  private final String source;
  private final ChangeSet changeSet;

  public SourceBackup(String source, ChangeSet changeSet) {
    this.source = source;
    this.changeSet = changeSet;
  }

  public String source() { return source; }

  public ChangeSet changeSet() { return changeSet; }

  public boolean changedBy(ChangeSet by) { return changeSet == by; }

  public SourceBackup reassign(ChangeSet target) { return new SourceBackup(source, target); }
}

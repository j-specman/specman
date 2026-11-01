package specman.editarea.plantuml;

import specman.ChangeSet;

/** The PlantUML source from before a change set changed it. Like the markups of a text, it is the content's own
 * change tracking: The area as a whole stays unchanged as far as its change info is concerned. */
public class PlantUmlSourceBackup {
  private final String source;
  private final ChangeSet changeSet;

  public PlantUmlSourceBackup(String source, ChangeSet changeSet) {
    this.source = source;
    this.changeSet = changeSet;
  }

  public String source() { return source; }

  public ChangeSet changeSet() { return changeSet; }

  public boolean changedBy(ChangeSet by) { return changeSet == by; }

  public PlantUmlSourceBackup reassign(ChangeSet target) { return new PlantUmlSourceBackup(source, target); }
}

package specman;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Objects;

import static specman.Aenderungsart.Geloescht;
import static specman.Aenderungsart.Hinzugefuegt;
import static specman.Aenderungsart.Untracked;
import static specman.ChangeSet.changeset;
import static specman.graphics.Styles.BACKGROUND_COLOR_STANDARD;
import static specman.util.ObjectUtils.nvl;

public class ChangeInfo {

  public static final ChangeInfo UNTRACKED = new ChangeInfo(Untracked, null);

  private final Aenderungsart art;
  private final ChangeSet changeSet;

  public ChangeInfo(Aenderungsart art, ChangeSet changeSet) {
    if (art == null) throw new IllegalArgumentException("art must not be null");
    if (art == Untracked && changeSet != null) throw new IllegalArgumentException("Untracked must not have a changeSet");
    if (art != Untracked && changeSet == null) throw new IllegalArgumentException("Non-untracked ChangeInfo requires a changeSet");
    this.art = art;
    this.changeSet = changeSet;
  }

  public static ChangeInfo added() { return new ChangeInfo(Hinzugefuegt, changeset()); }
  public static ChangeInfo untracked() { return UNTRACKED; }

  public Aenderungsart art() { return art; }
  public ChangeSet changeSet() { return changeSet; }
  public String changeSetName() { return changeSet != null ? changeSet.name : null; }

  public Color panelColor() {
    return isChange() ? changeSet.panelColor() : BACKGROUND_COLOR_STANDARD;
  }

  public boolean isUntracked() { return art == Untracked; }
  public boolean isAdded() { return art == Hinzugefuegt; }
  public boolean isDeleted() { return art == Geloescht; }
  public boolean isSourceStep() { return art == Aenderungsart.Quellschritt; }
  public boolean isTargetStep() { return art == Aenderungsart.Zielschritt; }
  public boolean isChange() { return art.istAenderung(); }
  public int numChanges() { return art.asNumChanges(); }

  public ChangeInfo withArt(Aenderungsart newArt) {
    return new ChangeInfo(newArt, changeSet);
  }

  @Deprecated
  /** Use {@link #deleted(ChangeSet)} instead. */
  public ChangeInfo deleted() { return withArt(Geloescht); }

  public ChangeInfo deleted(ChangeSet triggerSet) { return new ChangeInfo(Geloescht, triggerSet); }

  @Override
  public String toString() { return art + ((changeSet != null) ? ", " + changeSet : ""); }

  public ChangeInfo toQuellschritt() {
    return new ChangeInfo(Aenderungsart.Quellschritt, nvl(this.changeSet(), changeset()));
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    ChangeInfo that = (ChangeInfo) o;
    return art == that.art && changeSet == that.changeSet;
  }

  @Override
  public int hashCode() {
    return Objects.hash(art, changeSet);
  }

  public int numChangesBy(ChangeSet by) {
    return changedBy(by) ? art.asNumChanges() : 0;
  }

  public boolean addedBy(ChangeSet by) {
    return changedBy(by) ? art == Hinzugefuegt : false;
  }

  public boolean deletedBy(ChangeSet by) {
    return changedBy(by) ? art == Geloescht : false;
  }

  public boolean changedBy(ChangeSet by) {
    return changeSet == by;
  }

  public ChangeInfo untrack(ChangeSet by) {
    return changedBy(by) ? UNTRACKED : this;
  }

  public ChangeInfo reassign(@NotNull ChangeSet target) {
    return new ChangeInfo(art, target);
  }
}

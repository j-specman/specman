package specman;

import specman.model.v001.ChangeInfo_V001;

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

  /** This method allows to read files from Specman version 1.1.x which do not contain {@link ChangeInfo_V001}s but
   * only {@link Aenderungsart}s without an info about the change set which caused the modification. So if we don't
   * find a change info, we use the fallback, combined with the current change set. Specman warns about that once
   * <i>writing</i> the file with Specman 1.2 or higher, it won't longer be readable for older Specman versions. */
  public static ChangeInfo fromModel(ChangeInfo_V001 changeInfo, Aenderungsart fallback) {
    if (changeInfo != null) {
      return changeInfo.toChangeInfo();
    }
    if (fallback == null || fallback == Untracked) {
      return UNTRACKED;
    }
    return new ChangeInfo(fallback, changeset());
  }

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
}

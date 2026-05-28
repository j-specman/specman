package specman;

import specman.model.v001.ChangeInfo_V001;

import java.awt.*;

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

}

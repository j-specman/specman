package specman;

import specman.graphics.IconReader;
import specman.ops.buttons.AcceptChangesADBLOpButton;
import specman.ops.buttons.BirdsViewSpecmanOpButton;
import specman.ops.buttons.DeleteStepOpButton;
import specman.ops.buttons.ExportPDFOpButton;
import specman.ops.buttons.MergeChangeSetsADBLOpButton;
import specman.ops.buttons.RejectChangesADBLOpButton;
import specman.ops.buttons.ReviewOpButton;

import specman.ops.buttons.ToggleBorderTypeOpButton;
import specman.ops.buttons.ToneOpButton;

import javax.swing.*;
import java.awt.*;

import static specman.i18n.Messages.i18n;

public class DiagramToolBar extends JToolBar {

  public static final String SHADE_BUTTON = "toolbar.shade";
  public static final String DELETE_STEP_BUTTON = "toolbar.delete.step";
  public static final String TOGGLE_BORDER_BUTTON = "toolbar.toggle.border";
  public static final String TRACK_CHANGES_BUTTON = "toolbar.track.changes";
  public static final String ACCEPT_CHANGES_BUTTON = "toolbar.accept.changes";
  public static final String REJECT_CHANGES_BUTTON = "toolbar.reject.changes";
  public static final String MERGE_CHANGESETS_BUTTON = "toolbar.merge.changesets";
  public static final String REVIEW_BUTTON = "toolbar.review";
  public static final String BIRDSVIEW_BUTTON = "toolbar.birdsview";
  public static final String EXPORT_PDF_BUTTON = "toolbar.export.pdf";

  private final JToggleButton aenderungenVerfolgen;
  private final AcceptChangesADBLOpButton aenderungenUebernehmen;
  private final RejectChangesADBLOpButton aenderungenVerwerfen;
  private final MergeChangeSetsADBLOpButton mergeChangeSets;
  private final ZoomComboBox zoom;

  DiagramToolBar(Specman specman) {
    setFloatable(false);

    ToneOpButton einfaerben = new ToneOpButton(specman);
    DeleteStepOpButton loeschen = new DeleteStepOpButton(specman);
    ToggleBorderTypeOpButton toggleBorderType = new ToggleBorderTypeOpButton(specman);

    aenderungenVerfolgen = new JToggleButton();
    aenderungenUebernehmen = new AcceptChangesADBLOpButton(specman);
    aenderungenVerwerfen = new RejectChangesADBLOpButton(specman);
    mergeChangeSets = new MergeChangeSetsADBLOpButton(specman);
    updateChangeSetColor(specman.changeset());
    aenderungenVerfolgen.addChangeListener(e ->
        aenderungenVerfolgen.setBackground(aenderungenVerfolgen.isSelected()
            ? specman.changeset().activeButtonColor()
            : specman.changeset().buttonColor()));

    ReviewOpButton review = new ReviewOpButton(specman);
    zoom = new ZoomComboBox(specman);
    BirdsViewSpecmanOpButton birdsview = new BirdsViewSpecmanOpButton(specman);
    ExportPDFOpButton exportPDF = new ExportPDFOpButton(specman);

    addButton(einfaerben, "helligkeit", SHADE_BUTTON);
    addButton(loeschen, "loeschen", DELETE_STEP_BUTTON);
    addButton(toggleBorderType, "switch-border", TOGGLE_BORDER_BUTTON);
    addSeparator();
    addButton(aenderungenVerfolgen, "aenderungen", TRACK_CHANGES_BUTTON);
    addButton(aenderungenUebernehmen, "uebernehmen", ACCEPT_CHANGES_BUTTON);
    addButton(aenderungenVerwerfen, "verwerfen", REJECT_CHANGES_BUTTON);
    addButton(mergeChangeSets, "merge", MERGE_CHANGESETS_BUTTON);
    addButton(review, "review", REVIEW_BUTTON);
    addSeparator();
    add(zoom);
    addButton(birdsview, "birdsview", BIRDSVIEW_BUTTON);
    addButton(exportPDF, "pdf", EXPORT_PDF_BUTTON);
  }

  /** The button is named like its tooltip's message key, which is how tests find it. */
  private void addButton(AbstractButton button, String iconBasename, String tooltipKey) {
    button.setIcon(IconReader.readImageIcon(iconBasename));
    button.setMargin(new Insets(0, 0, 0, 0));
    button.setToolTipText(i18n(tooltipKey));
    button.setName(tooltipKey);
    add(button);
  }

  public void setChangeModeEnabled(boolean enabled) {
    aenderungenVerfolgen.setSelected(enabled);
  }

  public boolean isChangeModeEnabled() {
    return aenderungenVerfolgen.isSelected();
  }

  public void updateChangeSet(ChangeSet changeSet) {
    updateChangeSetColor(changeSet);
  }

  private void updateChangeSetColor(ChangeSet changeSet) {
    aenderungenVerfolgen.setBackground(aenderungenVerfolgen.isSelected()
        ? changeSet.activeButtonColor()
        : changeSet.buttonColor());
    aenderungenUebernehmen.setBackground(changeSet.buttonColor());
    aenderungenVerwerfen.setBackground(changeSet.buttonColor());
    mergeChangeSets.setBackground(changeSet.buttonColor());
  }

  public void updateZoomDisplay(int prozent) {
    zoom.updateDisplay(prozent);
  }

}

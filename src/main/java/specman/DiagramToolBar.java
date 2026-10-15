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

    addButton(einfaerben, "helligkeit", "toolbar.shade");
    addButton(loeschen, "loeschen", "toolbar.delete.step");
    addButton(toggleBorderType, "switch-border", "toolbar.toggle.border");
    addSeparator();
    addButton(aenderungenVerfolgen, "aenderungen", "toolbar.track.changes");
    addButton(aenderungenUebernehmen, "uebernehmen", "toolbar.accept.changes");
    addButton(aenderungenVerwerfen, "verwerfen", "toolbar.reject.changes");
    addButton(mergeChangeSets, "merge", "toolbar.merge.changesets");
    addButton(review, "review", "toolbar.review");
    addSeparator();
    add(zoom);
    addButton(birdsview, "birdsview", "toolbar.birdsview");
    addButton(exportPDF, "pdf", "toolbar.export.pdf");
  }

  private void addButton(AbstractButton button, String iconBasename, String tooltipKey) {
    button.setIcon(IconReader.readImageIcon(iconBasename));
    button.setMargin(new Insets(0, 0, 0, 0));
    button.setToolTipText(i18n(tooltipKey));
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

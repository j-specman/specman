package specman;

import specman.graphics.IconReader;
import specman.ops.buttons.AcceptChangesADBLOpButton;
import specman.ops.buttons.BirdsViewSpecmanOpButton;
import specman.ops.buttons.DeleteStepOpButton;
import specman.ops.buttons.ExportPDFOpButton;
import specman.ops.buttons.RejectChangesADBLOpButton;
import specman.ops.buttons.ReviewOpButton;

import specman.ops.buttons.ToggleBorderTypeOpButton;
import specman.ops.buttons.ToneOpButton;

import javax.swing.*;
import java.awt.*;

public class DiagramToolBar extends JToolBar {

  private final JToggleButton aenderungenVerfolgen;
  private final AcceptChangesADBLOpButton aenderungenUebernehmen;
  private final RejectChangesADBLOpButton aenderungenVerwerfen;
  private final ZoomComboBox zoom;

  DiagramToolBar(Specman specman) {
    setFloatable(false);

    ToneOpButton einfaerben = new ToneOpButton(specman);
    DeleteStepOpButton loeschen = new DeleteStepOpButton(specman);
    ToggleBorderTypeOpButton toggleBorderType = new ToggleBorderTypeOpButton(specman);

    aenderungenVerfolgen = new JToggleButton();
    aenderungenUebernehmen = new AcceptChangesADBLOpButton(specman);
    aenderungenVerwerfen = new RejectChangesADBLOpButton(specman);
    updateChangeSetColor(specman.changeset());
    aenderungenVerfolgen.addChangeListener(e ->
        aenderungenVerfolgen.setBackground(aenderungenVerfolgen.isSelected()
            ? specman.changeset().activeButtonColor()
            : specman.changeset().buttonColor()));

    ReviewOpButton review = new ReviewOpButton(specman);
    zoom = new ZoomComboBox(specman);
    BirdsViewSpecmanOpButton birdsview = new BirdsViewSpecmanOpButton(specman);
    ExportPDFOpButton exportPDF = new ExportPDFOpButton(specman);

    addButton(einfaerben, "helligkeit", "Hintergrund schattieren");
    addButton(loeschen, "loeschen", "Schritt löschen");
    addButton(toggleBorderType, "switch-border", "Rahmen umschalten");
    addSeparator();
    addButton(aenderungenVerfolgen, "aenderungen", "Änderungen verfolgen");
    addButton(aenderungenUebernehmen, "uebernehmen", "Änderungen übernehmen");
    addButton(aenderungenVerwerfen, "verwerfen", "Änderungen verwerfen");
    addButton(review, "review", "Für Review zusammenklappen");
    addSeparator();
    add(zoom);
    addButton(birdsview, "birdsview", "Bird's View");
    addButton(exportPDF, "pdf", "PDF exportieren");
  }

  private void addButton(AbstractButton button, String iconBasename, String tooltip) {
    button.setIcon(IconReader.readImageIcon(iconBasename));
    button.setMargin(new Insets(0, 0, 0, 0));
    button.setToolTipText(tooltip);
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
  }

  public void updateZoomDisplay(int prozent) {
    zoom.updateDisplay(prozent);
  }

}

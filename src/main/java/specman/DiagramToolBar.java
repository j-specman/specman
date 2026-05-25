package specman;

import specman.graphics.IconReader;
import specman.ops.buttons.AcceptChangesADBLOpButton;
import specman.ops.buttons.BirdsViewSpecmanOpButton;
import specman.ops.buttons.DeleteStepOpButton;
import specman.ops.buttons.ExportPDFOpButton;
import specman.ops.buttons.RevertChangesADBLOpButton;
import specman.ops.buttons.ReviewOpButton;
import specman.ops.buttons.ToggleBorderTypeOpButton;
import specman.ops.buttons.ToneOpButton;

import javax.swing.*;
import java.awt.*;

public class DiagramToolBar extends JToolBar {

  private final JToggleButton aenderungenVerfolgen;
  private final ZoomComboBox zoom;

  DiagramToolBar(Specman specman) {
    setFloatable(false);

    ToneOpButton einfaerben = new ToneOpButton(specman);
    DeleteStepOpButton loeschen = new DeleteStepOpButton(specman);
    ToggleBorderTypeOpButton toggleBorderType = new ToggleBorderTypeOpButton(specman);

    Color changeColor = specman.changeset().panelColor();
    aenderungenVerfolgen = new JToggleButton();
    AcceptChangesADBLOpButton aenderungenUebernehmen = new AcceptChangesADBLOpButton(specman);
    RevertChangesADBLOpButton aenderungenVerwerfen = new RevertChangesADBLOpButton(specman);
    aenderungenVerfolgen.setBackground(changeColor);
    aenderungenUebernehmen.setBackground(changeColor);
    aenderungenVerwerfen.setBackground(changeColor);

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

  public void updateZoomDisplay(int prozent) {
    zoom.updateDisplay(prozent);
  }

}

package specman.view;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import specman.Aenderungsart;
import specman.ChangeInfo;

import javax.swing.*;

/** A bottom filler is a panel at the bottom of structured step which
 * can be folded (see class {@link KlappButton}). The filler covers the
 * space below a folded step which is located at the very end of an
 * overlong sequence. Overlength can result from sister sequences in
 * if/else and case steps which need more space. Without the filler, a
 * folded step at the sequence end would cause the black background panel
 * become visible at the bottom. */
public class BottomFiller extends JPanel {
  final int row;

  public BottomFiller(JPanel parent, FormLayout layout, ChangeInfo changeInfo) {
    layout.appendRow(RowSpec.decode(KlappButton.ZEILENLAYOUT_FILLER_HIDDEN));
    row = layout.getRowCount();
    parent.add(this, CC.xyw(1, row, layout.getColumnCount()));
    setBackground(changeInfo.panelColor());
  }
}

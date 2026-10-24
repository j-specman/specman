package net.atlanticbb.tantlinger.ui.text.actions;

import javax.swing.JDialog;
import java.awt.Dimension;
import java.awt.Window;
import java.io.Serial;

/** Window around the {@link StepnumberLinkPanel}. The separation allows to use the
 * panel in headless tests while the dialog is only suitable to be instanciated
 * in a real UI. */
public class StepnumberLinkDialog extends JDialog {
    @Serial
    private static final long serialVersionUID = 1L;

    public StepnumberLinkDialog(Window parent, StepnumberLinkPanel panel) {
        super(parent, StepnumberLinkPanel.title());
        panel.onClose(() -> setVisible(false));
        getContentPane().add(panel);
        setMinimumSize(new Dimension(400, 600));
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
        setVisible(true);
    }
}

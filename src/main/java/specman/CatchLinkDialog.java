package specman;

import javax.swing.JDialog;
import java.awt.Frame;
import java.io.Serial;

/** Window around the {@link CatchLinkPanel}. The separation allows to use the
 * panel in headless tests while the dialog is only suitable to be instanciated
 * in a real UI. */
public class CatchLinkDialog extends JDialog {
    @Serial
    private static final long serialVersionUID = 1L;

    public CatchLinkDialog(Frame parent, CatchLinkPanel panel) {
        super(parent, CatchLinkPanel.title());
        panel.onClose(() -> setVisible(false));
        getContentPane().add(panel);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
        setVisible(true);
    }
}

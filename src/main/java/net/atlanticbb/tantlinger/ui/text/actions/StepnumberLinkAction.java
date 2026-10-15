package net.atlanticbb.tantlinger.ui.text.actions;

import net.atlanticbb.tantlinger.ui.UIUtils;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.Serial;

import static specman.i18n.Messages.i18n;

public class StepnumberLinkAction extends BasicEditAction {
    @Serial
    private static final long serialVersionUID = 1L;
    StepnumberLinkDialog dialog;

    public StepnumberLinkAction() {
        super(i18n("action.stepnumberlink"));
        this.putValue("SmallIcon", UIUtils.getIcon("resources/images/x16/", "link3.png"));
        this.putValue("ShortDescription", this.getValue("Name"));
    }

    /**
     * Always recreate the dialog since the stepnumberlinks are only fetched on creation
     */
    protected void doEdit(ActionEvent e, JEditorPane ed) {
        Component c = SwingUtilities.getWindowAncestor(ed);

        if (c instanceof Frame) {
            this.dialog = new StepnumberLinkDialog((Frame) c, ed);
        } else {
            if (!(c instanceof Dialog)) {
                return;
            }

            this.dialog = new StepnumberLinkDialog((Dialog) c, ed);
        }

        if (!this.dialog.isVisible()) {
            this.dialog.setLocationRelativeTo(c);
            this.dialog.setVisible(true);
        }

    }

    protected void updateContextState(JEditorPane editor) {
        if (this.dialog != null) {
            this.dialog.setJTextComponent(editor);
        }

    }
}
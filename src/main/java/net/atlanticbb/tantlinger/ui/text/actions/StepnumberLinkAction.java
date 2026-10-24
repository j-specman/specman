package net.atlanticbb.tantlinger.ui.text.actions;

import net.atlanticbb.tantlinger.ui.UIUtils;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.Serial;

import static specman.i18n.Messages.i18n;

public class StepnumberLinkAction extends BasicEditAction {
    @Serial
    private static final long serialVersionUID = 1L;
    private StepnumberLinkPanel panel;
    private boolean dialogDisplayed = true;

    public StepnumberLinkAction() {
        super(i18n("action.stepnumberlink"));
        this.putValue("SmallIcon", UIUtils.getIcon("resources/images/x16/", "link3.png"));
        this.putValue("ShortDescription", this.getValue("Name"));
    }

    /** Allows tests to operate the {@link StepnumberLinkPanel} without displaying
     * the dialog around it */
    public void setDialogDisplayed(boolean dialogDisplayed) {
        this.dialogDisplayed = dialogDisplayed;
    }

    /** The panel of the dialog opened by the last execution of the action. */
    public StepnumberLinkPanel stepnumberLinkPanel() {
        return panel;
    }

    /**
     * Always recreate the panel since the stepnumberlinks are only fetched on creation
     */
    protected void doEdit(ActionEvent e, JEditorPane ed) {
        panel = new StepnumberLinkPanel(ed);
        Window window = SwingUtilities.getWindowAncestor(ed);
        if (dialogDisplayed && window != null) {
            new StepnumberLinkDialog(window, panel);
        }
    }

    protected void updateContextState(JEditorPane editor) {
        if (this.panel != null) {
            this.panel.setJTextComponent(editor);
        }
    }
}

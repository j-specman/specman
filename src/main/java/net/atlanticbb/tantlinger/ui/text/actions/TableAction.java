package net.atlanticbb.tantlinger.ui.text.actions;

import net.atlanticbb.tantlinger.ui.UIUtils;
import net.atlanticbb.tantlinger.ui.text.actions.BasicEditAction;
import specman.Specman;
import static specman.Specman.editor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.Serial;

public class TableAction extends BasicEditAction {
    private TableDialog dialog;

    public TableAction() {
        super(i18n.str("table_"));
        this.putValue("SmallIcon", UIUtils.getIcon("resources/images/x16/", "table.png"));
        this.putValue("ShortDescription", this.getValue("Name"));
    }

    @Override
    protected void doEdit(ActionEvent actionEvent, JEditorPane ed) {
        Component c = SwingUtilities.getWindowAncestor(ed);
        if (c instanceof Frame) {
            this.dialog = new TableDialog((Frame) c);
        }
        else if (c instanceof Dialog) {
            this.dialog = new TableDialog((Dialog) c);
        }
        else {
            return;
        }

        if (!this.dialog.isVisible()) {
            this.dialog.setLocationRelativeTo(c);
            this.dialog.setVisible(true);
        }

    }


    //editor().addTable();
}
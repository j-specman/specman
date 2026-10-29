package net.atlanticbb.tantlinger.ui.text.actions;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import net.atlanticbb.tantlinger.i18n.I18n;
import net.atlanticbb.tantlinger.ui.UIUtils;
import specman.i18n.IHeaderPanel;
import specman.i18n.ILabel;
import static specman.Specman.editor;
import static specman.i18n.Messages.i18n;

import javax.swing.*;
import java.awt.*;

public class TableDialog extends JDialog {
    private static final I18n i18n = I18n.getInstance("net.atlanticbb.tantlinger.ui");
    private static final Icon icon = UIUtils.getIcon("resources/images/x32/", "table.png");
    private static final String TITLE_KEY = "dialog.table.title";
    private static final String title = i18n(TITLE_KEY);
    private JTextField columns = new JTextField("3");
    private JTextField rows = new JTextField("2");
    private JButton ok = new JButton(i18n.str("ok"));
    private JButton cancel = new JButton(i18n.str("cancel"));

    public TableDialog(Frame parent) {
        super(parent, title);
        this.init();
    }

    public TableDialog(Dialog parent) {
        super(parent, title);
        this.init();
    }

    private void init() {
        FormLayout layout = new FormLayout(
          "10px, 150px, 10px, 150px, 10px",
          "pref, 20px, 20px, 20px, 10px, 30px, 10px");
        Container contentPane = this.getContentPane();
        contentPane.setLayout(layout);
        JPanel headerPanel = new IHeaderPanel(TITLE_KEY, "dialog.table.description", icon);
        contentPane.add(headerPanel, CC.xywh(1, 1, 5, 1));
        contentPane.add(new ILabel("dialog.table.size"), CC.xywh(1, 2, 3, 1));
        contentPane.add(new ILabel("dialog.table.columns"), CC.xywh(2, 3, 1, 1));
        contentPane.add(columns, CC.xywh(4, 3, 1, 1));
        contentPane.add(new ILabel("dialog.table.rows"), CC.xywh(2, 4, 1, 1));
        contentPane.add(rows, CC.xywh(4, 4, 1, 1));
        contentPane.add(ok, CC.xywh(2, 6, 1, 1));
        contentPane.add(cancel, CC.xywh(4, 6, 1, 1));
        cancel.addActionListener(e -> cancel());
        ok.addActionListener(e -> ok());
        this.getRootPane().setDefaultButton(cancel);
        this.pack();
        this.setResizable(false);
    }

    private void cancel() {
        setVisible(false);
    }

    private void ok() {
        int numColumns = Integer.parseInt(columns.getText());
        int numRows = Integer.parseInt(rows.getText());
        if (numColumns > 0 && numColumns < 21 && numRows > 0 && numRows < 21) {
            editor().addTable(numColumns, numRows);
            setVisible(false);
        }
    }

}
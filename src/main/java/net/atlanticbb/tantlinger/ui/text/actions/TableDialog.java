package net.atlanticbb.tantlinger.ui.text.actions;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import net.atlanticbb.tantlinger.i18n.I18n;
import net.atlanticbb.tantlinger.ui.HeaderPanel;
import net.atlanticbb.tantlinger.ui.UIUtils;
import org.apache.commons.lang.StringUtils;
import specman.Specman;
import specman.editarea.TextEditArea;
import specman.view.AbstractSchrittView;
import static specman.Specman.editor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.Serial;
import java.util.List;

public class TableDialog extends JDialog {
    private static final I18n i18n = I18n.getInstance("net.atlanticbb.tantlinger.ui");
    private static final Icon icon = UIUtils.getIcon("resources/images/x32/", "table.png");
    private static final String title = "Insert table...";
    private static final String desc = "Insert a table";
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
          "50px, 20px, 20px, 20px, 10px, 30px, 10px");
        Container contentPane = this.getContentPane();
        contentPane.setLayout(layout);
        JPanel headerPanel = new HeaderPanel(title, desc, icon);
        contentPane.add(headerPanel, CC.xywh(1, 1, 5, 1));
        contentPane.add(new JLabel("Tabellengröße"), CC.xywh(1, 2, 3, 1));
        contentPane.add(new JLabel("Spaltenanzahl"), CC.xywh(2, 3, 1, 1));
        contentPane.add(columns, CC.xywh(4, 3, 1, 1));
        contentPane.add(new JLabel("Zeilenanzahl"), CC.xywh(2, 4, 1, 1));
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
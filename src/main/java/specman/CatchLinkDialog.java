package specman;

import net.atlanticbb.tantlinger.i18n.I18n;
import net.atlanticbb.tantlinger.ui.HeaderPanel;
import net.atlanticbb.tantlinger.ui.UIUtils;
import org.apache.commons.lang.StringUtils;
import specman.undo.manager.UndoRecording;
import specman.view.*;
import static specman.Specman.editor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.Serial;
import java.util.List;

/** This dialog is shown when the user is about to add a catch sequence. Catch sequences
 * have to be linked to a yet un-linked break steps within the same parent sequence or
 * any of its child sequences. The system provides an appropriate list here to select from. */
public class CatchLinkDialog extends JDialog {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final I18n i18n = I18n.getInstance("net.atlanticbb.tantlinger.ui.text.dialogs");
    private static final Icon icon = UIUtils.getIcon("resources/images/x32/", "link3.png");
    private static final String title = "Link to break step...";
    private static final String desc = "Create a catch sequence linked to a break step";
    private static final Color DEFAULT_BACKGROUND = Color.white;
    private static final Color HOVERED_BACKGROUND = Color.lightGray;
    private static final int MAXIMAL_REFERENCE_TEXTLENGTH = 60;
    private int hoveredIndex = -1;
    private SchrittSequenzView sequenz;
    private CatchUeberschrift referenceCatchHeading;
    private final CustomListCellRenderer customListCellRenderer = new CustomListCellRenderer();

    public CatchLinkDialog(Frame parent, SchrittSequenzView sequenz, CatchUeberschrift referenceCatchHeading) {
        super(parent, title);
        this.sequenz = sequenz;
        this.referenceCatchHeading = referenceCatchHeading;
        this.init();
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    private void init() {
        JPanel headerPanel = new HeaderPanel(title, desc, icon);

        JList<BreakSchrittView> jlist = new JList<>(getStepnumberList());
        jlist.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                hoveredIndex = -1;
                jlist.repaint();
            }
        });
        jlist.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Point position = e.getPoint();
                JList<AbstractSchrittView> jlist = (JList<AbstractSchrittView>) (e.getComponent());
                hoveredIndex = jlist.locationToIndex(position);
                jlist.repaint();
            }
        });
        jlist.addListSelectionListener(this::listValueChanged);
        jlist.setCellRenderer(customListCellRenderer);


        jlist.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 10, 10), new MatteBorder(1, 1, 1, 1, Color.black)));

        JButton close = new JButton(i18n.str("close"));
        close.addActionListener(e -> CatchLinkDialog.this.setVisible(false));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(close);
        this.getRootPane().setDefaultButton(close);
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(headerPanel, "North");
        this.getContentPane().add(jlist, "Center");
        this.getContentPane().add(buttonPanel, "South");
        this.pack();
        this.setResizable(false);
    }

    private DefaultListModel<BreakSchrittView> getStepnumberList() {
        DefaultListModel<BreakSchrittView> listModel = new DefaultListModel<>();
        final List<BreakSchrittView> steps = sequenz.queryUnlinkedBreakSteps();
        listModel.addAll(steps);
        return listModel;
    }

    private void listValueChanged(ListSelectionEvent e) {
      if (e.getValueIsAdjusting()) {
        EditorI editor = editor();
        try (UndoRecording ur = editor.composeUndo()) {
          BreakSchrittView breakStepToLink = ((JList<BreakSchrittView>) e.getSource()).getSelectedValue();
          CatchSchrittSequenzView catchSequence;
          if (referenceCatchHeading != null) {
            catchSequence = referenceCatchHeading.containingCatchSequence();
            catchSequence.addCoCatchUDBL(referenceCatchHeading, breakStepToLink);
          }
          else {
            catchSequence = sequenz.catchSequenzAnhaengenUDBL(breakStepToLink);
          }
          AbstractSchrittView firstInCatchSequence = catchSequence.getSchritte().get(0);
          editor.diagrammAktualisieren(firstInCatchSequence.getFirstEditArea());
        }
        CatchLinkDialog.this.setVisible(false);
      }
    }

    private class CustomListCellRenderer extends JLabel implements ListCellRenderer<AbstractSchrittView> {
        public CustomListCellRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList list, AbstractSchrittView step, int index, boolean isSelected, boolean cellHasFocus) {
            setText(StringUtils.left(step.toString(), MAXIMAL_REFERENCE_TEXTLENGTH));

            if (index == hoveredIndex) {
                setBackground(HOVERED_BACKGROUND);
            } else {
                setBackground(DEFAULT_BACKGROUND);
            }
            return this;
        }
    }
}
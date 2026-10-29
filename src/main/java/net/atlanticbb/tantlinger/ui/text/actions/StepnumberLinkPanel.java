package net.atlanticbb.tantlinger.ui.text.actions;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import net.atlanticbb.tantlinger.ui.UIUtils;
import org.apache.commons.lang.StringUtils;
import specman.editarea.TextEditArea;
import specman.i18n.IButton;
import specman.i18n.IHeaderPanel;
import specman.view.AbstractSchrittView;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.Serial;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static specman.Specman.editor;
import static specman.i18n.Messages.i18n;

/** The content of the {@link StepnumberLinkDialog}, which is shown when the user is about to insert a link
 * to a step number into a text. The panel offers all steps to select from, optionally narrowed down by a filter text.
 * Being a plain panel, it can be created and operated without displaying a dialog. */
public class StepnumberLinkPanel extends JPanel {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final String TITLE_KEY = "action.stepnumberlink";
    private static final Icon icon = UIUtils.getIcon("resources/images/x32/", "link3.png");
    private static final Color DEFAULT_BACKGROUND = Color.white;
    private static final Color HOVERED_BACKGROUND = Color.lightGray;
    private static final int MAXIMAL_REFERENCE_TEXTLENGTH = 60;

    private JTextComponent editor;
    private final List<AbstractSchrittView> steps;
    private final JList<AbstractSchrittView> stepsList = new JList<>();
    private final JTextField filter = new JTextField();
    private final JButton close = new IButton("button.close");
    private Runnable closeAction = () -> { };
    private int hoveredIndex = -1;

    /** Always create a new panel for a link to be inserted, since the steps are only fetched on creation. */
    public StepnumberLinkPanel(JTextComponent editor) {
        super(new FormLayout("0px:grow", "pref,pref,fill:pref:grow,pref"));
        this.editor = editor;
        this.steps = editor()
          .listAllSteps()
          .stream()
          .sorted(Comparator.comparing(AbstractSchrittView::getNumber))
          .toList();
        init();
    }

    public static String title() {
        return i18n(TITLE_KEY);
    }

    /** Defines what to do when the panel is done, i.e. after a step was chosen or the user closed the panel. */
    public void onClose(Runnable closeAction) {
        this.closeAction = closeAction;
    }

    public void setJTextComponent(JTextComponent editor) {
        this.editor = editor;
    }

    /** The steps currently offered for selection, in the order of the list. */
    public List<AbstractSchrittView> offeredSteps() {
        return List.copyOf(Collections.list(((DefaultListModel<AbstractSchrittView>) stepsList.getModel()).elements()));
    }

    /** Inserts a link to the given step into the last focused text, which is what the user does by clicking its entry in the list. */
    public void choose(AbstractSchrittView selectedStep) {
        if (editor == null) {
            return;
        }
        if (!editor.hasFocus()) {
            editor.requestFocusInWindow();
        }
        TextEditArea lastFocusedTextArea = editor().getLastFocusedTextArea();
        if (lastFocusedTextArea != null) {
            lastFocusedTextArea.addStepnumberLink(selectedStep);
            editor().diagrammAktualisieren(null);
        }
        closeAction.run();
    }

    /** Once the panel is part of a window, Enter closes it. */
    @Override
    public void addNotify() {
        super.addNotify();
        getRootPane().setDefaultButton(close);
    }

    private void init() {
        stepsList.setModel(stepListModel(steps));
        stepsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                hoveredIndex = -1;
                stepsList.repaint();
            }
        });
        stepsList.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Point position = e.getPoint();
                hoveredIndex = stepsList.locationToIndex(position);
                stepsList.repaint();
            }
        });
        stepsList.addListSelectionListener(this::listValueChanged);
        stepsList.setCellRenderer(new CustomListCellRenderer());
        stepsList.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 10, 10), new MatteBorder(1, 1, 1, 1, Color.black)));

        close.addActionListener(e -> closeAction.run());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(close);

        filter.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                stepsList.setModel(stepListModel(filterSteps(filter.getText())));
            }
        });

        add(new IHeaderPanel(TITLE_KEY, "dialog.stepnumberlink.description", icon), CC.xy(1, 1));
        add(filter, CC.xy(1, 2));
        add(new JScrollPane(stepsList), CC.xy(1, 3));
        add(buttonPanel, CC.xy(1, 4));
    }

    private List<AbstractSchrittView> filterSteps(String text) {
        if (text.isBlank()) {
            return steps;
        }
        return steps
          .stream()
          .filter(step -> step.toString().toLowerCase().contains(text.toLowerCase()))
          .toList();
    }

    private DefaultListModel<AbstractSchrittView> stepListModel(List<AbstractSchrittView> steps) {
        DefaultListModel<AbstractSchrittView> listModel = new DefaultListModel<>();
        listModel.addAll(steps);
        return listModel;
    }

    private void listValueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            choose(stepsList.getSelectedValue());
        }
    }

    private class CustomListCellRenderer extends JLabel implements ListCellRenderer<AbstractSchrittView> {
        public CustomListCellRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList list, AbstractSchrittView step, int index, boolean isSelected, boolean cellHasFocus) {
            setText(StringUtils.left(step.toString(), MAXIMAL_REFERENCE_TEXTLENGTH));
            setBackground(index == hoveredIndex ? HOVERED_BACKGROUND : DEFAULT_BACKGROUND);
            return this;
        }
    }
}

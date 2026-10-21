package specman;

import net.atlanticbb.tantlinger.ui.HeaderPanel;
import net.atlanticbb.tantlinger.ui.UIUtils;
import org.apache.commons.lang.StringUtils;
import specman.i18n.IButton;
import specman.undo.manager.UndoRecording;
import specman.view.AbstractSchrittView;
import specman.view.BreakSchrittView;
import specman.view.CatchSchrittSequenzView;
import specman.view.CatchUeberschrift;
import specman.view.SchrittSequenzView;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.ListSelectionEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.Serial;
import java.util.Collections;
import java.util.List;

import static specman.Specman.editor;
import static specman.i18n.Messages.i18n;

/** The content of the {@link CatchLinkDialog}, which is shown when the user is about to add a catch
 * sequence. Catch sequences have to be linked to a yet un-linked break steps within the same parent
 * sequence or any of its child sequences. The system provides an appropriate list here to select from.
 * Being a plain panel, it can be created and operated without displaying a dialog. */
public class CatchLinkPanel extends JPanel {
  @Serial
  private static final long serialVersionUID = 1L;
  private static final Icon icon = UIUtils.getIcon("resources/images/x32/", "link3.png");
  private static final Color DEFAULT_BACKGROUND = Color.white;
  private static final Color HOVERED_BACKGROUND = Color.lightGray;
  private static final int MAXIMAL_REFERENCE_TEXTLENGTH = 60;

  private final SchrittSequenzView sequenz;
  private final CatchUeberschrift referenceCatchHeading;
  private final DefaultListModel<BreakSchrittView> listModel = new DefaultListModel<>();
  private final JButton close = new IButton("button.close");
  private final CustomListCellRenderer customListCellRenderer = new CustomListCellRenderer();
  private Runnable closeAction = () -> { };
  private int hoveredIndex = -1;

  public CatchLinkPanel(SchrittSequenzView sequenz, CatchUeberschrift referenceCatchHeading) {
    super(new BorderLayout());
    this.sequenz = sequenz;
    this.referenceCatchHeading = referenceCatchHeading;
    init();
  }

  public static String title() {
    return i18n("dialog.catch.link.title");
  }

  /** Defines what to do when the panel is done, i.e. after a break step was chosen or the user closed the panel. */
  public void onClose(Runnable closeAction) {
    this.closeAction = closeAction;
  }

  /** The break steps currently offered for selection, in the order of the list. */
  public List<BreakSchrittView> offeredBreakSteps() {
    return List.copyOf(Collections.list(listModel.elements()));
  }

  /** Creates a catch sequence for the given break step, which is what the user does by clicking its entry in the list. */
  public void choose(BreakSchrittView breakStepToLink) {
    EditorI editor = editor();
    try (UndoRecording ur = editor.composeUndo()) {
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
    closeAction.run();
  }

  /** Once the panel is part of a window, Enter closes it. */
  @Override
  public void addNotify() {
    super.addNotify();
    getRootPane().setDefaultButton(close);
  }

  private void init() {
    listModel.addAll(sequenz.queryUnlinkedBreakSteps());
    JList<BreakSchrittView> jlist = new JList<>(listModel);
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
        hoveredIndex = jlist.locationToIndex(position);
        jlist.repaint();
      }
    });
    jlist.addListSelectionListener(this::listValueChanged);
    jlist.setCellRenderer(customListCellRenderer);
    jlist.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 10, 10, 10), new MatteBorder(1, 1, 1, 1, Color.black)));

    close.addActionListener(e -> closeAction.run());
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonPanel.add(close);
    add(new HeaderPanel(title(), i18n("dialog.catch.link.description"), icon), BorderLayout.NORTH);
    add(jlist, BorderLayout.CENTER);
    add(buttonPanel, BorderLayout.SOUTH);
  }

  private void listValueChanged(ListSelectionEvent e) {
    if (e.getValueIsAdjusting()) {
      choose(((JList<BreakSchrittView>) e.getSource()).getSelectedValue());
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

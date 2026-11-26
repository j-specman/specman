package specman.editarea.image;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import specman.i18n.IButton;
import specman.i18n.IFileNameExtensionFilter;
import specman.i18n.ILabel;
import specman.i18n.IRadioButton;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.io.File;

import static specman.i18n.Messages.i18n;

/** Asks for a file the source of a diagram is taken from, either as the initial source of a new diagram or to replace the
 * source of an existing one. For a new diagram the file is optional, so the dialog also offers to create an empty
 * diagram, with a button next to the ones to open and to cancel. Cancelling creates no diagram at all.
 * <p>
 * For a new diagram of a kind which can be linked, the user also says whether the diagram takes the content of the file
 * or links to the file, in a row of its own under the one of the file type. A diagram which links to a file has none to
 * start with, so the button for an empty diagram doesn't work then. */
public class SourceFileChooser extends JFileChooser {
  /** The distance of the row of the storage choice to the row above, like the look and feel puts between its rows. */
  private static final int ROW_GAP = 5;

  private final boolean emptyDiagramOffered;
  private final JButton emptyDiagramButton;
  private final JRadioButton linkedButton;
  private final JPanel storagePanel;
  private boolean emptyDiagramChosen;

  /** @param directory where the dialog starts, null for the user's default directory
   * @param emptyDiagramOffered whether the user may also choose to start without a file, which only makes sense
   *                            when creating a diagram, not when importing into an existing one */
  public SourceFileChooser(SourceFileKind kind, File directory, boolean emptyDiagramOffered) {
    super(directory);
    this.emptyDiagramOffered = emptyDiagramOffered;
    this.emptyDiagramButton = new IButton(kind.emptyButtonKey);
    setDialogTitle(i18n(emptyDiagramOffered ? kind.chooseTitleKey : kind.importTitleKey));
    setFileSelectionMode(JFileChooser.FILES_ONLY);
    IFileNameExtensionFilter filter = new IFileNameExtensionFilter(kind.filterMessageKey, kind.extensions);
    addChoosableFileFilter(filter);
    setFileFilter(filter);
    emptyDiagramButton.setToolTipText(i18n(kind.emptyButtonTooltipKey));
    emptyDiagramButton.addActionListener(e -> {
      emptyDiagramChosen = true;
      cancelSelection();
    });
    if (emptyDiagramOffered && kind.isLinkable()) {
      linkedButton = new IRadioButton("dialog.sourcefile.linked");
      storagePanel = buildStoragePanel(linkedButton);
    }
    else {
      linkedButton = null;
      storagePanel = null;
    }
  }

  private JPanel buildStoragePanel(JRadioButton linked) {
    JRadioButton embedded = new IRadioButton("dialog.sourcefile.embedded", true);
    embedded.setToolTipText(i18n("dialog.sourcefile.embedded.tooltip"));
    linked.setToolTipText(i18n("dialog.sourcefile.linked.tooltip"));
    ButtonGroup group = new ButtonGroup();
    group.add(embedded);
    group.add(linked);
    linked.addItemListener(e -> emptyDiagramButton.setEnabled(!linked.isSelected()));
    JPanel panel = new JPanel(new FormLayout("default, 8px, default, 8px, default, 0px:grow", "pref"));
    panel.add(new ILabel("dialog.sourcefile.storage"), CC.xy(1, 1));
    panel.add(embedded, CC.xy(3, 1));
    panel.add(linked, CC.xy(5, 1));
    panel.setBorder(BorderFactory.createEmptyBorder(ROW_GAP, 0, 0, 0));
    panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
    return panel;
  }

  /** @return true if the user chose to link the file instead of taking its content, which is only possible for a
   * new diagram of a kind which can be linked */
  boolean isLinkedSelected() {
    return linkedButton != null && linkedButton.isSelected();
  }

  boolean isEmptyDiagramButtonEnabled() {
    return emptyDiagramButton.isEnabled();
  }

  /** @return the row with the choice between embedding and linking, null if the dialog doesn't offer it */
  JPanel storagePanel() {
    return storagePanel;
  }

  @Override
  protected JDialog createDialog(Component parent) throws HeadlessException {
    JDialog dialog = super.createDialog(parent);
    Container contentPane = dialog.getContentPane();
    JButton cancelButton = findButton(contentPane, UIManager.getString("FileChooser.cancelButtonText", getLocale()));
    if (emptyDiagramOffered) {
      addEmptyDiagramButton(contentPane, cancelButton);
    }
    if (storagePanel != null) {
      addStoragePanel(contentPane, cancelButton);
    }
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    return dialog;
  }

  /** The look and feel owns the dialog's buttons, so the new button is put in front of the one to cancel, which
   * is found by its text. Should the look and feel's dialog be built differently, the button gets a row of its own. */
  private void addEmptyDiagramButton(Container contentPane, JButton cancelButton) {
    if (cancelButton != null) {
      Container buttonPanel = cancelButton.getParent();
      buttonPanel.add(emptyDiagramButton, indexOf(buttonPanel, cancelButton));
    }
    else {
      JPanel ownRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      ownRow.add(emptyDiagramButton);
      contentPane.add(ownRow, BorderLayout.SOUTH);
    }
  }

  /** The row goes under the one of the file type and above the buttons, which is in front of the panel of the buttons
   * in the panel which holds the rows. Should the look and feel's dialog be built differently, it is put above the
   * chooser. */
  private void addStoragePanel(Container contentPane, JButton cancelButton) {
    if (cancelButton != null) {
      Container buttonPanel = cancelButton.getParent();
      Container rows = buttonPanel.getParent();
      rows.add(storagePanel, indexOf(rows, buttonPanel));
    }
    else {
      contentPane.add(storagePanel, BorderLayout.NORTH);
    }
  }

  private static JButton findButton(Container container, String text) {
    for (Component component : container.getComponents()) {
      if (component instanceof JButton button && button.getText() != null && button.getText().equals(text)) {
        return button;
      }
      if (component instanceof Container child) {
        JButton found = findButton(child, text);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static int indexOf(Container container, Component component) {
    Component[] components = container.getComponents();
    for (int i = 0; i < components.length; i++) {
      if (components[i] == component) {
        return i;
      }
    }
    return -1;
  }

  public SourceFileChoice choose(Component parent) {
    int result = showOpenDialog(parent);
    if (emptyDiagramChosen) {
      return SourceFileChoice.emptyDiagram();
    }
    if (result == JFileChooser.APPROVE_OPTION && getSelectedFile() != null) {
      return isLinkedSelected() ? SourceFileChoice.linkedFile(getSelectedFile()) : SourceFileChoice.file(getSelectedFile());
    }
    return SourceFileChoice.cancelled();
  }
}

package specman.editarea.plantuml;

import specman.i18n.IButton;
import specman.i18n.IFileNameExtensionFilter;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.io.File;

import static specman.i18n.Messages.i18n;

/** Asks for a file PlantUML source is taken from, either as the initial source of a new diagram or to replace the
 * source of an existing one. For a new diagram the file is optional, so the dialog also offers to create an empty
 * diagram, with a button next to the ones to open and to cancel. Cancelling creates no diagram at all. */
public class PlantUmlFileChooser extends JFileChooser {
  /** The extensions PlantUML files commonly have. "wsd" is a heritage of WebSequenceDiagrams, which PlantUML tools
   * took over. "iuml" is missing on purpose: Include files have no @startuml and can't be a diagram of their own.
   * The filter is only a preselection, the user can still choose any file. */
  private static final String[] PLANTUML_EXTENSIONS = {"puml", "plantuml", "pu", "wsd", "txt"};

  private final boolean emptyDiagramOffered;
  private final JButton emptyDiagramButton = new IButton("dialog.plantuml.choose.empty");
  private boolean emptyDiagramChosen;

  /** @param directory where the dialog starts, null for the user's default directory
   * @param emptyDiagramOffered whether the user may also choose to start without a file, which only makes sense
   *                            when creating a diagram, not when importing into an existing one */
  public PlantUmlFileChooser(File directory, boolean emptyDiagramOffered) {
    super(directory);
    this.emptyDiagramOffered = emptyDiagramOffered;
    setDialogTitle(i18n(emptyDiagramOffered ? "dialog.plantuml.choose.title" : "dialog.plantuml.import.title"));
    setFileSelectionMode(JFileChooser.FILES_ONLY);
    IFileNameExtensionFilter plantUmlFilter = new IFileNameExtensionFilter("msg.filter.plantuml", PLANTUML_EXTENSIONS);
    addChoosableFileFilter(plantUmlFilter);
    setFileFilter(plantUmlFilter);
    emptyDiagramButton.setToolTipText(i18n("dialog.plantuml.choose.empty.tooltip"));
    emptyDiagramButton.addActionListener(e -> {
      emptyDiagramChosen = true;
      cancelSelection();
    });
  }

  @Override
  protected JDialog createDialog(Component parent) throws HeadlessException {
    JDialog dialog = super.createDialog(parent);
    if (emptyDiagramOffered) {
      addEmptyDiagramButton(dialog.getContentPane());
    }
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    return dialog;
  }

  /** The look and feel owns the dialog's buttons, so the new button is put in front of the one to cancel, which
   * is found by its text. Should the look and feel's dialog be built differently, the button gets a row of its own. */
  private void addEmptyDiagramButton(Container contentPane) {
    JButton cancelButton = findButton(contentPane, UIManager.getString("FileChooser.cancelButtonText", getLocale()));
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

  public PlantUmlFileChoice choose(Component parent) {
    int result = showOpenDialog(parent);
    if (emptyDiagramChosen) {
      return PlantUmlFileChoice.emptyDiagram();
    }
    if (result == JFileChooser.APPROVE_OPTION && getSelectedFile() != null) {
      return PlantUmlFileChoice.file(getSelectedFile());
    }
    return PlantUmlFileChoice.cancelled();
  }
}

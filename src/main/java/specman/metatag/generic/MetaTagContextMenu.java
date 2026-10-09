package specman.metatag.generic;

import specman.graphics.IconReader;
import specman.undo.UndoableMetaTagRemoved;
import specman.view.AbstractSchrittView;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static specman.Specman.editor;

/** Context menu of a {@link GenericMetaTag} at a step, same pattern as the one of the step
 * number: one shared instance, registered as mouse listener of every tag. */
class MetaTagContextMenu implements MouseListener {
  static final MetaTagContextMenu instance = new MetaTagContextMenu();

  private final JPopupMenu popup = new JPopupMenu();
  private GenericMetaTag currentTag;
  private AbstractSchrittView currentStep;

  private MetaTagContextMenu() {
    JMenuItem edit = new JMenuItem("Edit");
    edit.addActionListener(e -> currentTag.editFreetext());
    JMenuItem delete = new JMenuItem("Delete", IconReader.readImageIcon("loeschen"));
    delete.addActionListener(e -> deleteCurrentTag());
    popup.add(edit);
    popup.add(delete);
  }

  private void deleteCurrentTag() {
    int index = currentStep.removeTag(currentTag);
    editor().addEdit(new UndoableMetaTagRemoved(currentStep, currentTag, index));
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (SwingUtilities.isRightMouseButton(e)) {
      GenericMetaTag tag = (GenericMetaTag) e.getComponent();
      AbstractSchrittView step = editor().findStep(tag);
      // A tag outside of a step, like the one in the preview of the configuration dialog, has no menu.
      if (step != null) {
        currentTag = tag;
        currentStep = step;
        popup.show(e.getComponent(), e.getX(), e.getY());
      }
    }
  }

  @Override public void mousePressed(MouseEvent e) {}
  @Override public void mouseReleased(MouseEvent e) {}
  @Override public void mouseEntered(MouseEvent e) {}
  @Override public void mouseExited(MouseEvent e) {}
}

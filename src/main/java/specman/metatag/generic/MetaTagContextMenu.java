package specman.metatag.generic;

import specman.graphics.IconReader;
import specman.i18n.IMenuItem;
import specman.metatag.TagMoveDirection;
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
  private final JMenuItem edit = new IMenuItem("menu.context.edit");
  private final JMenuItem left = new IMenuItem("menu.context.move.left", IconReader.readImageIcon("arrow-left"));
  private final JMenuItem right = new IMenuItem("menu.context.move.right", IconReader.readImageIcon("arrow-right"));
  private GenericMetaTag currentTag;
  private AbstractSchrittView currentStep;

  private MetaTagContextMenu() {
    edit.addActionListener(e -> currentTag.editFreetext());
    left.addActionListener(e -> moveCurrentTag(TagMoveDirection.LEFT));
    right.addActionListener(e -> moveCurrentTag(TagMoveDirection.RIGHT));
    JMenuItem delete = new IMenuItem("menu.context.delete", IconReader.readImageIcon("loeschen"));
    delete.addActionListener(e -> deleteCurrentTag());
    popup.add(edit);
    popup.add(left);
    popup.add(right);
    popup.add(delete);
  }

  private void moveCurrentTag(TagMoveDirection direction) {
    currentStep.moveTagUDBL(currentTag, direction);
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
      // A tag outside of a step, like the one in the preview of the configuration dialog, has no menu,
      // and neither has a tag at a deleted step, which can't be changed.
      if (step != null && !tag.isDeleted()) {
        currentTag = tag;
        currentStep = step;
        edit.setVisible(tag.getConfig().allowsFreetext());
        boolean leftAllowed = step.allowsTagMove(tag, TagMoveDirection.LEFT);
        boolean rightAllowed = step.allowsTagMove(tag, TagMoveDirection.RIGHT);
        left.setVisible(leftAllowed || rightAllowed);
        right.setVisible(leftAllowed || rightAllowed);
        left.setEnabled(leftAllowed);
        right.setEnabled(rightAllowed);
        popup.show(e.getComponent(), e.getX(), e.getY());
      }
    }
  }

  @Override public void mousePressed(MouseEvent e) {}
  @Override public void mouseReleased(MouseEvent e) {}
  @Override public void mouseEntered(MouseEvent e) {}
  @Override public void mouseExited(MouseEvent e) {}
}

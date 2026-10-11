package specman.metatag.generic;

import specman.metatag.MetaTagPanel;
import specman.metatag.TagMoveDirection;
import specman.undo.UndoableMetaTagMoved;
import specman.undo.manager.UndoRecording;
import specman.view.AbstractSchrittView;

import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static specman.Specman.editor;

/** Reorders the tags of a step by dragging one of them horizontally, wherever the mouse is. All
 * moves of one drag become a single undo step on release. */
class MetaTagDragReorder extends MouseAdapter {
  private final GenericMetaTag tag;
  private AbstractSchrittView step;
  private final List<TagMoveDirection> moves = new ArrayList<>();
  private boolean dragging;
  private boolean reordered;

  MetaTagDragReorder(GenericMetaTag tag) {
    this.tag = tag;
  }

  /** @return whether the last mouse gesture on the tag moved it, so its release is not to be taken as click */
  boolean hasReordered() {
    return reordered;
  }

  @Override
  public void mousePressed(MouseEvent e) {
    reordered = false;
    moves.clear();
    step = null;
    if (SwingUtilities.isLeftMouseButton(e) && !tag.isDeleted()) {
      step = editor().findStep(tag);
    }
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    if (step == null || !(tag.getParent() instanceof MetaTagPanel)) {
      return;
    }
    MetaTagPanel panel = (MetaTagPanel) tag.getParent();
    Point p = SwingUtilities.convertPoint(tag, e.getPoint(), panel);
    TagMoveDirection direction;
    while ((direction = panel.directionTowards(tag, p.x)) != null) {
      step.moveTag(tag, direction);
      recordMove(direction);
      reordered = true;
    }
    dragging = true;
    editor().vertikalLinieSetzen(p.x - tag.getX(), tag);
  }

  private void hideDragLine() {
    if (dragging) {
      dragging = false;
      editor().vertikalLinieSetzen(0, null);
    }
  }

  /** Keeps only the net moves: a move against the last one just cancels it. */
  private void recordMove(TagMoveDirection direction) {
    int last = moves.size() - 1;
    if (last >= 0 && moves.get(last) == direction.opposite()) {
      moves.remove(last);
    } else {
      moves.add(direction);
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    hideDragLine();
    if (step != null && !moves.isEmpty()) {
      try (UndoRecording ur = editor().composeUndo()) {
        for (TagMoveDirection direction : moves) {
          editor().addEdit(new UndoableMetaTagMoved(step, tag, direction));
        }
      }
    }
    moves.clear();
    step = null;
  }
}

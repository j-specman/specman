package specman.ops.buttons;

import org.jetbrains.annotations.NotNull;
import specman.ChangeSet;
import specman.EditException;
import specman.menubar.ChangeSetDotIcon;
import specman.ops.SpecmanOpContext;
import specman.undo.manager.UndoRecording;
import specman.visitor.ChangeSetMerger;

import javax.swing.*;
import java.awt.*;

public class MergeChangeSetsADBLOpButton extends AbstractSpecmanOpButton {

  public MergeChangeSetsADBLOpButton(SpecmanOpContext context) {
    super(context);
  }

  @Override
  void execute() throws EditException {
    JPopupMenu popup = new JPopupMenu();
    ChangeSet currentSet = ChangeSet.changeset();
    for (ChangeSet cs : ChangeSet.ALL.values()) {
      if (cs == currentSet) {
        continue;
      }
      JMenuItem item = createSourceChangeSetIcon(cs);
      popup.add(item);
    }
    popup.show(this, 0, getHeight());
  }

  private @NotNull JMenuItem createSourceChangeSetIcon(ChangeSet cs) {
    JMenuItem item = new JMenuItem(cs.name, new ChangeSetDotIcon(cs));
    item.setBackground(cs.panelColor());
    item.setForeground(Color.BLACK);
    item.setOpaque(true);
    item.addActionListener(e -> mergeChangeSets(cs));
    return item;
  }

  /** Makes all changes of the given change set changes of the current change set. */
  public void mergeChangeSets(ChangeSet source) {
    try (UndoRecording ur = composeUndo()) {
      ChangeSet targetSet = ChangeSet.changeset();
      context().traverseDiagram(new ChangeSetMerger(targetSet, source, true));
      diagrammAktualisieren(null);
    }
  }

}

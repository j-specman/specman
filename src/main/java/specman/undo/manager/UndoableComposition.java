package specman.undo.manager;

import specman.EditException;
import specman.Specman;
import specman.undo.AbstractUndoableInteraction;
import static specman.Specman.editor;

import javax.swing.undo.UndoableEdit;
import java.util.ArrayList;
import java.util.List;

public class UndoableComposition extends AbstractUndoableInteraction {
  private List<UndoableEdit> interactions = new ArrayList();

  public void add(UndoableEdit interaction) {
    interactions.add(interaction);
  }

  @Override
  protected void undoEdit() throws EditException {
    try (UndoRecording ur = editor().pauseUndo()) {
      for (int i = interactions.size()-1; i >= 0; i-- ) {
        interactions.get(i).undo();
      }
    }
  }

  @Override
  protected void redoEdit() throws EditException {
    try (UndoRecording ur = editor().pauseUndo()) {
      interactions.forEach(i -> i.redo());
    }
  }
}

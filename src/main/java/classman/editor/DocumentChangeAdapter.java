package classman.editor;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Runs something after every change of a document. */
class DocumentChangeAdapter implements DocumentListener {
  private final Runnable changed;

  DocumentChangeAdapter(Runnable changed) {
    this.changed = changed;
  }

  @Override
  public void insertUpdate(DocumentEvent e) {
    changed.run();
  }

  @Override
  public void removeUpdate(DocumentEvent e) {
    changed.run();
  }

  @Override
  public void changedUpdate(DocumentEvent e) {
    changed.run();
  }
}

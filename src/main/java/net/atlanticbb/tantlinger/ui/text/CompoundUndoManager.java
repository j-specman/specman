package net.atlanticbb.tantlinger.ui.text;

import net.atlanticbb.tantlinger.i18n.I18n;
import net.atlanticbb.tantlinger.ui.UIUtils;

import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.util.Vector;

public class CompoundUndoManager implements UndoableEditListener {
  private static final I18n i18n = I18n.getInstance("net.atlanticbb.tantlinger.ui.text");
  public static Action UNDO = new CompoundUndoManager.UndoAction();
  public static Action REDO = new CompoundUndoManager.RedoAction();
  private UndoManager undoer;
  private CompoundEdit compoundEdit;
  private Document document;
  private static Vector docs = new Vector();
  private static Vector lsts = new Vector();
  private static Vector undoers = new Vector();
  private static UndoManager centralUndoer;

  public static void setCentralUndoManager(UndoManager centralUndoManager) {
    centralUndoer = centralUndoManager;
  }

  protected static void registerDocument(Document doc, CompoundUndoManager lst, UndoManager um) {
    docs.add(doc);
    lsts.add(lst);
    undoers.add(um);
  }

  public static UndoManager getUndoManagerForDocument(Document doc) {
    for(int i = 0; i < docs.size(); ++i) {
      if (docs.elementAt(i) == doc) {
        return (UndoManager)undoers.elementAt(i);
      }
    }

    return centralUndoer;
  }

  public static void beginCompoundEdit(Document doc) {
    for(int i = 0; i < docs.size(); ++i) {
      if (docs.elementAt(i) == doc) {
        CompoundUndoManager l = (CompoundUndoManager)lsts.elementAt(i);
        l.beginCompoundEdit();
        return;
      }
    }

  }

  public static void endCompoundEdit(Document doc) {
    for(int i = 0; i < docs.size(); ++i) {
      if (docs.elementAt(i) == doc) {
        CompoundUndoManager l = (CompoundUndoManager)lsts.elementAt(i);
        l.endCompoundEdit();
        return;
      }
    }

  }

  public static void updateUndo(Document doc) {
    UndoManager um = getUndoManagerForDocument(doc);
    if (um != null) {
      UNDO.setEnabled(um.canUndo());
      REDO.setEnabled(um.canRedo());
    }

  }

  /** Brings the enabled state of {@link #UNDO} and {@link #REDO} in line with the central undo
   * manager, for changes of its stack which don't originate from a text document, e.g. tag moves. */
  public static void updateCentralUndo() {
    if (centralUndoer != null) {
      UNDO.setEnabled(centralUndoer.canUndo());
      REDO.setEnabled(centralUndoer.canRedo());
    }
  }

  public static void discardAllEdits(Document doc) {
    UndoManager um = getUndoManagerForDocument(doc);
    if (um != null) {
      um.discardAllEdits();
      UNDO.setEnabled(um.canUndo());
      REDO.setEnabled(um.canRedo());
    }

  }

  public CompoundUndoManager(Document doc, UndoManager um) {
    this.compoundEdit = null;
    this.document = null;
    this.undoer = um;
    this.document = doc;
    registerDocument(this.document, this, this.undoer);
  }

  public CompoundUndoManager(Document doc) {
    this(doc, new UndoManager());
  }

  public void undoableEditHappened(UndoableEditEvent evt) {
    UndoableEdit edit = evt.getEdit();
    if (this.compoundEdit != null) {
      this.compoundEdit.addEdit(edit);
    } else {
      this.undoer.addEdit(edit);
      updateUndo(this.document);
    }

  }

  protected void beginCompoundEdit() {
    this.compoundEdit = new CompoundEdit();
  }

  protected void endCompoundEdit() {
    if (this.compoundEdit != null) {
      this.compoundEdit.end();
      this.undoer.addEdit(this.compoundEdit);
      updateUndo(this.document);
    }

    this.compoundEdit = null;
  }

  static class RedoAction extends TextAction {
    @Serial private static final long serialVersionUID = 1L;

    public RedoAction() {
      super(CompoundUndoManager.i18n.str("redo"));
      this.putValue("SmallIcon", UIUtils.getIcon("resources/images/x16/", "redo.png"));
      this.putValue("LARGE_ICON", UIUtils.getIcon("resources/images/x24/", "redo.png"));
      this.putValue("MnemonicKey", (int) CompoundUndoManager.i18n.mnem("redo"));
      this.setEnabled(false);
      this.putValue("AcceleratorKey", KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
      this.putValue("ShortDescription", this.getValue("Name"));
    }

    public void actionPerformed(ActionEvent e) {
      UndoManager um;
      Document doc = null;
      JTextComponent textComponent = this.getTextComponent(e);
      if (textComponent != null) {
        doc = this.getTextComponent(e).getDocument();
      }
      um = CompoundUndoManager.getUndoManagerForDocument(doc);
      if (um != null) {
        try {
          um.redo();
          CompoundUndoManager.updateUndo(doc);
        } catch (CannotUndoException var5) {
          System.out.println("Unable to redo: " + var5);
          var5.printStackTrace();
        }
      }

    }
  }

  static class UndoAction extends TextAction {
    @Serial private static final long serialVersionUID = 1L;

    public UndoAction() {
      super(CompoundUndoManager.i18n.str("undo"));
      this.putValue("SmallIcon", UIUtils.getIcon("resources/images/x16/", "undo.png"));
      this.putValue("LARGE_ICON", UIUtils.getIcon("resources/images/x24/", "undo.png"));
      this.putValue("MnemonicKey", (int) CompoundUndoManager.i18n.mnem("undo"));
      this.setEnabled(false);
      this.putValue("AcceleratorKey", KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
      this.putValue("ShortDescription", this.getValue("Name"));
    }

    public void actionPerformed(ActionEvent e) {
      UndoManager um;
      Document doc = null;
      JTextComponent textComponent = this.getTextComponent(e);
      if (textComponent != null) {
        doc = this.getTextComponent(e).getDocument();
      }
      um = CompoundUndoManager.getUndoManagerForDocument(doc);
      if (um != null) {
        try {
          um.undo();
          CompoundUndoManager.updateUndo(doc);
        } catch (CannotUndoException var5) {
          System.out.println("Unable to undo: " + var5);
          var5.printStackTrace();
        }
      }

    }
  }
}
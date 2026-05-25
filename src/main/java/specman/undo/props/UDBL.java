package specman.undo.props;

import specman.Aenderungsart;
import specman.ChangeInfo;
import specman.Specman;
import specman.editarea.EditArea;
import specman.editarea.ImageEditArea;
import specman.editarea.EditArea;
import specman.editarea.stepnumberlabel.StepnumberLabel;
import specman.editarea.stepnumberlabel.StepnumberLabel.LabelStructure;
import specman.undo.AbstractUndoableInteraction;
import specman.view.AbstractSchrittView;
import specman.view.QuellSchrittView;
import specman.view.SchrittSequenzView;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.Objects;

/** Wrapper class to perform typical property changes in an undoable way
 * by just one line. The class name is very short to make these lines become
 * readable. Usually there are the same method names available within the
 * calling classes itself and in this case, the methods in here can't be
 * imported statically. So the class name must often be used as qualifier.
 *
 * The class sets attributes and records undo actions only in case the
 * attribute values actually change. This avoids overcrowding the undo
 * history with useless entries. E.g. adding or removing steps causes a
 * resync of step number labels of all other steps where most of the
 * labels don't change at all.
 */
public class UDBL {
  private static void addEdit(AbstractUndoableInteraction action) {
    Specman.instance().addEdit(action);
  }

  public static void setBackgroundUDBL(Component component, Color bg) {
    Color undoBackground = component.getBackground();
    if (!Objects.equals(undoBackground, bg)) {
      component.setBackground(bg);
      addEdit(new UndoableSetBackground(component, undoBackground));
    }
  }

  public static void setForegroundUDBL(Component component, Color bg) {
    Color undoForeground = component.getForeground();
    if (!Objects.equals(undoForeground, bg)) {
      component.setForeground(bg);
      addEdit(new UndoableSetForeground(component, undoForeground));
    }
  }

  public static void setBorderUDBL(JComponent component, Border border) {
    Border undoBorder = component.getBorder();
    if (!Objects.equals(undoBorder, border)) {
      component.setBorder(border);
      addEdit(new UndoableSetBorder(component, undoBorder));
    }
  }

  public static void setTextUDBL(JLabel label, String text) {
    String undoText = label.getText();
    if (!Objects.equals(undoText, text)) {
      label.setText(text);
      addEdit(new UndoableSetText(label, undoText));
    }
  }

  public static void setAenderungsart(AbstractSchrittView abstractSchrittView, Aenderungsart aenderungsart) {
    Aenderungsart undoAenderungsart = abstractSchrittView.getAenderungsart();
    if (!Objects.equals(undoAenderungsart, aenderungsart)) {
      abstractSchrittView.setAenderungsart(aenderungsart);
      addEdit(new UndoableSetAenderungsart(abstractSchrittView, undoAenderungsart));
    }
  }

  public static void setAenderungsart(SchrittSequenzView schrittSequenzView, Aenderungsart aenderungsart) {
    Aenderungsart undoAenderungsart = schrittSequenzView.getAenderungsart();
    if (!Objects.equals(undoAenderungsart, aenderungsart)) {
      schrittSequenzView.setAenderungsart(aenderungsart);
      addEdit(new UndoableSetAenderungsartSchrittSequenzView(schrittSequenzView, undoAenderungsart));
    }
  }

  public static void setAenderungsart(EditArea editArea, Aenderungsart aenderungsart) {
    Aenderungsart undoAenderungsart = editArea.getAenderungsart();
    if (!Objects.equals(undoAenderungsart, aenderungsart)) {
      editArea.setAenderungsart(aenderungsart);
      addEdit(new UndoableSetAenderungsartEditArea(editArea, undoAenderungsart));
    }
  }

  public static void setChangeInfo(EditArea editArea, ChangeInfo changeInfo) {
    ChangeInfo undoChangeInfo = editArea.getChangeInfo();
    if (!Objects.equals(undoChangeInfo, changeInfo)) {
      editArea.setChangeInfo(changeInfo);
      addEdit(new UndoableSetChangeInfoEditArea(editArea, undoChangeInfo));
    }
  }

  public static void setEditable(JTextComponent component, boolean editable) {
    boolean undoEditable = component.isEditable();
    if (undoEditable != editable) {
      component.setEditable(editable);
      addEdit(new UndoableSetEditable(component, undoEditable));
    }
  }

  public static void setStructureUDBL(StepnumberLabel label, LabelStructure structure) {
    LabelStructure undoStructure = label.getStructure();
    if (!Objects.equals(undoStructure, structure)) {
      label.setStructure(structure);
      addEdit(new UndoableSetLabelStructure(label, undoStructure));
    }
  }

  public static void setQuellschrittUDBL(AbstractSchrittView abstractSchrittView, QuellSchrittView quellschritt) {
    QuellSchrittView undoQuellschritt = abstractSchrittView.getQuellschritt();
    if (!Objects.equals(undoQuellschritt, quellschritt)) {
      abstractSchrittView.setQuellschritt(quellschritt);
      addEdit((new UndoableSetQuellschritt(abstractSchrittView, undoQuellschritt)));
    }
  }

  public static void setZielschrittUDBL(QuellSchrittView quellSchrittView, AbstractSchrittView zielschritt) {
    AbstractSchrittView undoZielschritt = quellSchrittView.getZielschritt();
    if (!Objects.equals(undoZielschritt, zielschritt)) {
      quellSchrittView.setZielschritt(zielschritt);
      addEdit((new UndoableSetZielschritt(quellSchrittView, undoZielschritt)));
    }
  }

  public static void repaint(Component component) {
    component.repaint();
    addEdit(new UndoableRepaint(component));
  }

}

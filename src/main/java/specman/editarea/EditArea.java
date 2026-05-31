package specman.editarea;

import specman.ChangeInfo;
import specman.ChangeSet;
import specman.editarea.stepnumberlabel.StepnumberLabel;
import specman.model.v001.AbstractEditAreaModel_V001;
import specman.pdf.Shape;

import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.FocusListener;
import java.util.HashMap;
import java.util.List;

public interface EditArea<MODEL extends AbstractEditAreaModel_V001> extends InteractiveStepFragment {
  void addSchrittnummer(StepnumberLabel schrittNummer);

  int getWidth();

  /** The passed ChangeSet is the one which the deletion has been triggered with. The EditArea
   * might already have a change set assigned e.g. which caused the area to be created. The
   * deletion trigger change set rules over the existing one. */
  void setGeloeschtMarkiertStilUDBL(ChangeSet triggerSet);

  Component asComponent();

  MODEL toModel(boolean formatierterText);

  String getPlainText();

  void skalieren(int prozentNeu, int prozentAktuell);

  void addFocusListener(FocusListener focusListener);

  void requestFocus();

  void setBorder(Border editorPaneBorder);

  void setOpaque(boolean isOpaque);

  Color getBackground();

  int aenderungenUebernehmen();

  int aenderungenVerwerfen();

  TextEditArea asTextArea();

  default boolean isTextArea() { return false; }

  default boolean isOrderedListItemArea() { return false; }
  ImageEditArea asImageArea();

  void setQuellStil(ChangeSet changeSet);

  void aenderungsmarkierungenEntfernen();

  void mergeChangeSetUDBL(ChangeSet target, ChangeSet source, boolean withMarkups);

  boolean enthaeltAenderungsmarkierungen();

  void findStepnumberLinkIDs(HashMap<TextEditArea, List<String>> stepnumberLinkMap);

  Shape getShape();

  void setEditBackgroundUDBL(Color bg);

  void setEditDecorationIndentions(Indentions indentions);

  boolean enthaelt(InteractiveStepFragment fragment);

  void setChangeInfo(ChangeInfo changeInfo);

  ChangeInfo getChangeInfo();

  EditContainer getParent();

  default boolean isListItemArea() { return false; }

  default boolean isTableEditArea() { return false; }

  default boolean isImageEditArea() { return false; }

  default AbstractListItemEditArea asListItemArea() { return null; };

  List<JTextComponent> getTextAreas();

  void viewsNachinitialisieren();
}
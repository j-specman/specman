package specman.editarea;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import specman.ChangeInfo;
import specman.EditorI;
import specman.Specman;
import specman.editarea.document.WrappedPosition;
import specman.editarea.stepnumberlabel.StepnumberLabel;
import specman.model.v001.ChangeInfo_V001;
import specman.model.v001.EditorContentModel_V001;
import specman.model.v001.ListItemEditAreaModel_V001;
import specman.undo.UndoableListItemSplitted;
import specman.undo.manager.UndoRecording;
import specman.undo.props.UDBL;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ComponentListener;
import java.awt.event.FocusListener;
import java.util.HashMap;
import java.util.List;

import static specman.ChangeInfo.fromModel;

abstract public class AbstractListItemEditArea extends JPanel implements EditArea<ListItemEditAreaModel_V001> {
  static final int DEFAULT_PROMPT_SPACE = 20;
  protected EditContainer content;
  protected ChangeInfo changeInfo;
  protected int promptSpace;
  protected JPanel itemPrompt;
  protected FormLayout layout;

  public AbstractListItemEditArea(TextEditArea initialContent, ChangeInfo changeInfo) {
    this.changeInfo = changeInfo;
    this.content = new EditContainer(Specman.instance(), initialContent, null);
    initLayout();
  }

  public AbstractListItemEditArea(ListItemEditAreaModel_V001 model) {
    this.changeInfo = fromModel(model.changeInfo, model.aenderungsart);
    this.content = new EditContainer(Specman.instance(), model.content, null);
    initLayout();
  }

  protected void initLayout() {
    this.promptSpace = DEFAULT_PROMPT_SPACE * Specman.instance().getZoomFactor() / 100;

    layout = new FormLayout(promptSpace + "px, default:grow", "fill:pref:grow");
    setLayout(layout);
    add(content, CC.xy(2, 1));

    this.itemPrompt = new JPanel() {
      @Override
      public void paint(Graphics g) {
        super.paint(g);
        drawPrompt((Graphics2D)g);
      }
    };
    this.itemPrompt.setOpaque(true);
    this.setBackground(changeInfo.panelColor());

    add(itemPrompt, CC.xy(1, 1));
  }

  @Override
  public void setBackground(Color bg) {
    super.setBackground(bg);
    if (itemPrompt != null) { // is null on look&feel defaults init
      itemPrompt.setBackground(bg);
    }
    if (content != null) {
      content.setBackground(bg);
    }
  }

  protected abstract void drawPrompt(Graphics2D g);

  @Override
  public void setGeloeschtMarkiertStilUDBL() {
    setBackgroundUDBL(changeInfo.changeSet().panelColor());
    content.setGeloeschtMarkiertStilUDBL(null);
  }

  @Override
  public ListItemEditAreaModel_V001 toModel(boolean formatierterText) {
    EditorContentModel_V001 contentModel = content.editorContent2Model(formatierterText);
    return new ListItemEditAreaModel_V001(contentModel, ordered(), new ChangeInfo_V001(changeInfo));
  }

  abstract protected boolean ordered();

  @Override
  public void skalieren(int prozentNeu, int prozentAktuell) {
    content.skalieren(prozentNeu, prozentAktuell);
    promptSpace = DEFAULT_PROMPT_SPACE * prozentNeu / 100;
    layout.setColumnSpec(1, ColumnSpec.decode(promptSpace + "px"));
  }

  @Override
  public int aenderungenUebernehmen() {
    int changesMade = changeInfo.numChanges();
    if (changeInfo.isDeleted()) {
      getParent().removeEditAreaUDBL(this);
      changesMade++;
    }
    else {
      changesMade += content.aenderungenUebernehmen();
    }
    aenderungsmarkierungenEntfernen();
    changeInfo = ChangeInfo.untracked();
    return changesMade;
  }

  @Override
  public int aenderungenVerwerfen() {
    int changesMade = changeInfo.numChanges();
    if (changeInfo.isAdded()) {
      getParent().removeEditAreaUDBL(this);
      changesMade++;
    }
    else {
      changesMade += content.aenderungenVerwerfen();
    }
    aenderungsmarkierungenEntfernen();
    changeInfo = ChangeInfo.untracked();
    return changesMade;
  }

  @Override
  public specman.pdf.Shape getShape() {
    return new specman.pdf.Shape(this)
      .add(content.getShape());
  }

  @Override
  public void setEditBackgroundUDBL(Color bg) {
    content.setBackgroundUDBL(changeInfo.panelColor());
    setBackgroundUDBL(bg);
  }

  private void setBackgroundUDBL(Color bg) {
    UDBL.setBackgroundUDBL(this, bg);
  }

  public EditContainer getContent() {
    return content;
  }

  abstract protected AbstractListItemEditArea createSplittedItem(TextEditArea splitTextEditArea);

  private void addEditAreas(List<EditArea> areas) {
    content.addEditAreas(areas);
  }

  public void split(TextEditArea initiatingEditArea) {
    EditorI editor = Specman.instance();
    try (UndoRecording ur = editor.composeUndo()) {
      WrappedPosition initiatingCaretPosition = initiatingEditArea.getWrappedCaretPosition();
      TextEditArea splitTextEditArea = initiatingEditArea.split(initiatingCaretPosition);
      if (splitTextEditArea == null) {
        splitTextEditArea = new TextEditArea(initiatingEditArea.getFont());
      }
      AbstractListItemEditArea splitListItemEditArea = createSplittedItem(splitTextEditArea);
      moveEditAreas(initiatingEditArea, splitListItemEditArea);
      editor.addEdit(new UndoableListItemSplitted(this, initiatingEditArea, splitListItemEditArea));
      editor.diagrammAktualisieren(splitListItemEditArea.content.getFirstEditArea());
    }
  }

  public void moveEditAreas(TextEditArea initiatingEditArea, AbstractListItemEditArea splitListItemEditArea) {
    int splitAreaIndex = content.indexOf(initiatingEditArea);
    List<EditArea> removedAreas = content.removeEditAreaComponents(splitAreaIndex + 1);
    splitListItemEditArea.addEditAreas(removedAreas);
    this.getParent().addListItem(this, splitListItemEditArea);
  }

  public TextEditArea appendText(TextEditArea text) {
    TextEditArea lastArea = content.getLastEditArea().asTextArea();
    if (lastArea == null) {
      lastArea = text.copyArea();
      content.appendTextEditArea(lastArea);
    }
    else {
      lastArea.appendText(text);
    }
    return lastArea;
  }

  @Override
  public void viewsNachinitialisieren() {
    if (changeInfo.isDeleted()) {
      content.setGeloeschtMarkiertStilUDBL(null);
    }
    content.viewsNachinitialisieren();
  }

  public EditArea getFirstEditArea() { return content.getFirstEditArea(); }

  public void removeEditAreaComponent(EditArea editArea) { content.removeEditAreaComponent(editArea); }

  public List<EditArea> removeEditAreaComponents(int fromIndex) { return content.removeEditAreaComponents(fromIndex); }

  public EditArea getLastEditArea() { return content.getLastEditArea(); }

  @Override public synchronized void addFocusListener(FocusListener l) { content.addEditAreasFocusListener(l); }
  @Override public synchronized void addComponentListener(ComponentListener l) { content.addEditComponentListener(l); }
  @Override public void requestFocus() { content.requestFocus(); }

  //**************** canonical EditArea method implementations ************************
  @Override public boolean enthaeltAenderungsmarkierungen() { return changeInfo.isChange() || content.enthaeltAenderungsmarkierungen(); }
  @Override public void aenderungsmarkierungenEntfernen() { content.aenderungsmarkierungenEntfernen(null); }
  @Override public void findStepnumberLinkIDs(HashMap<TextEditArea, List<String>> stepnumberLinkMap) { content.findStepnumberLinkIDs(stepnumberLinkMap); }
  @Override public boolean enthaelt(InteractiveStepFragment fragment) { return content.enthaelt(fragment); }
  @Override public ChangeInfo getChangeInfo() { return changeInfo; }
  @Override public void setChangeInfo(ChangeInfo changeInfo) { this.changeInfo = changeInfo; }
  @Override public EditContainer getParent() { return (EditContainer) super.getParent(); }
  @Override public Component asComponent() { return this; }
  @Override public String getPlainText() { return content.getPlainText(); }
  @Override public TextEditArea asTextArea() { return null; }
  @Override public ImageEditArea asImageArea() { return null; }
  @Override public AbstractListItemEditArea asListItemArea() { return this; };
  @Override public boolean isListItemArea() { return true; }
  @Override public void setQuellStil() { /* Not required for list items - source steps only contain an empty text area */ }
  @Override public void setEditDecorationIndentions(Indentions indentions) { /* Nothing to do here */ }
  @Override public String getText() { return "list item"; }
  @Override public void addSchrittnummer(StepnumberLabel schrittNummer) {}
  @Override public List<JTextComponent> getTextAreas() { return content.getTextAreas(); }

}

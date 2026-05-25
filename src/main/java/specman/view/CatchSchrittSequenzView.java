package specman.view;

import specman.TextInit;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import specman.*;
import static specman.ChangeSet.changeset;
import specman.editarea.EditContainer;
import specman.editarea.InteractiveStepFragment;
import specman.editarea.TextEditArea;
import specman.graphics.Styles;
import specman.model.v001.CatchSchrittSequenzModel_V001;
import specman.model.v001.CoCatchModel_V001;
import specman.model.v001.EditorContentModel_V001;
import specman.pdf.Shape;
import specman.undo.UndoableCatchSequenceRemoved;
import specman.undo.UndoableCoCatchAdded;
import specman.undo.UndoableCoCatchRemoved;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;

import static specman.Aenderungsart.Geloescht;
import static specman.Aenderungsart.Hinzugefuegt;
import static specman.ColumnSpecByPercent.copyOf;
import static specman.view.AbstractSchrittView.*;

public class CatchSchrittSequenzView extends ZweigSchrittSequenzView implements FocusListener, SpaltenContainerI {
  JPanel headingPanel;
  JPanel headingRightBarPanel;
  JPanel headingHeightEaterPanel;
  FormLayout headingPanelLayout;
  CatchUeberschrift primaryCatchHeading;
  int headingRightBarWidth;

  /** The co-catches are additional linked break steps which share the same catch sequence
   * with the primary linked break step. This is the correspondence to something like
   * <pre>
   *   catch (ExceptionType1 | ExceptionType2... e) {
   * </pre>
   * where everything following the very first exception type is a co-catch. It contributes
   * only an additional heading to this {@link CatchSchrittSequenzView}, being placed between
   * the primary heading and the handling sequence. */
  List<CatchUeberschrift> coCatchHeadings = new ArrayList<>();

  public CatchSchrittSequenzView(CatchBereich catchBereich, BreakSchrittView linkedBreakStep) {
    super(Specman.instance(), catchBereich, linkedBreakStep.id.naechsteEbene(), linkedBreakStep.getEditorContent(true));
    einfachenSchrittAnhaengen(Specman.instance());
    init(linkedBreakStep, null, TextInit.initialArt());
    initHeadingsLayout();
  }

  public CatchSchrittSequenzView(AbstractSchrittView parent, CatchSchrittSequenzModel_V001 model) {
    super(Specman.instance(), parent, model);
    BreakSchrittView linkedBreakSchritt = (BreakSchrittView) parent.getParent().findStepByStepID(model.id.toString());
    init(linkedBreakSchritt, model.headingRightBarWidth, model.aenderungsart);
    initCoCatches(model.coCatches);
    initHeadingsLayout();
  }

  private void initHeadingsLayout() {
    createHeadingsLayout();
    reassignHeadingsToLayout();
  }

  private void createHeadingsLayout() {
    String rowSpecs = "fill:pref, fill:0px:grow";
    for (int i = 0; i < coCatchHeadings.size(); i++) {
      rowSpecs += ", " + FORMLAYOUT_GAP + ", fill:pref";
    }
    String colSpecs = "fill:pref:grow";
    if (!coCatchHeadings.isEmpty()) {
      colSpecs += ", " + FORMLAYOUT_GAP + ", " + umgehungLayout(headingRightBarWidth);
    }
    headingPanelLayout = new FormLayout(colSpecs, rowSpecs);
    headingPanel.setLayout(headingPanelLayout);
  }

  private void reassignHeadingsToLayout() {
    headingPanel.removeAll();
    headingPanel.add(primaryCatchHeading, CC.xyw(1, 1, headingPanelLayout.getColumnCount()));
    headingPanel.add(headingHeightEaterPanel, CC.xywh(1, 2, headingPanelLayout.getColumnCount(), 1));
    if (!coCatchHeadings.isEmpty()) {
      headingPanel.add(headingRightBarPanel, CC.xywh(3, 3, 1, headingPanelLayout.getRowCount()-2));
      headingPanel.add(new SpaltenResizer(this), CC.xywh(2, 3, 1, headingPanelLayout.getRowCount()-2));
    }
    for (int i = 0; i < coCatchHeadings.size(); i++) {
      headingPanel.add(coCatchHeadings.get(i), CC.xy(1, 4 + i * 2));
    }
  }

  @Override
  public int spaltenbreitenAnpassenNachMausDragging(int delta, int spalte) {
    int newRightBarX = headingPanel.getWidth() - headingRightBarWidth + delta;
    if (newRightBarX < headingPanel.getWidth()/2
      || newRightBarX > headingPanel.getWidth()) {
      return 0;
    }

    // The resizer is LEFT from the right bar, so the delta must be applied inversely
    updateBarWidthInLayout(headingRightBarWidth - delta);
    return delta;
  }

  private void init(BreakSchrittView linkedBreakStep, Integer headingRightBarWidth, Aenderungsart initialChangetype) {
    headingPanel = new JPanel();
    headingPanel.setBackground(Styles.DIAGRAMM_LINE_COLOR);
    headingRightBarPanel = new JPanel();
    headingRightBarPanel.setBackground(initialChangetype.toBackgroundColor());
    headingHeightEaterPanel = new JPanel();
    headingHeightEaterPanel.setBackground(initialChangetype.toBackgroundColor());
    this.headingRightBarWidth = headingRightBarWidth != null ? headingRightBarWidth : SPALTENLAYOUT_UMGEHUNG_GROESSE;
    ueberschrift.setId(linkedBreakStep.id);
    primaryCatchHeading = new CatchUeberschrift(ueberschrift, linkedBreakStep, this, initialChangetype);
    linkedBreakStep.catchAnkoppeln(primaryCatchHeading);
    ueberschrift.addEditAreasFocusListener(this);
  }

  private void initCoCatches(List<CoCatchModel_V001> coCatches) {
    if (coCatches != null) { // For compatibility with Specman versions < 1.0.2
      int insertionIndex = 0;
      for (CoCatchModel_V001 coCatchModel : coCatches) {
        BreakSchrittView breakStepToLink = (BreakSchrittView) parent.getParent().findStepByStepID(coCatchModel.breakStepId.toString());
        addCoCatch(insertionIndex, coCatchModel.heading, breakStepToLink, coCatchModel.changetype);
        insertionIndex++;
      }
    }
  }

  public void addCoCatchUDBL(CatchUeberschrift referenceCatchHeading, BreakSchrittView breakStepToLink) {
    int insertionIndex = coCatchHeadings.indexOf(referenceCatchHeading) + 1;
    EditorContentModel_V001 breakStepContent = breakStepToLink.getEditorContent(true);
    CatchUeberschrift coCatchHeading = addCoCatch(insertionIndex, breakStepContent, breakStepToLink, TextInit.initialArt());
    initHeadingsLayout();
    Specman.instance().addEdit(new UndoableCoCatchAdded(this, breakStepToLink, insertionIndex, coCatchHeading));
  }

  public void addCoCatchUDBL(int insertionIndex, CatchUeberschrift coCatchHeading, BreakSchrittView breakStepToLink) {
    coCatchHeadings.add(insertionIndex, coCatchHeading);
    breakStepToLink.catchAnkoppeln(coCatchHeading);
    initHeadingsLayout();
    Specman.instance().addEdit(new UndoableCoCatchAdded(this, breakStepToLink, insertionIndex, coCatchHeading));
    headingPanel.revalidate();
  }

  private CatchUeberschrift addCoCatch(int insertionIndex, EditorContentModel_V001 heading, BreakSchrittView breakStepToLink, Aenderungsart changetype) {
    EditContainer coCatchHeadingContent = new EditContainer(Specman.instance(), heading, breakStepToLink.id);
    coCatchHeadingContent.addEditAreasFocusListener(this);
    CatchUeberschrift coCatchHeading = new CatchUeberschrift(coCatchHeadingContent, breakStepToLink, this, changetype);
    coCatchHeadings.add(insertionIndex, coCatchHeading);
    breakStepToLink.catchAnkoppeln(coCatchHeading);
    return coCatchHeading;
  }

  @Override
  protected void ueberschriftInitialisieren(EditorI editor, EditorContentModel_V001 initialerText, SchrittID initialeSchrittnummer) {
    // Dummy SchrittID causes the heading to be created with step number label which will be updated later
    super.ueberschriftInitialisieren(editor, initialerText, new SchrittID(0));
  }

  @Override
  public void skalieren(int prozentNeu, int prozentAktuell) {
    super.skalieren(prozentNeu, prozentAktuell);
    primaryCatchHeading.skalieren(prozentNeu, prozentAktuell);
    coCatchHeadings.stream().forEach(coCatchHeading -> coCatchHeading.skalieren(prozentNeu, prozentAktuell));
    updateBarWidthInLayout(groesseUmrechnen(headingRightBarWidth, prozentNeu, prozentAktuell));
  }

  private void updateBarWidthInLayout(int headingRightBarWidth) {
    this.headingRightBarWidth = headingRightBarWidth;
    if (!coCatchHeadings.isEmpty()) {
      String barWidthSpec = umgehungLayout(headingRightBarWidth);
      headingPanelLayout.setColumnSpec(3, ColumnSpec.decode(barWidthSpec));
      headingPanel.revalidate();
    }
  }

  protected void catchBereichInitialisieren() {
    // There is no catch area in a catch sequence ;-)
  }

  protected void catchBereichSkalieren(int prozentNeu, int prozentAktuell) {}

  public Component getHeadingPanel() { return headingPanel; }

  @Override
  public CatchBereich getParent() { return (CatchBereich) super.getParent(); }

  public void setId(SchrittID id) {
    sequenzBasisId = id.naechsteEbene();
    renummerieren();
  }

  public void removeOrMarkAsDeletedUDBL() {
    EditorI editor = Specman.instance();
    if (aenderungsart == Hinzugefuegt || !editor.aenderungenVerfolgen()) {
      removeUDBL();
    }
    else {
      alsGeloeschtMarkierenUDBL();
    }
  }

  public void removeUDBL(CatchUeberschrift catchHeading) {
    if (catchHeading == primaryCatchHeading) {
      removeUDBL();
    }
    else {
      int deletionIndex = coCatchHeadings.indexOf(catchHeading);
      coCatchHeadings.remove(catchHeading);
      catchHeading.disconnectLinkedBreakStep();
      initHeadingsLayout();
      Specman.instance().addEdit(new UndoableCoCatchRemoved(this, catchHeading.linkedBreakStep, catchHeading, deletionIndex));
    }
  }

  public void removeOrMarkAsDeletedUDBL(CatchUeberschrift catchHeading) {
    EditorI editor = Specman.instance();
    if (catchHeading.changetype == Hinzugefuegt || !editor.aenderungenVerfolgen()) {
      removeUDBL(catchHeading);
    }
    else {
      catchHeading.alsGeloeschtMarkierenUDBL();
    }
  }

  public void removeUDBL() {
    CatchBereich catchBereich = getParent();
    List<Integer> backupSequencesWidthPercent = copyOf(catchBereich.sequencesWidthPercent);
    int catchIndex = catchBereich.catchEntfernen(this);
    primaryCatchHeading.disconnectLinkedBreakStep();
    coCatchHeadings.stream().forEach(coCatchHeading -> coCatchHeading.disconnectLinkedBreakStep());
    Specman.instance().addEdit(new UndoableCatchSequenceRemoved(this, catchIndex, backupSequencesWidthPercent));
  }

  @Override
  protected void ueberschriftAlsGeloeschtMarkierenUDBL() {
    primaryCatchHeading.alsGeloeschtMarkierenUDBL();
    coCatchHeadings.forEach(cch -> cch.alsGeloeschtMarkierenUDBL());
    headingHeightEaterPanel.setBackground(changeset().panelColor());
    headingRightBarPanel.setBackground(changeset().panelColor());
  }

  @Override public void focusGained(FocusEvent e) {}

  @Override public void focusLost(FocusEvent e) {
    if (aenderungsart != Geloescht) {
      TextEditArea editArea = (TextEditArea) e.getSource();
      CatchUeberschrift catchHeading = editArea.containingCatchHeading();
      catchHeading.updateLinkedBreakStepContent();
    }
  }

  @Override
  public int aenderungenUebernehmen(EditorI editor) throws EditException {
    if (aenderungsart == Geloescht) {
      removeUDBL();
      return 1;
    }
    else {
      int numChanges = super.aenderungenUebernehmen(editor)
        + primaryCatchHeading.aenderungenUebernehmen();
      for (CatchUeberschrift coCatchHeading : modifyableCoCatchHeadings()) {
        numChanges += coCatchHeading.aenderungenUebernehmen();
      }
      return numChanges;
    }
  }

  @Override
  public int aenderungenVerwerfen(EditorI editor) throws EditException {
    Aenderungsart lastChangetype = aenderungsart;
    int changesReverted = super.aenderungenVerwerfen(editor) + primaryCatchHeading.aenderungenVerwerfen();
    for (CatchUeberschrift coCatchHeading : modifyableCoCatchHeadings()) {
      changesReverted += coCatchHeading.aenderungenVerwerfen();
    }
    if (lastChangetype == Geloescht) {
      // While the catch sequences was marked as deleted, its heading was not synchronized
      // with the linked break step's content. So when we have rolled back a deletion, we
      // might have to resynchronize
      updateHeadings();
    }
    return changesReverted;
  }

  /** Required for iterations that may modify the list of headings.
   * Working directly on the list whould cause concurrent operation exceptions
   * in these cases. */
  private List<CatchUeberschrift> modifyableCoCatchHeadings() {
    return new ArrayList<>(coCatchHeadings);
  }

  private void updateHeadings() {
    primaryCatchHeading.updateLinkedBreakStepContent();
    coCatchHeadings.stream().forEach(coCatchHeading -> coCatchHeading.updateLinkedBreakStepContent());
  }

  @Override
  public void aenderungsmarkierungenEntfernen() {
    primaryCatchHeading.aenderungsmarkierungenEntfernen();
    coCatchHeadings.forEach(cch -> cch.aenderungsmarkierungenEntfernen());
    headingHeightEaterPanel.setBackground(Styles.BACKGROUND_COLOR_STANDARD);
    headingRightBarPanel.setBackground(Styles.BACKGROUND_COLOR_STANDARD);
  }

  public CatchSchrittSequenzModel_V001 generiereModel(boolean formatierterText) {
    List<CoCatchModel_V001> coCatches = generateCoCatchModels(formatierterText);
    CatchSchrittSequenzModel_V001 model = new CatchSchrittSequenzModel_V001(
      primaryCatchHeading.linkedBreakStepId(),
      aenderungsart,
      ueberschrift.editorContent2Model(formatierterText),
      coCatches,
      headingRightBarWidth);
    populateModel(model, formatierterText);
    return model;
  }

  private List<CoCatchModel_V001> generateCoCatchModels(boolean formatierterText) {
    return new ArrayList<>(coCatchHeadings
      .stream()
      .map(coCatchHeading -> coCatchHeading.toModel(formatierterText))
      .toList());
  }

  @Override
  public Shape getShapeSequence() {
    Shape shape = super.getShapeSequence();
    if (shape != null) {
      Shape headingShape = new Shape(headingPanel, this);
      headingShape.add(primaryCatchHeading.getShape());
      for (CatchUeberschrift coCatchHeading : coCatchHeadings) {
        headingShape.add(coCatchHeading.getShape());
      }
      headingShape
        .add(headingRightBarPanel)
        .add(headingHeightEaterPanel);
      shape.add(headingShape);
    }
    return shape;
  }

  /** Reconnecting is required for undo / redo operations. As a catch sequence may be removed
   * either <i>separately</i> or <i>combined with the linked breakstep</i>. Therefore the
   * catch sequence gets de-connected from its break step on removal. Otherwise the break step
   * could not be connected with a new sequence. If this sequence here is restored by undo / redo,
   * the cut connection must be re-established. */
  public void reconnectToBreakstep() {
    primaryCatchHeading.connectLinkedBreakStep();
    coCatchHeadings.stream().forEach(coCatchHeading -> coCatchHeading.connectLinkedBreakStep());
  }

  public boolean contains(CatchUeberschrift catchHeading) {
    return primaryCatchHeading == catchHeading || coCatchHeadings.contains(catchHeading);
  }

  public boolean isDeleted() {
    return aenderungsart == Geloescht;
  }

  public boolean isPrimaryHeading(CatchUeberschrift catchUeberschrift) {
    return catchUeberschrift == primaryCatchHeading;
  }

  public boolean enthaelt(InteractiveStepFragment fragment) {
    return headingFromFragment(fragment) != null;
  }

  CatchUeberschrift headingFromFragment(InteractiveStepFragment fragment) {
    if (primaryCatchHeading.ueberschrift.enthaelt(fragment)) {
      return primaryCatchHeading;
    }
    return coCatchHeadings
      .stream()
      .filter(cch -> cch.ueberschrift.enthaelt(fragment))
      .findFirst()
      .orElse(null);
  }

  public boolean allowsMoveDown(CatchUeberschrift catchHeading) {
    int index = coCatchHeadings.indexOf(catchHeading);
    return index >= 0 && index < coCatchHeadings.size() - 1;
  }

  public boolean allowsMoveUp(CatchUeberschrift catchHeading) {
    int index = coCatchHeadings.indexOf(catchHeading);
    return index > 0;
  }

  public void moveUpUDBL(CatchUeberschrift catchHeading) {
    moveUDBL(catchHeading, -1);
  }

  public void moveDownUDBL(CatchUeberschrift catchHeading) {
    moveUDBL(catchHeading, 1);
  }

  public void moveUDBL(CatchUeberschrift catchHeading, int delta) {
    int index = coCatchHeadings.indexOf(catchHeading);
    BreakSchrittView breakStep = catchHeading.linkedBreakStep;
    removeUDBL(catchHeading);
    addCoCatchUDBL(index + delta, catchHeading, breakStep);

  }

  public boolean allowsDeletion(CatchUeberschrift catchHeading) {
    return !isDeleted() && !catchHeading.isDeleted();
  }
}

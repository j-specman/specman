package specman.view;

import org.jetbrains.annotations.NotNull;
import specman.TextInit;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import specman.*;
import static specman.ChangeSet.changeset;

import specman.draganddrop.LocalCursor;
import specman.editarea.EditContainer;
import specman.editarea.InteractiveStepFragment;
import specman.editarea.TextEditArea;
import specman.graphics.Styles;
import specman.model.v002.CatchSequenceModel_V002;
import specman.model.v002.CoCatchModel_V002;
import specman.model.v002.EditorContentModel_V002;
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

import specman.undo.props.UDBL;
import static specman.ColumnSpecByPercent.copyOf;
import static specman.util.ObjectUtils.nvl;
import static specman.view.AbstractSchrittView.*;
import static specman.Specman.editor;

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

  public CatchSchrittSequenzView(CatchBereich catchBereich, BreakSchrittView linkedBreakStep, ChangeInfo changeInfo) {
    super(catchBereich, linkedBreakStep.number.naechsteEbene(), linkedBreakStep.getEditorContent(true), changeInfo);
    einfachenSchrittAnhaengen();
    init(linkedBreakStep, null, TextInit.initialChangeInfo());
    initHeadingsLayout();
    initHeadingChangeSet(linkedBreakStep);
  }

  private void initHeadingChangeSet(BreakSchrittView linkedBreakStep) {
    ChangeInfo breakStepChangeInfo = linkedBreakStep.getChangeInfo();
    if (breakStepChangeInfo.isChange() && breakStepChangeInfo.changeSet() != changeset()) {
      ueberschrift.mergeChangeSetUDBL(changeset(), breakStepChangeInfo.changeSet(), false);
    }
  }

  public CatchSchrittSequenzView(AbstractSchrittView parent, CatchSequenceModel_V002 model) {
    super(parent, model);
    BreakSchrittView linkedBreakStep = (BreakSchrittView) parent.getParent().findStepByUUID(model.id);
    init(linkedBreakStep, model.headingRightBarWidth, model.changeInfo != null ? model.changeInfo.toChangeInfo() : ChangeInfo.UNTRACKED);
    initCoCatchesV2(model.coCatches);
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

  private void init(BreakSchrittView linkedBreakStep, Integer headingRightBarWidth, ChangeInfo initialChangeInfo) {
    headingPanel = new JPanel();
    headingPanel.setBackground(Styles.DIAGRAMM_LINE_COLOR);
    headingRightBarPanel = new JPanel();
    headingRightBarPanel.setBackground(initialChangeInfo.panelColor());
    headingHeightEaterPanel = new JPanel();
    headingHeightEaterPanel.setBackground(initialChangeInfo.panelColor());
    this.headingRightBarWidth = nvl(headingRightBarWidth, SPALTENLAYOUT_UMGEHUNG_GROESSE);
    sequenzBasisId = linkedBreakStep.number.naechsteEbene();
    ueberschrift.setId(linkedBreakStep.number);
    primaryCatchHeading = new CatchUeberschrift(ueberschrift, linkedBreakStep, this, initialChangeInfo);
    linkedBreakStep.catchAnkoppeln(primaryCatchHeading);
    ueberschrift.addEditAreasFocusListener(this);
  }

  private void initCoCatchesV2(List<CoCatchModel_V002> coCatches) {
    if (coCatches == null) return;
    int insertionIndex = 0;
    for (CoCatchModel_V002 coCatchModel : coCatches) {
      if (coCatchModel.breakStepId == null) continue;
      BreakSchrittView breakStepToLink = (BreakSchrittView) parent.getParent().findStepByUUID(coCatchModel.breakStepId);
      if (breakStepToLink != null) {
        addCoCatch(insertionIndex, coCatchModel.heading, breakStepToLink,
            coCatchModel.changeInfo != null ? coCatchModel.changeInfo.toChangeInfo() : ChangeInfo.UNTRACKED);
        insertionIndex++;
      }
    }
  }

  public void addCoCatchUDBL(CatchUeberschrift referenceCatchHeading, BreakSchrittView breakStepToLink) {
    int insertionIndex = coCatchHeadings.indexOf(referenceCatchHeading) + 1;
    EditorContentModel_V002 breakStepContent = breakStepToLink.getEditorContent(true);
    CatchUeberschrift coCatchHeading = addCoCatch(insertionIndex, breakStepContent, breakStepToLink, TextInit.initialChangeInfo());
    initHeadingsLayout();
    editor().addEdit(new UndoableCoCatchAdded(this, breakStepToLink, insertionIndex, coCatchHeading));
  }

  public void addCoCatchUDBL(int insertionIndex, CatchUeberschrift coCatchHeading, BreakSchrittView breakStepToLink) {
    coCatchHeadings.add(insertionIndex, coCatchHeading);
    breakStepToLink.catchAnkoppeln(coCatchHeading);
    initHeadingsLayout();
    editor().addEdit(new UndoableCoCatchAdded(this, breakStepToLink, insertionIndex, coCatchHeading));
    headingPanel.revalidate();
  }

  private CatchUeberschrift addCoCatch(int insertionIndex, EditorContentModel_V002 heading, BreakSchrittView breakStepToLink, ChangeInfo changetype) {
    EditContainer coCatchHeadingContent = new EditContainer(heading, breakStepToLink.number);
    coCatchHeadingContent.addEditAreasFocusListener(this);
    CatchUeberschrift coCatchHeading = new CatchUeberschrift(coCatchHeadingContent, breakStepToLink, this, changetype);
    coCatchHeadings.add(insertionIndex, coCatchHeading);
    breakStepToLink.catchAnkoppeln(coCatchHeading);
    return coCatchHeading;
  }

  @Override
  protected void ueberschriftInitialisieren(EditorContentModel_V002 content) {
    ueberschrift = new EditContainer(content, new StepNumber(0));
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

  public void setId(StepNumber id) {
    sequenzBasisId = id.naechsteEbene();
    renummerieren();
  }

  public void removeOrMarkAsDeletedUDBL() {
    EditorI editor = editor();
    if (changeInfo.isAdded() || !editor.aenderungenVerfolgen()) {
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
      editor().addEdit(new UndoableCoCatchRemoved(this, catchHeading.linkedBreakStep, catchHeading, deletionIndex));
    }
  }

  public void removeOrMarkAsDeletedUDBL(CatchUeberschrift catchHeading) {
    EditorI editor = editor();
    if (catchHeading.changeInfo.isAdded() || !editor.aenderungenVerfolgen()) {
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
    editor().addEdit(new UndoableCatchSequenceRemoved(this, catchIndex, backupSequencesWidthPercent));
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
    if (!changeInfo.isDeleted()) {
      TextEditArea editArea = (TextEditArea) e.getSource();
      CatchUeberschrift catchHeading = editArea.containingCatchHeading();
      catchHeading.updateLinkedBreakStepContent();
    }
  }

  @Override
  public void mergeChangeSetUDBL(@NotNull ChangeSet target, @NotNull ChangeSet source) {
    boolean ownChangeAffected = changeInfo.changedBy(source);
    super.mergeChangeSetUDBL(target, source);
    if (ownChangeAffected) {
      UDBL.setBackgroundUDBL(headingRightBarPanel, changeInfo.panelColor());
      UDBL.setBackgroundUDBL(headingHeightEaterPanel, changeInfo.panelColor());
    }
    primaryCatchHeading.mergeChangeSetUDBL(target, source);
    coCatchHeadings.forEach(h -> h.mergeChangeSetUDBL(target, source));
  }

  @Override
  public int aenderungenUebernehmen() throws EditException {
    if (changeInfo.deletedBy(changeset())) {
      removeUDBL();
      return 1;
    }
    else {
      int numChanges = super.aenderungenUebernehmen()
        + primaryCatchHeading.aenderungenUebernehmen();
      for (CatchUeberschrift coCatchHeading : modifyableCoCatchHeadings()) {
        numChanges += coCatchHeading.aenderungenUebernehmen();
      }
      return numChanges;
    }
  }

  @Override
  public int aenderungenVerwerfen() throws EditException {
    ChangeInfo lastChangetype = changeInfo;
    int changesRejected = super.aenderungenVerwerfen() + primaryCatchHeading.aenderungenVerwerfen();
    for (CatchUeberschrift coCatchHeading : modifyableCoCatchHeadings()) {
      changesRejected += coCatchHeading.aenderungenVerwerfen();
    }
    if (lastChangetype.isDeleted() && lastChangetype.changeSet() == changeset()) {
      // While the catch sequences was marked as deleted, its heading was not synchronized
      // with the linked break step's content. So when we have rolled back a deletion, we
      // might have to resynchronize
      updateHeadings();
    }
    return changesRejected;
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

  public CatchSequenceModel_V002 generiereModel(boolean formatierterText) {
    List<CoCatchModel_V002> coCatches = generateCoCatchModels(formatierterText);
    CatchSequenceModel_V002 model = new CatchSequenceModel_V002(
      primaryCatchHeading.linkedBreakStepUUID(),
      changeInfo,
      ueberschrift.editorContent2Model(formatierterText),
      coCatches,
      headingRightBarWidth);
    populateModel(model, formatierterText);
    return model;
  }

  private List<CoCatchModel_V002> generateCoCatchModels(boolean formatierterText) {
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
    return changeInfo.isDeleted();
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

  public CatchUeberschrift findCatchHeading(LocalCursor localCursor) {
    if (localCursor.isInAny(primaryCatchHeading, headingRightBarPanel, headingHeightEaterPanel)) {
      return primaryCatchHeading;
    }
    for (CatchUeberschrift coCatchHeading : coCatchHeadings) {
      if (localCursor.isIn(coCatchHeading)) {
        return coCatchHeading;
      }
    }
    return null;
  }

  public Component dropZoneBelow(CatchUeberschrift hoverHeading) {
    return hoverHeading == primaryCatchHeading
      ? headingHeightEaterPanel
      : hoverHeading;
  }
}

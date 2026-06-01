package specman.view;

import org.jetbrains.annotations.NotNull;
import specman.Aenderungsart;
import specman.ChangeInfo;
import specman.ChangeSet;
import specman.EditException;
import specman.EditorI;
import specman.SchrittID;
import specman.Specman;

import static specman.ChangeSet.changeset;
import static specman.util.ObjectUtils.nvl;
import specman.editarea.EditArea;
import specman.editarea.stepnumberlabel.StepnumberLabel;
import specman.model.v001.AbstractSchrittModel_V001;
import specman.model.v001.BreakSchrittModel_V001;
import specman.model.v001.CaseSchrittModel_V001;
import specman.model.v001.EditorContentModel_V001;
import specman.model.v001.EinfacherSchrittModel_V001;
import specman.model.v001.IfElseSchrittModel_V001;
import specman.model.v001.IfSchrittModel_V001;
import specman.model.v001.QuellSchrittModel_V001;
import specman.model.v001.SubsequenzSchrittModel_V001;
import specman.model.v001.WhileSchrittModel_V001;
import specman.model.v001.WhileWhileSchrittModel_V001;
import specman.draganddrop.BranchHeadingZone;
import specman.draganddrop.DragSource;
import specman.draganddrop.DropTarget;
import specman.pdf.RoundedBorderShape;
import specman.pdf.Shape;
import specman.editarea.EditContainer;
import specman.editarea.Indentions;
import specman.editarea.InteractiveStepFragment;
import specman.editarea.TextEditArea;
import specman.undo.props.UDBL;

import javax.swing.JComponent;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static specman.Aenderungsart.Geloescht;
import static specman.Aenderungsart.Zielschritt;
import static specman.graphics.Styles.BACKGROUND_COLOR_STANDARD;
import static specman.view.RelativeStepPosition.After;
import static specman.view.RoundedBorderDecorationStyle.Co;
import static specman.view.RoundedBorderDecorationStyle.Full;
import static specman.view.RoundedBorderDecorationStyle.None;
import static specman.view.StepRemovalPurpose.Accept;
import static specman.view.StepRemovalPurpose.Discard;
import static specman.view.StepRemovalPurpose.Move;
import static specman.view.StepRemovalPurpose.Reject;
import static specman.Specman.editor;

abstract public class AbstractSchrittView implements KlappbarerBereichI, ComponentListener, FocusListener {
	public static final int LINIENBREITE = 2;
	public static final String FORMLAYOUT_GAP = LINIENBREITE + "px";
	public static final String ZEILENLAYOUT_INHALT_SICHTBAR = "fill:pref:grow";
	public static final String ZEILENLAYOUT_INHALT_VERBORGEN = "0px";
	public static final int SPALTENLAYOUT_UMGEHUNG_GROESSE = 18;

	protected static final List<SchrittSequenzView> KEINE_SEQUENZEN = new ArrayList<SchrittSequenzView>();

	protected final EditContainer editContainer;
	protected SchrittID id;
	protected ChangeInfo changeInfo;
	protected SchrittSequenzView parent;
	protected RoundedBorderDecorator roundedBorderDecorator;
	protected QuellSchrittView quellschritt;

	private final java.util.List<TextEditArea> referencedByTextEditAreas = new ArrayList<>();

	public AbstractSchrittView(SchrittSequenzView parent, EditorContentModel_V001 initialContent, SchrittID id, ChangeInfo changeInfo) {
		this.id = id;
		this.changeInfo = changeInfo;
		this.editContainer = new EditContainer(initialContent, id);
		this.parent = parent;
		editContainer.addEditAreasFocusListener(this);
		editContainer.addEditComponentListener(this);
	}

	public void setAenderungsartUDBL(Aenderungsart aenderungsart) {
    ChangeSet changeset = nvl(changeInfo.changeSet(), changeset());
		UDBL.setChangeInfo(this, new ChangeInfo(aenderungsart, changeset));
	}

	public ChangeInfo getChangeInfo() { return changeInfo; }

	public void setChangeInfo(ChangeInfo changeInfo) { this.changeInfo = changeInfo; }

	public void setId(SchrittID id) {
		SchrittID oldSchrittID = this.id;

		this.id = id;
		editContainer.setId(id);

		if (!oldSchrittID.equals(id)) {
			for (TextEditArea textEditArea : referencedByTextEditAreas) {
				textEditArea.updateStepnumberLink(oldSchrittID.toString(), id.toString());
			}
      if (quellschritt != null) {
        quellschritt.resyncStepnumberStyleUDBL();
      }
		}
	}

	public SchrittID newStepIDInSameSequence(RelativeStepPosition direction) {
		return direction == RelativeStepPosition.After ? id.naechsteID() : id.sameID();
	}

	protected EditorContentModel_V001 getEditorContent(boolean formatierterText) {
		return editContainer.editorContent2Model(formatierterText);
	}

	public void setBackgroundUDBL(Color bg) {
		editContainer.setBackgroundUDBL(bg);
	}

	public Color getBackground() {
		return editContainer.getBackground();
	}

	public void scrollTo() {
    editContainer.scrollTo();
	}

	public Shape getShape() {
		return decoratedShape(new Shape(getPanel(), this)
			.withBackgroundColor(editContainer.getBackground())
			.add(editContainer.getShape()));
	}

	protected Shape decoratedShape(Shape undecoratedShape) {
		return roundedBorderDecorator != null
			? new RoundedBorderShape(roundedBorderDecorator, undecoratedShape)
			: undecoratedShape;
	}

	public JComponent getDecoratedComponent() {
		return decorated(getPanel());
	}

	protected JComponent decorated(JComponent undecoratedComponent) {
		return nvl(roundedBorderDecorator, undecoratedComponent);
	}

	public boolean isStrukturiert() { return false; }

	void schrittnummerSichtbarkeitSetzen(boolean sichtbar) {
		editContainer.schrittnummerAnzeigen(sichtbar);
	}

	abstract public AbstractSchrittModel_V001 generiereModel(boolean formatierterText);

	public static AbstractSchrittView baueSchrittView( SchrittSequenzView parent, AbstractSchrittModel_V001 model) {
		if (model instanceof WhileWhileSchrittModel_V001) {
			return new WhileWhileSchrittView(parent, (WhileWhileSchrittModel_V001) model);
		}
		if (model instanceof WhileSchrittModel_V001) {
			return new WhileSchrittView(parent, (WhileSchrittModel_V001) model);
		}
		if (model instanceof IfElseSchrittModel_V001) {
			return new IfElseSchrittView(parent, (IfElseSchrittModel_V001) model);
		}
		if (model instanceof IfSchrittModel_V001) {
			return new IfSchrittView(parent, (IfSchrittModel_V001) model);
		}
		if (model instanceof CaseSchrittModel_V001) {
			return new CaseSchrittView(parent, (CaseSchrittModel_V001) model);
		}
		if (model instanceof SubsequenzSchrittModel_V001) {
			return new SubsequenzSchrittView(parent, (SubsequenzSchrittModel_V001) model);
		}
		if (model instanceof BreakSchrittModel_V001) {
			return new BreakSchrittView(parent, (BreakSchrittModel_V001) model);
		}
		//TODO TEST
		if (model instanceof QuellSchrittModel_V001){
			return new QuellSchrittView(parent, (QuellSchrittModel_V001) model);
		}
		// TEST ENDE
		return new EinfacherSchrittView(parent, (EinfacherSchrittModel_V001)model);
	}

	public void geklappt(boolean auf) {}

	public void zusammenklappenFuerReview() {
		unterSequenzen().forEach(SchrittSequenzView::zusammenklappenFuerReview);
	}

	public String ersteZeileExtraieren() {
		String[] zeilen = editContainer.getPlainText().split("\n");
		for (String zeile: zeilen) {
			String getrimmteZeile = zeile.trim();
			if (getrimmteZeile.length() > 0)
				return getrimmteZeile;
		}
		return null;
	}

	public void setGeloeschtMarkiertStilUDBL() {
		setAenderungsartUDBL(Geloescht);
		setBackgroundUDBL(changeInfo.changeSet().panelColor());
		editContainer.setGeloeschtMarkiertStilUDBL(id, changeInfo.changeSet());
	}

	public void setZielschrittStilUDBL() {
		setAenderungsartUDBL(Zielschritt);
		editContainer.setZielschrittStilUDBL(getQuellschritt().getId(), changeInfo.changeSet());
	}

	public void alsGeloeschtMarkierenUDBL() {
		unterSequenzen().forEach(SchrittSequenzView::alsGeloeschtMarkierenUDBL);
		setGeloeschtMarkiertStilUDBL();
	}

	/** Entfernt im Rahmen der Übernahme oder Rücknahme von Änderungen alle Einfärbungen,
	 * die bis dahin im Änderungsmodus entstanden sind.
	 * <ul>
	 *   <li>Im Änderungsmodus hinzugefügte oder verschobene Schritte haben einen hellgelben
	 *   Hintergrund, der im Falle der Übernahme von Änderungen wieder auf weis geändert
	 *   werden muss</li>
	 *   <li>In allen {@link EditContainer}n eines gelöschten Schritts wurde die Schrift
	 *   im Änderungsmodus auf grau mit schwarzem Hintergrund gesetzt, was im Falle einer
	 *   Rücknahme der Löschung wieder geändert werden muss.
	 *   </li>
	 * </ul>
	 * Einfärbungen für <i>inhaltliche</i> Änderungen von {@link EditContainer}n spielen
	 * hier keine Rolle. Diese werden bereits <i>vor</i> dem Aufruf der Methode hier über
	 * {@link #editAenderungenUebernehmen} bzw. {@link #editAenderungenVerwerfen()} entfernt. */
	public void aenderungsmarkierungenEntfernen() {
		setBackgroundUDBL(BACKGROUND_COLOR_STANDARD);
		editContainer.aenderungsmarkierungenEntfernen(id);
	}

	public boolean enthaeltAenderungsmarkierungen() {
		if (editContainer.enthaeltAenderungsmarkierungen()) {
			return true;
		}
		return unterSequenzen()
			.stream()
			.anyMatch(untersequenz -> untersequenz.enthaeltAenderungsmarkierungen());
	}

	public AbstractSchrittView findeSchritt(InteractiveStepFragment fragment) {
		for (SchrittSequenzView unterSequenz: unterSequenzen()) {
			AbstractSchrittView schritt = unterSequenz.findeSchritt(fragment);
			if (schritt != null)
				return schritt;
		}
		return null;
	}

	/** Liefert alle in einem Schritt enthalten Untersequenzen, um damit die verschiedenen
	 * Traversierungsfunktionen wie {@link #findeSchritt(InteractiveStepFragment)} zu füttern. Damit
	 * spart man sich das rekursive Absteigen in allen Ableitungen für jede dieser
	 * Funktionen zu dublizieren
	 */

	public List<SchrittSequenzView> unterSequenzen() {
		return KEINE_SEQUENZEN;
	}

	/** Bisschen Convenience, um die Funktion unterSequenz als Einzeler schreiben zu k�nnen */
	protected static List<SchrittSequenzView> sequenzenAuflisten(List<? extends SchrittSequenzView> sequenzSammlung, SchrittSequenzView... einzelSequenzen) {
		List<SchrittSequenzView> ergebnis = new ArrayList<SchrittSequenzView>();
		if (sequenzSammlung != null)
			ergebnis.addAll(sequenzSammlung);
		ergebnis.addAll(Arrays.asList(einzelSequenzen));
		return ergebnis;
	}

	protected static List<SchrittSequenzView> sequenzenAuflisten(SchrittSequenzView... einzelSequenzen) {
		return sequenzenAuflisten(null, einzelSequenzen);
	}

	/** Informiert den Schritt darüber, dass er gerade aus seiner Sequenz entfernt wird */
	public void entfernen(SchrittSequenzView container, StepRemovalPurpose purpose) {
		unterSequenzen().forEach(sequenz -> sequenz.entfernen(this, purpose));
	}

	public void skalieren(int prozentNeu, int prozentAktuell) {
		editContainer.skalieren(prozentNeu, prozentAktuell);
		editContainer.updateBounds();
		if (roundedBorderDecorator != null) {
			roundedBorderDecorator.skalieren(prozentNeu, editContainer.getStepNumberBounds().getHeight());
		}
		unterSequenzen().forEach(sequenz -> sequenz.skalieren(prozentNeu, prozentAktuell));
	}

	public boolean enthaelt(InteractiveStepFragment fragment) {
		return editContainer.enthaelt(fragment);
	}

	static int groesseUmrechnen(int groesse, int prozentNeu, int prozentAktuell) {
		float groesse100Prozent = (float)groesse / prozentAktuell * 100;
		return (int)(groesse100Prozent * prozentNeu / 100);
	}

	static String umgehungLayout() {
		return umgehungLayout(SPALTENLAYOUT_UMGEHUNG_GROESSE * editor().getZoomFactor() / 100);
	}

	static String umgehungLayout(int groesse) {
		return "fill:" + groesse + "px";
	}

	public EditArea getFirstEditArea() { return editContainer.getFirstEditArea(); }

	public JComponent toggleBorderType() {
		JComponent toggleResult;
		if (roundedBorderDecorator == null) {
			JComponent coreComponent = getDecoratedComponent();
			roundedBorderDecorator = new RoundedBorderDecorator(coreComponent, editContainer.getStepNumberBounds().getHeight());
			RoundedBorderDecorationStyle requiredDecorationStyle =
					parent.deriveDecorationStyleFromPosition(this);
			roundedBorderDecorator.setStyle(requiredDecorationStyle);
			toggleResult = roundedBorderDecorator;
		}
		else {
			JComponent coreComponent = roundedBorderDecorator.getDecoratedComponent();
			roundedBorderDecorator.remove(coreComponent);
			roundedBorderDecorator = null;
			toggleResult = coreComponent;
		}
		updateTextfieldDecorationIndentions();
		return toggleResult;
	}

	private boolean decorationRequiresTopInset() {
		return parent.decorationRequiresTopInset(this);
	}

	protected void updateTextfieldDecorationIndentions(Indentions indentions) {
		editContainer.updateDecorationIndentions(indentions);
	}

	public void updateTextfieldDecorationIndentions() {
		Indentions indentions = new Indentions(getDecorated());
		updateTextfieldDecorationIndentions(indentions);
	}

	public RoundedBorderDecorationStyle getDecorated() {
		if (roundedBorderDecorator == null) {
			return None;
		}
		return roundedBorderDecorator.getStyle();
	}

	public void decorateAsFollower(RoundedBorderDecorationStyle predecessorDeco) {
		if (getDecorated() != None) {
			roundedBorderDecorator.setStyle(predecessorDeco == None ? Full : Co);
		}
	}

	public void initInheritedTextFieldIndentions() {
		AbstractSchrittView decoratedParent = parent.findFirstDecoratedParent();
		if (decoratedParent != null) {
			decoratedParent.updateTextfieldDecorationIndentions();
		}
	}

	public SchrittSequenzView getParent() { return parent; }

	public void setParent(SchrittSequenzView parent){
		this.parent = parent;
	}

	public EditContainer getTextShef() {
		return editContainer;
	}

	public SchrittID getId() {
		return id;
	}

	public abstract JComponent getPanel();

	public void setQuellschrittUDBL(QuellSchrittView quellschritt){
		UDBL.setQuellschrittUDBL(this, quellschritt);
	}

	public QuellSchrittView getQuellschritt(){
		return quellschritt;
	}

	public void setQuellschritt(QuellSchrittView quellschritt) { this.quellschritt = quellschritt; }

	public SchrittID getQuellschrittID(){
		return quellschritt != null ? quellschritt.getId() : null;
	}

	public void resyncStepnumberStyleUDBL() {
    if (changeInfo.isSourceStep()) {
			editContainer.resyncStepnumberAsSourceUDBL(((QuellSchrittView)this).getZielschrittID());
		}
		else if (changeInfo.isTargetStep()) {
			editContainer.resyncStepnumberAsTargetUDBL(getQuellschritt().getId());
		}
		unterSequenzen().forEach(SchrittSequenzView::resyncStepnumberStyleADBL);
	}

	public void viewsNachinitialisieren() {
    switch (changeInfo.art()) {
      case Geloescht -> setGeloeschtMarkiertStilUDBL();
      case Quellschritt -> ((QuellSchrittView) this).setQuellStil();
      case Zielschritt -> setZielschrittStilUDBL();
    }
    editContainer.viewsNachinitialisieren();
		registerAllExistingStepnumbers();
		unterSequenzen().forEach(SchrittSequenzView::viewsNachinitialisieren);
	}

	/**
	 * Registers all stepnumbers found in all editAreas.
	 * <p>
	 * This is needed for loading diagrams since the references between stepnumberLinks
	 * and its referenced stepnumber are not saved.
	 * <p>
	 * This can be further optimized by using a HashMap as a cache to prevent
	 * calling {@link Specman#findStepByStepID(String)} more than once for the same step.
	 * However, to benefit from such a cache it would need to be shared with other {@link AbstractSchrittView}s
	 */
	protected void registerAllExistingStepnumbers() {
    editContainer.registerAllExistingStepnumbers();
	}

	public AbstractSchrittView findeSchrittZuId(SchrittID id) {
		if (this.id.equals(id)) return this;
		for (SchrittSequenzView seq : unterSequenzen()) {
			AbstractSchrittView result = seq.findeSchrittZuId(id);
			if (result != null) return result;
		}
		return null;
	}

	public void mergeChangeSetUDBL(@NotNull ChangeSet target, @NotNull ChangeSet source) {
		if (changeInfo.changedBy(source)) {
			UDBL.setChangeInfo(this, changeInfo.reassign(target));
			setBackgroundUDBL(changeInfo.panelColor());
		}
		editContainer.mergeChangeSetUDBL(target, source, true);
		unterSequenzen().forEach(seq -> seq.mergeChangeSetUDBL(target, source));
	}

	public int aenderungenUebernehmen() throws EditException {
		ChangeSet currentSet = changeset();
		int changesMade = editAenderungenUebernehmen();
		if (changeInfo.changedBy(currentSet)) {
			changesMade += changeInfo.numChangesBy(currentSet);
			switch (changeInfo.art()) {
				case Geloescht:
				case Quellschritt:
					markStepnumberLinksAsDefect();
					getParent().schrittEntfernen(this, Accept);
					break;
				case Zielschritt:
					setQuellschrittUDBL(null);
					break;
			}
			aenderungsmarkierungenEntfernen();
			changeInfo = changeInfo.untrack(currentSet);
		}
		return changesMade;
	}

	protected int editAenderungenUebernehmen() {
		return editContainer.aenderungenUebernehmen();
	}

	public int aenderungenVerwerfen() throws EditException {
		ChangeSet currentSet = changeset();
		int changesRejected = editAenderungenVerwerfen();
		if (changeInfo.changedBy(currentSet)) {
			changesRejected += changeInfo.numChangesBy(currentSet);
			switch (changeInfo.art()) {
				case Hinzugefuegt:
					markStepnumberLinksAsDefect();
					getParent().schrittEntfernen(this, Reject);
					break;
				case Zielschritt:
					getParent().schrittEntfernen(this, Move);
					setId(getQuellschritt().newStepIDInSameSequence(After));
					setParent(getQuellschritt().getParent());
					getQuellschritt().getParent().insertStep(this, After, getQuellschritt());
					getQuellschritt().getParent().schrittEntfernen(getQuellschritt(), Discard);
					setQuellschrittUDBL(null);
					break;
			}
			aenderungsmarkierungenEntfernen();
			changeInfo = changeInfo.untrack(currentSet);
		}
		return changesRejected;
	}

	protected int editAenderungenVerwerfen() {
		return editContainer.aenderungenVerwerfen();
	}

	public void registerStepnumberLink(TextEditArea textEditArea) {
		referencedByTextEditAreas.add(textEditArea);
	}

	public void unregisterStepnumberLink(TextEditArea textEditArea) {
		if (!referencedByTextEditAreas.remove(textEditArea)) {
			throw new IllegalArgumentException(
					"The referenced TextEditArea '" + textEditArea.getPlainText() + "' was not registered." +
					" If there was a recent Undo/Redo, check if registerStepnumberLink() was called.");
		}
	}

	public void markStepnumberLinksAsDefect() {
		String id = getId().toString();
        for (TextEditArea referencedByTextEditArea : referencedByTextEditAreas) {
            referencedByTextEditArea.markStepnumberLinkAsDefect(id);
        }
	}

	public boolean hasStepnumberLinks() {
		return !referencedByTextEditAreas.isEmpty();
	}

	@Override
	public void componentResized(ComponentEvent e) {
		editContainer.updateBounds();
	}

	@Override public void componentMoved(ComponentEvent e) {
	}

	@Override public void componentShown(ComponentEvent e) {
	}

	@Override public void componentHidden(ComponentEvent e) {
	}

	@Override public void focusGained(FocusEvent e) {}

	@Override
	public void focusLost(FocusEvent e) {}

	@Override
	public String toString() {
		return id + " - " + getTextShef().getPlainText();
	}

	public List<JTextComponent> getTextAreas() {
		List<JTextComponent> result = editContainer.getTextAreas();
		unterSequenzen().forEach(seq -> result.addAll(seq.getTextAreas()));
		return result;
	}

	public List<BreakSchrittView> queryUnlinkedBreakSteps() {
		List<BreakSchrittView> result = new ArrayList<>();
		unterSequenzen().forEach(seq -> result.addAll(seq.queryUnlinkedBreakSteps()));
		return result;
	}

  public boolean refersToOtherStep() { return false; }

  public Boolean getFlatNumbering() { return null; }

  public int dragIndicatorTopOffset(ZweigSchrittSequenzView branch) { return 0; }

  public DropTarget findDropTarget(Point localCursor, DragSource dragSource) { return null; }

  public List<BranchHeadingZone> getBranchHeadingZones(DragSource dragSource) { return List.of(); }

  public void toggleFlatNumbering(boolean flatNumbering) {}

  /** Method being called by a step's sub-sequence when either a step has been added to or removed
   * from that sequence. If the step uses flat numbering, it has to initiate renumbering
   * of following steps in its own sequence. */
  public void renumberFollowingSteps(SchrittSequenzView modifiedSubsequence) {}

  /** Returns true, if the passed initiating label addresses a branch sequence the
   * position of which can be switched with another branch sequence at the left.
   * This is the case e.g. for the label
   * <ul>
   *   <li>in a catch sequence heading which with at least one other catch sequence at the left</li>
   *   <li>of any step being a direct child of a branch sequence in a case step with other case
   *   sequences at the left</li>
   *   <li>for any step being a direct child in the if-sequence of an if-else step</li>
   * </ul>*/
  public boolean allowsBranchSequenceMoveLeft(StepnumberLabel initiatingLabel) { return false; }

  /** the opposite of {@link #allowsBranchSequenceMoveLeft(StepnumberLabel)} */
  public boolean allowsBranchSequenceMoveRight(StepnumberLabel initiatingLabel) { return false; }

  public boolean allowsCoCatchMoveUp(StepnumberLabel initiatingLabel) { return false; }

  public boolean allowsCoCatchMoveDown(StepnumberLabel initiatingLabel) { return false; }

  public void moveCoCatchUpUDBL(StepnumberLabel initiatingLabel) {}

  public void moveCoCatchDownUDBL(StepnumberLabel initiatingLabel) {}

  public boolean allowsDeletion(StepnumberLabel initiatingLabel) {
    return getParent().allowsStepDeletion() && !changeInfo.isDeleted();
  }
}

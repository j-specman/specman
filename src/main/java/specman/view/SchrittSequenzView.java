package specman.view;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import org.jetbrains.annotations.NotNull;
import specman.Aenderungsart;
import specman.EditException;
import specman.EditorI;
import specman.SchrittID;
import specman.Specman;
import specman.TextInit;
import specman.model.v001.AbstractSchrittModel_V001;
import specman.model.v001.EditorContentModel_V001;
import specman.model.v001.SchrittSequenzModel_V001;
import specman.pdf.Shape;
import specman.editarea.EditContainer;
import specman.editarea.Indentions;
import specman.editarea.InteractiveStepFragment;
import specman.undo.props.UDBL;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static specman.Aenderungsart.Untracked;
import static specman.TextInit.initialtext;
import static specman.styles.Styles.DIAGRAMM_LINE_COLOR;
import static specman.pdf.Shape.GAP_COLOR;
import static specman.view.RelativeStepPosition.After;
import static specman.view.RelativeStepPosition.Before;
import static specman.view.RoundedBorderDecorationStyle.Co;
import static specman.view.RoundedBorderDecorationStyle.Full;
import static specman.view.RoundedBorderDecorationStyle.None;

public class SchrittSequenzView {
	public static final String ZEILENLAYOUT_GAP = AbstractSchrittView.FORMLAYOUT_GAP;
	public static final String ZEILENLAYOUT_SCHRITT = "fill:pref";
	public static final String ZEILENLAYOUT_LETZTER_SCHRITT = "fill:pref:grow";
	public static final String ZEILENLAYOUT_CATCHBEREICH = "pref";

	SchrittID sequenzBasisId;
	@NotNull Aenderungsart aenderungsart;
	final JPanel sequenzBereich;
	CatchBereich catchBereich;
	final JPanel panel;
	//CopyOnWriteArrayList Arraylisten ermöglichen das removen von Schritten während man eine Liste durchläuft
	//wird benötigt um mehre als gelöscht markierte Schritte auf einmal zu löschen
	public final List<AbstractSchrittView> schritte = new CopyOnWriteArrayList<AbstractSchrittView>();
	final FormLayout sequenzbereichLayout;
	boolean schrittnummernSichtbar = true;
	final FormLayout huellLayout;
	final AbstractSchrittView parent;

	public SchrittSequenzView() {
		this(null, new SchrittID(0));
	}

	public SchrittSequenzView(AbstractSchrittView parent, SchrittID sequenzBasisId) {
		this.parent = parent;
		panel = new JPanel();
		huellLayout = new FormLayout("10px:grow", ZEILENLAYOUT_LETZTER_SCHRITT + ", " + ZEILENLAYOUT_CATCHBEREICH);
		panel.setLayout(huellLayout);
		panel.setBackground(TextInit.schrittHintergrund());
		this.sequenzBasisId = sequenzBasisId;
		sequenzBereich = new JPanel();
		sequenzBereich.setBackground(DIAGRAMM_LINE_COLOR);
		sequenzbereichLayout = new FormLayout("10px:grow");
		sequenzBereich.setLayout(sequenzbereichLayout);
		panel.add(sequenzBereich, CC.xy(1, 1));
		catchBereichInitialisieren();
	}

	protected void catchBereichInitialisieren() {
		catchBereich = new CatchBereich(this);
		panel.add(catchBereich.getPanel(), CC.xy(1, 2));
	}

	public SchrittSequenzView(EditorI editor, AbstractSchrittView parent, SchrittSequenzModel_V001 model) {
		this(parent, model.id);
    this.aenderungsart = model.aenderungsart;
		for (AbstractSchrittModel_V001 schritt : model.schritte) {
			AbstractSchrittView schrittView = AbstractSchrittView.baueSchrittView(editor, this, schritt);
			appendStep(schrittView);
			// TODO JL: Das hier ist schön einfach hinzuschreiben, aber ziemlich ineffizient
			// Wir sollten schauen, ob das herstellen einer *initialen* Dekoration nicht leichtgewichtiger geht
			if (schritt.decorationStyle != None) {
				toggleBorderType(schrittView);
			}
		}
		// Model is null if this sequence is itself a catch sequence which does not support nested catch sequences
		if (model.catchBereich != null) {
			catchBereich.populate(model.catchBereich);
		}
	}

	public Aenderungsart getAenderungsart() { return aenderungsart; }

	public void setAenderungsart(Aenderungsart aenderungsart) { this.aenderungsart = aenderungsart; }

	public void setAenderungsartUDBL(Aenderungsart aenderungsart) {
		UDBL.setAenderungsart(this, aenderungsart);
	}

	public List<AbstractSchrittView> getSchritte() { return schritte; }

	public JPanel getContainer() { return panel; }

	private SchrittID naechsteSchrittID() {
		if (schritte.size() > 0) {
      return getLastStep().newStepIDInSameSequence(After);
    }
		return sequenzBasisId.naechsteID();
	}

	void schrittnummerSichtbarkeitSetzen(boolean sichtbar) {
		schrittnummernSichtbar = sichtbar;
		for (AbstractSchrittView schritt: schritte) {
			schritt.schrittnummerSichtbarkeitSetzen(sichtbar);
		}
	}

	public AbstractSchrittView einfachenSchrittAnhaengen(EditorI editor) {
		EditorContentModel_V001 initialerText = initialtext("Neuer Schritt " + (schritte.size() + 1));
		EinfacherSchrittView schritt = new EinfacherSchrittView(editor, this, initialerText, naechsteSchrittID(), TextInit.initialArt());
		return appendStep(schritt);
	}

	public AbstractSchrittView whileSchrittAnhaengen(EditorI editor) {
		EditorContentModel_V001 initialerText = initialtext("Neue Schleife " + (schritte.size() + 1));
		WhileSchrittView schritt = new WhileSchrittView(editor, this, initialerText, naechsteSchrittID(), TextInit.initialArt());
		return appendStep(schritt);
	}

	public AbstractSchrittView whileWhileSchrittAnhaengen(EditorI editor) {
		EditorContentModel_V001 initialerText = initialtext("Neue Schleife " + (schritte.size() + 1));
		WhileWhileSchrittView schritt = new WhileWhileSchrittView(editor, this, initialerText, naechsteSchrittID(), TextInit.initialArt());
		return appendStep(schritt);
	}

	public AbstractSchrittView ifElseSchrittAnhaengen(EditorI editor) {
		EditorContentModel_V001 initialerText = EditContainer.center("If-Else " + (schritte.size()+1));
		IfElseSchrittView schritt = new IfElseSchrittView(editor, this, initialerText, naechsteSchrittID(), TextInit.initialArt());
		schritt.initialeSchritteAnhaengen(editor);
		return appendStep(schritt);
	}

	public AbstractSchrittView ifSchrittAnhaengen(EditorI editor) {
		EditorContentModel_V001 initialerText = EditContainer.center("If " + (schritte.size()+1));
		IfSchrittView schritt = new IfSchrittView(editor, this, initialerText, naechsteSchrittID(), TextInit.initialArt());
		schritt.initialeSchritteAnhaengen(editor);
		return appendStep(schritt);
	}

	public AbstractSchrittView caseSchrittAnhaengen(EditorI editor) {
		EditorContentModel_V001 initialerText = initialtext("Case-" + (schritte.size()+1));
		CaseSchrittView schritt = new CaseSchrittView(editor, this, initialerText, naechsteSchrittID(), TextInit.initialArt());
		schritt.initialeSchritteAnhaengen(editor);
		return appendStep(schritt);
	}

	public AbstractSchrittView subsequenzSchrittAnhaengen(EditorI editor) {
		EditorContentModel_V001 initialerText = initialtext("<b>Subsequenz " + (schritte.size()+1) + "<b>");
		SubsequenzSchrittView schritt = new SubsequenzSchrittView(editor, this, initialerText, naechsteSchrittID(), TextInit.initialArt());
		return appendStep(schritt);
	}

	public AbstractSchrittView breakSchrittAnhaengen(EditorI editor) {
		EditorContentModel_V001 initialerText = initialtext("<b>Exception " + (schritte.size()+1) + "<b>");
		BreakSchrittView schritt = new BreakSchrittView(editor, this, initialerText, naechsteSchrittID(), TextInit.initialArt());
		return appendStep(schritt);
	}

	private void updateLayoutRowspecsForAllsStepsAndGaps() {
		for (int i = 0; i < schritte.size(); i++) {
			sequenzbereichLayout.setRowSpec(i*2 + 1, rowspec4step(i));
			if (i > 0) {
				AbstractSchrittView schritt = schritte.get(i);
				String rowSpec = schritt.getDecorated() == Co
						? AbstractSchrittView.ZEILENLAYOUT_INHALT_VERBORGEN
						: ZEILENLAYOUT_GAP;
				sequenzbereichLayout.setRowSpec(i*2, RowSpec.decode(rowSpec));
			}
		}
	}

	private RowSpec rowspec4step(int stepIndex) {
		return (stepIndex == schritte.size()-1)
			? RowSpec.decode(ZEILENLAYOUT_LETZTER_SCHRITT)
			: RowSpec.decode(ZEILENLAYOUT_SCHRITT);
	}

	private CellConstraints constraints4step(int stepIndex) {
		return CC.xy(1, stepIndex * 2 + 1);
	}

	public AbstractSchrittView appendStep(final AbstractSchrittView schritt) {
		schritt.schrittnummerSichtbarkeitSetzen(schrittnummernSichtbar);
		if (schritte.size() != 0) {
			sequenzbereichLayout.appendRow(RowSpec.decode(ZEILENLAYOUT_GAP));
		}
		sequenzbereichLayout.appendRow(RowSpec.decode(ZEILENLAYOUT_SCHRITT));
		sequenzBereich.add(schritt.getDecoratedComponent(), constraints4step(schritte.size()));
		schritte.add(schritt);
		updateLayoutRowspecsForAllsStepsAndGaps();
    renumberFollowingStepsInParent();
    return schritt;
	}

	public AbstractSchrittView einfachenSchrittZwischenschieben(RelativeStepPosition insertionPosition,
			AbstractSchrittView referenceStep, EditorI editor) {
		EditorContentModel_V001 initialerText = initialtext("Neuer Schritt " + (schritte.size() + 1));
		EinfacherSchrittView schritt = new EinfacherSchrittView
				(editor, this, initialerText, referenceStep.newStepIDInSameSequence(insertionPosition), TextInit.initialArt());
		return insertStep(schritt, insertionPosition, referenceStep);
	}

	public AbstractSchrittView whileSchrittZwischenschieben(RelativeStepPosition insertionPosition,
			AbstractSchrittView referenceStep, EditorI editor) {
		EditorContentModel_V001 initialerText = initialtext("Neue Schleife " + (schritte.size() + 1));
		WhileSchrittView schritt = new WhileSchrittView(editor, this, initialerText, referenceStep.newStepIDInSameSequence(insertionPosition), TextInit.initialArt());
		return insertStep(schritt, insertionPosition, referenceStep);
	}

	public AbstractSchrittView whileWhileSchrittZwischenschieben(RelativeStepPosition insertionPosition,
			AbstractSchrittView referenceStep, EditorI editor) {
		EditorContentModel_V001 initialerText = initialtext("Neue Schleife " + (schritte.size() + 1));
		WhileWhileSchrittView schritt = new WhileWhileSchrittView(editor, this, initialerText, referenceStep.newStepIDInSameSequence(insertionPosition), TextInit.initialArt());
		return insertStep(schritt, insertionPosition, referenceStep);
	}

	public AbstractSchrittView ifElseSchrittZwischenschieben(RelativeStepPosition insertionPosition,
			AbstractSchrittView referenceStep, EditorI editor) {
		EditorContentModel_V001 initialerText = EditContainer.center("Neue Bedingung " + (schritte.size()+1));
		IfElseSchrittView schritt = new IfElseSchrittView(editor, this, initialerText, referenceStep.newStepIDInSameSequence(insertionPosition), TextInit.initialArt());
		schritt.initialeSchritteAnhaengen(editor);
		return insertStep(schritt, insertionPosition, referenceStep);
	}

	public AbstractSchrittView ifSchrittZwischenschieben(RelativeStepPosition insertionPosition,
			AbstractSchrittView referenceStep, EditorI editor) {
		EditorContentModel_V001 initialerText = EditContainer.center("Neue Bedingung " + (schritte.size()+1));
		IfSchrittView schritt = new IfSchrittView(editor, this, initialerText, referenceStep.newStepIDInSameSequence(insertionPosition), TextInit.initialArt());
		schritt.initialeSchritteAnhaengen(editor);
		return insertStep(schritt, insertionPosition, referenceStep);
	}

	public AbstractSchrittView caseSchrittZwischenschieben(RelativeStepPosition insertionPosition,
			AbstractSchrittView referenceStep, EditorI editor) {
		EditorContentModel_V001 initialerText = initialtext("Case-" + (schritte.size()+1));
		CaseSchrittView schritt = new CaseSchrittView(editor, this, initialerText, referenceStep.newStepIDInSameSequence(insertionPosition), TextInit.initialArt());
		schritt.initialeSchritteAnhaengen(editor);
		return insertStep(schritt, insertionPosition, referenceStep);
	}

	public AbstractSchrittView subsequenzSchrittZwischenschieben(RelativeStepPosition insertionPosition,
			AbstractSchrittView referenceStep, EditorI editor) {
		EditorContentModel_V001 initialerText = initialtext("<b>Subsequenz " + (schritte.size()+1) + "<b>");
		SubsequenzSchrittView schritt = new SubsequenzSchrittView(editor, this, initialerText, referenceStep.newStepIDInSameSequence(insertionPosition), TextInit.initialArt());
		return insertStep(schritt, insertionPosition, referenceStep);
	}

	public AbstractSchrittView breakSchrittZwischenschieben(RelativeStepPosition insertionPosition,
			AbstractSchrittView referenceStep, EditorI editor) {
		EditorContentModel_V001 initialerText = initialtext("<b>Exception " + (schritte.size()+1) + "<b>");
		BreakSchrittView schritt = new BreakSchrittView(editor, this, initialerText, referenceStep.newStepIDInSameSequence(insertionPosition), TextInit.initialArt());
		return insertStep(schritt, insertionPosition, referenceStep);
	}

	private int stepIndex(AbstractSchrittView schritt) {
		int i = 0;
		for (AbstractSchrittView vorgaenger: schritte) {
			if (vorgaenger == schritt) {
				return i;
			}
			i++;
		}
		throw new IllegalArgumentException("Step " + schritt + " is not part of sequenz " + this);
	}

	public AbstractSchrittView insertStep(AbstractSchrittView newStep, RelativeStepPosition insertionPosition, AbstractSchrittView referenceStep) {
		newStep.schrittnummerSichtbarkeitSetzen(schrittnummernSichtbar);

		int newStepOffset = (insertionPosition == After ? 1 : 0);
		int newStepIndex = stepIndex(referenceStep) + newStepOffset;

		sequenzbereichLayout.appendRow(RowSpec.decode(ZEILENLAYOUT_GAP));
		sequenzbereichLayout.appendRow(RowSpec.decode(ZEILENLAYOUT_SCHRITT));

		sequenzBereich.add(newStep.getDecoratedComponent(), constraints4step(newStepIndex));

		for (int followerIndex = newStepIndex; followerIndex < schritte.size(); followerIndex++) {
			AbstractSchrittView nachfolger = schritte.get(followerIndex);
			sequenzbereichLayout.setConstraints(nachfolger.getDecoratedComponent(), constraints4step(followerIndex+1));
		}

		schritte.add(newStepIndex, newStep);
		updateFollowingStepDecoration(newStepIndex+1);
		updateLayoutRowspecsForAllsStepsAndGaps();
		renumberFollowingSteps(newStep);
    renumberFollowingStepsInParent();
		return newStep;
	}

  private void renumberFollowingStepsInParent() {
    if (parent != null) {
      parent.renumberFollowingSteps(this);
    }
  }

  public void renumberFollowingSteps(AbstractSchrittView schritt) {
		int i = schritte.indexOf(schritt);
		for (i++; i<schritte.size(); i++) {
			AbstractSchrittView folgeschritt = schritte.get(i);
			folgeschritt.setId(schritt.newStepIDInSameSequence(After));
			schritt = folgeschritt;
		}
    // If the sequence is a sub-sequence within a step, we propagate the modification to the
    // parent as the change might require cascading renumbering on higher levels in case of
    // nested flat numbering
    if (parent != null) {
      parent.renumberFollowingSteps(this);
    }
	}

	public void renummerieren() { renummerieren(sequenzBasisId); }

	public void renummerieren(SchrittID sequenzBasisId) {
		this.sequenzBasisId = sequenzBasisId;
		if (schritte.size() > 0) {
			AbstractSchrittView ersterSchritt = schritte.get(0);
			ersterSchritt.setId(sequenzBasisId.naechsteID());
			renumberFollowingSteps(ersterSchritt);
		}
	}

	public void checkSchrittEntfernen(AbstractSchrittView schritt) throws EditException {
		if (schritte.size() == 1) {
			throw new EditException("Letzten Schritt entfernen is nich!");
		}
	}

	/**
	 * @return Den Index des entfernten Schritts in der Sequenz. Dient der Wiedereingliederung beim Redo
	 */
	public int schrittEntfernen(AbstractSchrittView schritt, StepRemovalPurpose purpose) throws EditException {
		checkSchrittEntfernen(schritt);
		schritt.entfernen(this, purpose);
		sequenzBereich.remove(schritt.getDecoratedComponent());
		int schrittIndex = schritte.indexOf(schritt);
		int layoutZeilenLoeschIndex = (schrittIndex == 0) ? 1 : schrittIndex*2;
		sequenzbereichLayout.removeRow(layoutZeilenLoeschIndex);
		sequenzbereichLayout.removeRow(layoutZeilenLoeschIndex);
		schritte.remove(schrittIndex);
		updateFollowingStepDecoration(schrittIndex);
		updateLayoutRowspecsForAllsStepsAndGaps();
		renummerieren(sequenzBasisId);
    renumberFollowingStepsInParent();
		AbstractSchrittView naechsterFokus = schritte.get(schrittIndex == 0 ? schrittIndex : schrittIndex-1);
		Specman.instance().diagrammAktualisieren(naechsterFokus.getFirstEditArea());
		return schrittIndex;
	}

	public void schrittHinzufuegen(AbstractSchrittView schritt, int schrittIndex) {
		if (schrittIndex == schritte.size()) {
			appendStep(schritt);
		}
		else {
			if (schrittIndex == 0) {
				AbstractSchrittView ersterSchritt = schritte.get(schrittIndex);
				insertStep(schritt, Before, ersterSchritt);
			}
			else {
				AbstractSchrittView vorgaengerSchritt = schritte.get(schrittIndex-1);
				insertStep(schritt, After, vorgaengerSchritt);
			}
		}
		Specman.instance().diagrammAktualisieren(schritt.getFirstEditArea());
	}

	public AbstractSchrittView findeSchritt(InteractiveStepFragment fragment) {
		for (AbstractSchrittView schritt: schritte) {
			if (schritt.enthaelt(fragment))
				return schritt;
			AbstractSchrittView subStep = schritt.findeSchritt(fragment);
			if (subStep != null) {
				return subStep;
			}
		}
		return (catchBereich != null) ? catchBereich.findeSchritt(fragment) : null;
	}

	public SchrittSequenzModel_V001 generiereSchrittSequenzModel(boolean formatierterText) {
		SchrittSequenzModel_V001 model = new SchrittSequenzModel_V001(
			sequenzBasisId, aenderungsart,
			catchBereich.generiereCatchBereichModel(formatierterText));
		populateModel(model, formatierterText);
		return model;
	}

	protected void populateModel(SchrittSequenzModel_V001 model, boolean formatierterText) {
		for (AbstractSchrittView view : schritte) {
			model.schritte.add(view.generiereModel(formatierterText));
		}
	}

	public boolean enthaeltAenderungsmarkierungen() {
		for (AbstractSchrittView schritt: schritte) {
			if (schritt.enthaeltAenderungsmarkierungen())
				return true;
		}
		return false;
	}

	public void setVisible(boolean auf) {
		sequenzBereich.setVisible(auf);
	}

	public void entfernen(AbstractSchrittView container, StepRemovalPurpose purpose) {
		for (AbstractSchrittView schritt: schritte) {
			schritt.entfernen(this, purpose);
		}
		catchBereich.entfernen(this, purpose);
	}

	public void zusammenklappenFuerReview() {
		for (AbstractSchrittView schritt: schritte) {
			schritt.zusammenklappenFuerReview();
		}
		catchBereich.zusammenklappenFuerReview();
	}

	public void skalieren(int prozentNeu, int prozentAktuell) {
		for (AbstractSchrittView schritt: schritte) {
			schritt.skalieren(prozentNeu, prozentAktuell);
		}
		catchBereichSkalieren(prozentNeu, prozentAktuell);
	}

	protected void catchBereichSkalieren(int prozentNeu, int prozentAktuell) {
		catchBereich.skalieren(prozentNeu, prozentAktuell);
	}

	public void resyncStepnumberStyleUDBL() {
		for (AbstractSchrittView schritt : schritte) {
			schritt.resyncStepnumberStyleUDBL();
		}
	}

	public void viewsNachinitialisieren() {
		for(AbstractSchrittView schritt: schritte) {
			schritt.viewsNachinitialisieren();
		}
	}

	public AbstractSchrittView findeSchrittZuId(SchrittID id){
		for(AbstractSchrittView schritt: schritte) {
			AbstractSchrittView result = schritt.findeSchrittZuId(id);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	public int aenderungenUebernehmen(EditorI editor) throws EditException {
		int changesMade = 0;
		for (AbstractSchrittView schritt: schritte) {
			changesMade += schritt.aenderungenUebernehmen(editor);
		}
		setAenderungsart(Untracked);
		if (catchBereich != null) {
			changesMade += catchBereich.aenderungenUebernehmen(editor);
		}
		return changesMade;
	}

	public int aenderungenVerwerfen(EditorI editor) throws EditException {
		int changesReverted = 0;
		for (AbstractSchrittView schritt: schritte) {
			changesReverted += schritt.aenderungenVerwerfen(editor);
		}
		setAenderungsart(Untracked);
		if (catchBereich != null) {
			changesReverted += catchBereich.aenderungenVerwerfen(editor);
		}
		return changesReverted;
	}

	public void alsGeloeschtMarkierenUDBL() {
		for (AbstractSchrittView schritt: schritte) {
			schritt.alsGeloeschtMarkierenUDBL();
		}
		setAenderungsartUDBL(Aenderungsart.Geloescht);
	}

	public void toggleBorderType(AbstractSchrittView schritt) {
		int stepIndex = stepIndex(schritt);
		int componentIndex = stepComponentIndex(schritt);
		sequenzBereich.remove(componentIndex);
		JComponent switchedStepComponent = schritt.toggleBorderType();
		CellConstraints constraints = constraints4step(stepIndex);
		sequenzBereich.add(switchedStepComponent, constraints, componentIndex);
		if (schritt.getDecorated() == None) {
			// If the step just lost its decoration by the toggling, its text fields
			// now have to adjust to what indentions the parent step may require
			schritt.initInheritedTextFieldIndentions();
		}
		updateFollowingStepDecoration(stepIndex+1);
		updateLayoutRowspecsForAllsStepsAndGaps();
	}

	/** Find the index of a step's grafical root component within this sequence' panel */
	int stepComponentIndex(AbstractSchrittView step) {
		JComponent stepComponent = step.getDecoratedComponent();
		Component[] sequenceChildren = sequenzBereich.getComponents();
		for (int componentIndex = 0; componentIndex < sequenceChildren.length; componentIndex++) {
			if (sequenceChildren[componentIndex] == stepComponent) {
				return componentIndex;
			}
		}
		throw new IllegalArgumentException("Step " + step + " is not part of " + this);
	}

	private void updateFollowingStepDecoration(int followerIndex) {
		if (schritte.size() > followerIndex) {
			RoundedBorderDecorationStyle preceedingStyle = None;
			if (followerIndex > 0) {
				AbstractSchrittView step = schritte.get(followerIndex-1);
				preceedingStyle = step.getDecorated();
			}
			AbstractSchrittView followingStep = schritte.get(followerIndex);
			followingStep.decorateAsFollower(preceedingStyle);
		}
	}

	public void updateTextfieldDecorationIndentions(Indentions lastStepIndention) {
		Indentions stepIndentions = lastStepIndention.withBottom(false);
		int s;
		for (s = 0; s < schritte.size() - 1; s++) {
			forwardTextfieldDecorationIndentions(s, stepIndentions);
		}
		if (s < schritte.size()) {
			forwardTextfieldDecorationIndentions(s, lastStepIndention);
		}
  }

  private void forwardTextfieldDecorationIndentions(int substepIndex, Indentions indentions) {
		AbstractSchrittView substep = schritte.get(substepIndex);
		if (substep.getDecorated() == None) {
			substep.updateTextfieldDecorationIndentions(indentions);
		}
	}

	public AbstractSchrittView findFirstDecoratedParent() {
		SchrittSequenzView sequenz = this;
		while(sequenz.parent != null) {
			if (sequenz.parent.getDecorated() != None) {
				return sequenz.parent;
			}
			sequenz = sequenz.parent.parent;
		}
		return null;
	}

	/** Returns true if the passed step would require a top inset if it is decorated
	 * by a rounded border. This is the case if the step is either the first one in its
	 * sequence or if the preceeding step isn't decorated too. If two decorated steps
	 * directly follow each other we don't want a double-sized space between them. */
	public boolean decorationRequiresTopInset(AbstractSchrittView step) {
		for (int i = 0; i < schritte.size(); i++) {
			if (schritte.get(i) == step) {
				return i == 0 || schritte.get(i-1).getDecorated() == None;
			}
		}
		return false;
	}

	public AbstractSchrittView getParent() {
		return parent;
	}

	public RoundedBorderDecorationStyle deriveDecorationStyleFromPosition(AbstractSchrittView childStep) {
		for (int i = 0; i < schritte.size(); i++) {
			if (schritte.get(i) == childStep) {
				return (i == 0 || schritte.get(i-1).getDecorated() == None)
						? Full : Co;
			}
		}
		return Co;
	}

	public List<JTextComponent> getTextAreas() {
		List<JTextComponent> result = new ArrayList<>();
		for (AbstractSchrittView schritt : schritte) {
			result.addAll(schritt.getTextAreas());
		}
		return result;
	}

	public Shape getShapeSequence() {
		if (!sequenzBereich.isVisible()) {
			return null;
		}
		Shape sequence = new Shape(sequenzBereich);
		for (AbstractSchrittView schritt : schritte) {
			sequence.add(schritt.getShape());
		}
		return new Shape(getContainer(), this)
			.withBackgroundColor(GAP_COLOR)
			.add(sequence)
			.add(getCatchShape());
	}

	private Shape getCatchShape() { return (catchBereich != null) ? catchBereich.getShape() : null; }

	public CatchSchrittSequenzView catchSequenzAnhaengenUDBL(BreakSchrittView breakStepToLink) {
		return catchBereich.catchSequenzAnhaengenUDBL(breakStepToLink);
	}

	public List<BreakSchrittView> queryUnlinkedBreakSteps() {
		List<BreakSchrittView> result = new ArrayList<>();
		for (AbstractSchrittView schritt: schritte) {
			result.addAll(schritt.queryUnlinkedBreakSteps());
		}
		return result;
	}

	public AbstractSchrittView findStepByStepID(String stepID) {
		for (AbstractSchrittView step: schritte) {
			if (stepID.equals(step.getId().toString())) {
				return step;
			}
			for (SchrittSequenzView unterSequenz : step.unterSequenzen()) {
				AbstractSchrittView result = unterSequenz.findStepByStepID(stepID);
				if (result != null) {
					return result;
				}
			}
		}
		if (catchBereich != null) {
			for (CatchSchrittSequenzView catchSequence: catchBereich.catchSequences) {
				AbstractSchrittView result = catchSequence.findStepByStepID(stepID);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}


	public List<AbstractSchrittView> listSteps() {
		List<AbstractSchrittView> stepList = new ArrayList<>();
		for (AbstractSchrittView step : getSchritte()) {
			stepList.add(step);
			for (SchrittSequenzView unterSequenz : step.unterSequenzen()) {
				List<AbstractSchrittView> subStepList = unterSequenz.listSteps();
				stepList.addAll(subStepList);
			}
		}
		if (catchBereich != null) {
			for (CatchSchrittSequenzView catchSequence: catchBereich.catchSequences) {
				stepList.addAll(catchSequence.listSteps());
			}
		}
		return stepList;
	}

  public void scrollTo() {
    schritte.get(0).scrollTo();
  }

  /** A step might only be deleted from a sequence if there remains at least
   * one step which is not marked for deletion. Otherwise, deletion of this last
   * step would leave the sequence empty, which is not allowed. */
  public boolean allowsStepDeletion() {
    int numStepsMarkedForDeletion = (int)schritte
      .stream()
      .filter(s -> s.getAenderungsart() == Aenderungsart.Geloescht)
      .count();
    return schritte.size() - numStepsMarkedForDeletion > 1;
  }

  public AbstractSchrittView getLastStep() {
    return schritte.get(schritte.size() - 1);
  }

}

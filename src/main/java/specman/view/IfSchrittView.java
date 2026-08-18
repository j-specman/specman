package specman.view;

import com.jgoodies.forms.layout.ColumnSpec;

import specman.ChangeInfo;
import specman.StepNumber;
import specman.model.v002.EditorContentModel_V002;
import specman.model.v002.AbstractStepModel_V002;
import specman.model.v002.IfStepModel_V002;
import specman.editarea.EditContainer;
import specman.undo.props.UDBL;


import java.awt.*;


import static specman.TextInit.schrittHintergrund;

import static specman.Specman.editor;

/**
 * Im Gegensatz zum Struktogramm-Standard verwenden wird die <i>rechte</i> Seite für die Sequenz der
 * bedingt auszuführenden Unterschritte und die linke Seite bleibt leer. Das ist vorteilhaft für die grafische
 * Anordnung. Z.B. kann der Fragetext linksbündig platziert werden.<br>
 * Durch die Basisklasse ist auf der rechten Seite der Else-Zweig. Der Einfachheit halber verwenden wir den
 * also hier. Kann man auch noch mal ändern, wenn das bei generativen Auswertungen der Modelle für zu
 * viel Verwirrung sorgen sollte.
 * 
 * @author less02
 */
public class IfSchrittView extends IfElseSchrittView {
	int ifBreite;
	
	public IfSchrittView(SchrittSequenzView parent, EditorContentModel_V002 initialerString, StepNumber id, ChangeInfo changeInfo) {
		super(parent, initialerString, id, changeInfo, false);
		initIfSequenz(new ZweigSchrittSequenzView(this, id.naechsteID().naechsteEbene(), EditorContentModel_V002.empty(), changeInfo));
		initElseSequenz(new ZweigSchrittSequenzView(this, id.naechsteEbene(), EditContainer.right("Ja"), changeInfo));
		ifBreite = SPALTENLAYOUT_UMGEHUNG_GROESSE + 2; /**@author PVN, Dueck */ 
	}

	public IfSchrittView(SchrittSequenzView parent, IfStepModel_V002 model) {
		super(parent, model.content, model.id, model.changeInfo != null ? model.changeInfo.toChangeInfo() : ChangeInfo.UNTRACKED);
		initIfSequenz(new ZweigSchrittSequenzView(this, new StepNumber(0), EditorContentModel_V002.empty(), this.changeInfo));
		initElseSequenz(new ZweigSchrittSequenzView(this, model.ifSequence));
		this.setBackgroundUDBL(new Color(model.color));
		ifBreiteSetzen(model.emptyWidth);
		klappen.init(model.collapsed);
		this.id = model.id;
	}

	@Override
	protected void initIfSequenz(ZweigSchrittSequenzView pIfSequenz) {
		super.initIfSequenz(pIfSequenz);
		ifSequenz.sequenzBereich.setBackground(schrittHintergrund());
	}

	@Override
	protected void initialeSchritteAnhaengen() {
		elseSequenz.einfachenSchrittAnhaengen();
	}

	@Override
	public void setNumber(StepNumber number) {
		super.setNumber(number);
		StepNumber elseID = number.naechsteEbene();
		elseSequenz.renummerieren(elseID);
	}
	
	@Override public StepNumber newStepIDInSameSequence(RelativeStepPosition direction) {
		return number.naechsteID();
	}

	@Override
	public int spaltenbreitenAnpassenNachMausDragging(int delta, int spalte) {
		int angepassteIfBreite = ifSequenz.ueberschrift.getWidth() + delta;
		ifBreiteSetzen(angepassteIfBreite);
		editor().diagrammAktualisieren(null);
		return delta;
	}

	private void ifBreiteSetzen(int angepassteIfBreite) {
		ifBreite = angepassteIfBreite;
		panelLayout.setColumnSpec(1, ColumnSpec.decode(angepassteIfBreite + "px"));
	}
	
	@Override
	protected int texteinrueckungNeuberechnen() {
		return ifSequenz.ueberschrift.getWidth();
	}
	
	@Override
	public void skalieren(int prozentNeu, int prozentAktuell) {
		super.skalieren(prozentNeu, prozentAktuell);
		int neueIfBreite = groesseUmrechnen(ifBreite, prozentNeu, prozentAktuell);
		ifBreiteSetzen(neueIfBreite);
	}

	@Override
	public AbstractStepModel_V002 generiereModel(boolean formatierterText) {
		return new IfStepModel_V002(
			id,
			getEditorContent(formatierterText),
			getBackground().getRGB(),
			getDecorated(),
			klappen.isSelected(),
			changeInfo,
			elseSequenz.generiereZweigSchrittSequenzModel(formatierterText),
			ifSequenz.ueberschrift.getWidth(),
			null);
	}
	
	public void setBackgroundUDBL(Color bg) {
		super.setBackgroundUDBL(bg);
		UDBL.setBackgroundUDBL(ifSequenz.sequenzBereich, bg);
		UDBL.repaint(panel); // Damit die Linien nachgezeichnet werden
	}
	
}

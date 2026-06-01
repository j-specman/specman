package specman.view;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;

import specman.ChangeInfo;
import specman.EditorI;
import specman.SchrittID;
import specman.Specman;
import specman.model.v001.EditorContentModel_V001;
import specman.model.v001.IfSchrittModel_V001;
import specman.model.v001.AbstractSchrittModel_V001;
import specman.editarea.EditContainer;
import specman.undo.props.UDBL;

import specman.draganddrop.DragSource;
import specman.draganddrop.DropTarget;
import specman.draganddrop.LocalCursor;

import java.awt.*;
import java.util.List;


import static specman.TextInit.schrittHintergrund;
import static specman.model.v001.EditorContentModel_V001.empty;
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
	
	public IfSchrittView(SchrittSequenzView parent, EditorContentModel_V001 initialerString, SchrittID id, ChangeInfo changeInfo) {
		super(parent, initialerString, id, changeInfo, false);
		initIfSequenz(new ZweigSchrittSequenzView(this, id.naechsteID().naechsteEbene(), empty(), changeInfo));
		initElseSequenz(new ZweigSchrittSequenzView(this, id.naechsteEbene(), EditContainer.right("Ja"), changeInfo));
		ifBreite = SPALTENLAYOUT_UMGEHUNG_GROESSE + 2; /**@author PVN, Dueck */ 
	}

	public IfSchrittView(SchrittSequenzView parent, IfSchrittModel_V001 model) {
		super(parent, model.inhalt, model.id, ChangeInfo.fromModel(model.changeInfo, model.aenderungsart), false);
		initIfSequenz(new ZweigSchrittSequenzView(this, new SchrittID(), empty(), this.changeInfo));
		initElseSequenz(new ZweigSchrittSequenzView(this, model.ifSequenz));
		this.setBackgroundUDBL(new Color(model.farbe));
		ifBreiteSetzen(model.leerBreite);
		klappen.init(model.zugeklappt);;
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
	public void setId(SchrittID id) {
		super.setId(id);
		SchrittID elseID = id.naechsteEbene();
		elseSequenz.renummerieren(elseID);
	}
	
	@Override public SchrittID newStepIDInSameSequence(RelativeStepPosition direction) {
		return id.naechsteID();
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
	public AbstractSchrittModel_V001 generiereModel(boolean formatierterText) {
		IfSchrittModel_V001 model = new IfSchrittModel_V001(
			id,
			getEditorContent(formatierterText),
			getBackground().getRGB(),
			getDecorated(),
			klappen.isSelected(),
			changeInfo,
			elseSequenz.generiereZweigSchrittSequenzModel(formatierterText),
			ifSequenz.ueberschrift.getWidth(), getQuellschrittID());
		return model;
	}
	
	@Override
	public DropTarget findHeadingDropTarget(LocalCursor localCursor, DragSource dragSource) {
		if (localCursor.isIn(elseSequenz.getUeberschrift())) {
			return new DropTarget(elseSequenz);
		}
		return null;
	}

	public void setBackgroundUDBL(Color bg) {
		super.setBackgroundUDBL(bg);
		UDBL.setBackgroundUDBL(ifSequenz.sequenzBereich, bg);
		UDBL.repaint(panel); // Damit die Linien nachgezeichnet werden
	}
	
}

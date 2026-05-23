package specman.view;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import specman.Aenderungsart;
import specman.EditException;
import specman.EditorI;
import specman.SchrittID;
import specman.SpaltenContainerI;
import specman.SpaltenResizer;
import specman.Specman;
import specman.TextInit;
import specman.model.v001.AbstractSchrittModel_V001;
import specman.model.v001.EditorContentModel_V001;
import specman.model.v001.IfElseSchrittModel_V001;
import specman.pdf.Shape;
import specman.editarea.EditContainer;
import specman.editarea.Indentions;
import specman.editarea.InteractiveStepFragment;
import specman.undo.props.UDBL;

import javax.swing.JPanel;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ComponentListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;

import static specman.TextInit.initialtext;

public class IfElseSchrittView extends VerzweigungSchrittView implements ComponentListener, SpaltenContainerI {
	ZweigSchrittSequenzView ifSequenz;
	ZweigSchrittSequenzView elseSequenz;
	boolean mittelpunktRaute = true;
	JPanel leeresFeld;
	JPanel panelBedingung;
	JPanel panelElse;
	JPanel panelIf;

	protected IfElseSchrittView(EditorI editor, SchrittSequenzView parent, EditorContentModel_V001 initialerText, SchrittID id, Aenderungsart anderungsart, boolean withDefaultContent) {
		super(editor, parent, initialerText, id, anderungsart, createPanelLayout());
		/** @author PVN */
		leeresFeld = new JPanel();
		leeresFeld.setBackground(TextInit.schrittHintergrund());
		panelBedingung = new JPanel();
		panelBedingung.setBackground(TextInit.schrittHintergrund());
		panelBedingung.setLayout(createSpalteLinks());
		panelBedingung.add(editContainer, "2,1");
		panel.add(panelBedingung, CC.xy(3, 1));
		panel.add(leeresFeld, CC.xy(1, 1));
		editContainer.addEditAreasFocusListener(new FocusAdapter() {
			@Override public void focusLost(FocusEvent e) {
				berechneHoeheFuerVollstaendigUnberuehrtenText();
			}
		});
		panel.add(new SpaltenResizer(this), CC.xywh(2, 1, 1, 5));
		if(withDefaultContent) {
			initIfSequenz(new ZweigSchrittSequenzView(editor, this, id.naechsteEbene(), initialtext("Ja")));
			initElseSequenz(new ZweigSchrittSequenzView(editor, this, id.naechsteID().naechsteEbene(), EditContainer.right("Nein")));
		}
	}

	public IfElseSchrittView(EditorI editor, SchrittSequenzView parent, IfElseSchrittModel_V001 model) {
		this(editor, parent, model.inhalt, model.id, model.aenderungsart, false);
		panel.add(new SpaltenResizer(this), CC.xywh(2, 1, 1, 5));
		initIfSequenz(new ZweigSchrittSequenzView(editor, this, model.ifSequenz));
		initElseSequenz(new ZweigSchrittSequenzView(editor, this, model.elseSequenz));
		setBackgroundUDBL(new Color(model.farbe));
		ifBreitenanteilSetzen(model.ifBreitenanteil);
		klappen.init(model.zugeklappt);
	}

	public IfElseSchrittView(EditorI editor, SchrittSequenzView parent, EditorContentModel_V001 initialerText, SchrittID id, Aenderungsart aenderungsart) {
		this(editor, parent, initialerText, id, aenderungsart, true);
	}

	protected void initIfSequenz(ZweigSchrittSequenzView pIfSequenz) {
		this.ifSequenz = pIfSequenz;
		ifBedingungAnlegen(ifSequenz);
		panel.add(ifSequenz.getContainer(), CC.xy(1, 5)); /**@author PVN */
	}

	protected void initElseSequenz(ZweigSchrittSequenzView pElseSequenz) {
		this.elseSequenz = pElseSequenz;
		elseBedingungAnlegen(elseSequenz);
		panel.add(elseSequenz.getContainer(), CC.xy(3, 5)); /**@author PVN */
	}

	protected static FormLayout createPanelLayout() {
		return new FormLayout(
				"10px:grow, " + FORMLAYOUT_GAP + ", 10px:grow",
				layoutRowSpec1() + ", " + FORMLAYOUT_GAP + ", fill:pref, " + FORMLAYOUT_GAP + ", " + ZEILENLAYOUT_INHALT_SICHTBAR);
	}

	protected void elseBedingungAnlegen(ZweigSchrittSequenzView elseSequenz) {
		elseSequenz.ueberschrift.addEditAreasFocusListener(this);
		/** @author PVN */
		panelElse = new JPanel();
		panelElse.setBackground(TextInit.schrittHintergrund());
		panelElse.setLayout(createSpalteLinks());
		panelElse.add(elseSequenz.ueberschrift, CC.xywh(2, 1, 1, 1));
		panel.add(panelElse, CC.xy(3, 3));
	}

	protected void ifBedingungAnlegen(ZweigSchrittSequenzView ifSequenz) {
		ifSequenz.ueberschrift.addEditAreasFocusListener(this);
		/**@author PVN */
		panelIf = new JPanel();
		panelIf.setBackground(TextInit.schrittHintergrund());
		panelIf.setLayout(createSpalteRechts());
		panelIf.add(ifSequenz.ueberschrift, CC.xy(1,1));
		panel.add(panelIf, CC.xy(1, 3));
	}

	@Override
	public int spaltenbreitenAnpassenNachMausDragging(int delta, int spalte) {
		float angepassteIfBreite = ifSequenz.ueberschrift.getWidth() + delta;
		float angepassteElseBreite = elseSequenz.ueberschrift.getWidth() - delta;
		float angepassterIfBreitenanteil = ifBreitenanteil(angepassteIfBreite, angepassteElseBreite);
		ifBreitenanteilSetzen(angepassterIfBreitenanteil);
		Specman.instance().diagrammAktualisieren(null);
		return delta;
	}

	private float ifBreitenanteil(float ifBreite, float elseBreite) {
		return ifBreite / (ifBreite + elseBreite);
	}

	private void ifBreitenanteilSetzen(float ifBreitenanteil) {
		float elseBreitenanteil = 1.0f - ifBreitenanteil;
		panelLayout.setColumnSpec(1, ColumnSpec.decode("10px:grow(" + ifBreitenanteil + ")"));
		panelLayout.setColumnSpec(3, ColumnSpec.decode("10px:grow(" + elseBreitenanteil + ")"));
		componentResized(null);
	}

	@Override
	public void setId(SchrittID id) {
		super.setId(id);
		SchrittID ifID = id.naechsteEbene();
		SchrittID elseID = id.naechsteID().naechsteEbene();
		ifSequenz.renummerieren(ifID);
		elseSequenz.renummerieren(elseID);
	}

	public SchrittID newStepIDInSameSequence(RelativeStepPosition direction) {
		return super.newStepIDInSameSequence(direction).naechsteID();
	}

	protected Point berechneRautenmittelpunkt() { //umbenannt
		return new Point(
			ifSequenz.getContainer().getWidth() + (LINIENBREITE / 2),
			ifSequenz.ueberschrift.getY() + ifSequenz.ueberschrift.getHeight());
	}

	@Override
	public void setBackgroundUDBL(Color bg) {
		super.setBackgroundUDBL(bg);

		ifSequenz.ueberschrift.setBackgroundUDBL(bg);
		elseSequenz.ueberschrift.setBackgroundUDBL(bg);

		UDBL.setBackgroundUDBL(panelIf, bg);
		UDBL.setBackgroundUDBL(panelElse, bg);
		UDBL.setBackgroundUDBL(leeresFeld, bg);
		UDBL.setBackgroundUDBL(panelBedingung, bg);

		UDBL.repaint(panel); // Damit die Raute nachgezeichnet wird
	}

	@Override
	void schrittnummerSichtbarkeitSetzen(boolean sichtbar) {
		super.schrittnummerSichtbarkeitSetzen(sichtbar);
		ifSequenz.schrittnummerSichtbarkeitSetzen(sichtbar);
		elseSequenz.schrittnummerSichtbarkeitSetzen(sichtbar);
	}


	@Override
    public List<SchrittSequenzView> unterSequenzen() {
		return sequenzenAuflisten(ifSequenz, elseSequenz);
	}

	@Override
	public void geklappt(boolean auf) {
		ifSequenz.setVisible(auf);
		elseSequenz.setVisible(auf);
	}

	@Override
	public void zusammenklappenFuerReview() {
		if (!enthaeltAenderungsmarkierungen()) {
			klappen.init(true);
		}
		ifSequenz.zusammenklappenFuerReview();
		elseSequenz.zusammenklappenFuerReview();
	}

	/** @author PVN */
	public static int spalteUmrechnen(int prozentNeu) {
		int breiteSpaltenLayout = 20*prozentNeu/100;
		return breiteSpaltenLayout;
	}

	@Override
	public void skalieren(int prozentNeu, int prozentAktuell) {
		super.skalieren(prozentNeu, prozentAktuell);
		int neueSpaltenbreite = spalteUmrechnen(prozentNeu); /** @author PVN */
		panelBedingung.setLayout(new FormLayout(neueSpaltenbreite + ", 10px:grow", "fill:pref:grow")); /**@author SD */
		panelElse.setLayout(new FormLayout(neueSpaltenbreite + ", 10px:grow", "fill:pref:grow")); /**@author SD */
		panelIf.setLayout(new FormLayout("10px:grow, " + neueSpaltenbreite, "fill:pref:grow")); /**@author SD */
		panelBedingung.add(editContainer, CC.xy(2, 1)); //siehe Konstruktor
		panelElse.add(elseSequenz.ueberschrift, CC.xy(2, 1)); //siehe Methode elseBedingungAnlegen
		panelIf.add(ifSequenz.ueberschrift, CC.xy(1,1)); //siehe Methode ifBedingungAnlegen
	}

	protected int texteinrueckungNeuberechnen() {
		return 0; /**@author PVN */
	}

	@Override
	public AbstractSchrittModel_V001 generiereModel(boolean formatierterText) {
		IfElseSchrittModel_V001 model = new IfElseSchrittModel_V001(
			id,
			getEditorContent(formatierterText),
			getBackground().getRGB(),
			getDecorated(),
			klappen.isSelected(),
			aenderungsart,
			ifSequenz.generiereZweigSchrittSequenzModel(formatierterText),
			elseSequenz.generiereZweigSchrittSequenzModel(formatierterText),
			ifBreitenanteil(ifSequenz.ueberschrift.getWidth(), elseSequenz.ueberschrift.getWidth()), getQuellschrittID());
		return model;
	}

	/**
	 * Wenn der Text etwas gr��er ist, neigen die Dreieckslinien dazu, die Textbox zu schneiden. Also
	 * rechnen wir aus, wie hoch der Kopfbereich sein muss, damit das nicht passiert. Und das geht so:
	 * <ol>
	 * <li>Wir stellen die unteren beiden Eckpunkte des Textfeldes fest
	 * <li>Wir berechnen den Winkel, der sich ergibt, wenn man eine Linie von den oberen Bereichsecken
	 *   zu den unteren Textfeldecken zieht.
	 * <li>Anschlie�end suchen wir den Punkt, in dem sich diese beiden Diagonalen schneiden, wenn man
	 *   sie nach unten verl�ngert.
	 * <li>Die H�he dieses Punktes ist die Minimalh�he des Kopfbereiches, damit die Dreieckslinie den
	 *   Text nicht schneidet.
	 * <li>Danach m�ssen wir schauen, ob dieser Punkt tief genug liegt, um If- und Else-Bedingung gen�gend
	 *   Platz zu lassen. Ist das nicht der Fall, rechnen wir die Differenz noch dazu. �ber diese
	 *   Strecke muss dann noch eine senkrechte Verbindungslinie von der Dreiecksspitze bis zur Basis des
	 *   Kofbereichs gezogen werden. Das sieht gef�lliger aus, als wenn man den Kopfbereich noch tiefer
	 *   macht.
	 * </ol>
	 */
	private void berechneHoeheFuerVollstaendigUnberuehrtenText() {

	}

	@Override
	public boolean enthaelt(InteractiveStepFragment fragment) {
		return super.enthaelt(fragment) ||
			ifSequenz.hatUeberschrift(fragment) ||
			elseSequenz.hatUeberschrift(fragment);
	}

	@Override public void resyncStepnumberStyleUDBL() {
		super.resyncStepnumberStyleUDBL();
		getElseSequenz().resyncStepnumberStyleADBL();
		getIfSequenz().resyncStepnumberStyleADBL();
	}

	@Override public void viewsNachinitialisieren() {
		super.viewsNachinitialisieren();
		elseSequenz.viewsNachinitialisieren();
		ifSequenz.viewsNachinitialisieren();
	}

	@Override public void alsGeloeschtMarkierenUDBL() {
		elseSequenz.alsGeloeschtMarkierenUDBL();
		ifSequenz.alsGeloeschtMarkierenUDBL();
		super.alsGeloeschtMarkierenUDBL();
	}

	@Override public void aenderungsmarkierungenEntfernen() {
		super.aenderungsmarkierungenEntfernen();
		elseSequenz.aenderungsmarkierungenEntfernen();
		ifSequenz.aenderungsmarkierungenEntfernen();
	}

	@Override protected int editAenderungenUebernehmen() {
		int changesMade = super.editAenderungenUebernehmen();
		changesMade += elseSequenz.ueberschriftAenderungenUebernehmen();
		changesMade += ifSequenz.ueberschriftAenderungenUebernehmen();
		return changesMade;
	}

	@Override public int aenderungenUebernehmen(EditorI editor) throws EditException {
		int changesMade = super.aenderungenUebernehmen(editor);
		changesMade += elseSequenz.aenderungenUebernehmen(editor);
		changesMade += ifSequenz.aenderungenUebernehmen(editor);
		return changesMade;
	}

	@Override protected int editAenderungenVerwerfen() {
		int changesReverted = super.editAenderungenVerwerfen();
		changesReverted += elseSequenz.ueberschriftAenderungenVerwerfen();
		changesReverted += ifSequenz.ueberschriftAenderungenVerwerfen();
		return changesReverted;
	}

	@Override public int aenderungenVerwerfen(EditorI editor) throws EditException {
		int changesReverted = super.aenderungenVerwerfen(editor);
		changesReverted += elseSequenz.aenderungenVerwerfen(editor);
		changesReverted += ifSequenz.aenderungenVerwerfen(editor);
		return changesReverted;
	}

	@Override public AbstractSchrittView findeSchrittZuId(SchrittID id) {
		return findeSchrittZuIdIncludingSubSequences(id, elseSequenz, ifSequenz);
	}

	@Override
	protected void updateTextfieldDecorationIndentions(Indentions indentions) {
		super.updateTextfieldDecorationIndentions(indentions);
		Indentions ifIndentions = indentions.withRight(false).withTop(false);
		ifSequenz.updateTextfieldDecorationIndentions(ifIndentions);
		Indentions elseIndentions = ifIndentions.withLeft(false);
		elseSequenz.updateTextfieldDecorationIndentions(elseIndentions);
	}

	public ZweigSchrittSequenzView getIfSequenz() {
        return ifSequenz;
    }

  public ZweigSchrittSequenzView getElseSequenz() {
        return elseSequenz;
    }

	@Override
	public List<JTextComponent> getTextAreas() {
		List<JTextComponent> result = super.getTextAreas();
		result.addAll(ifSequenz.getTextAreas());
		result.addAll(elseSequenz.getTextAreas());
		return result;
	}

	public List<BreakSchrittView> queryUnlinkedBreakSteps() {
		List<BreakSchrittView> result = ifSequenz.queryUnlinkedBreakSteps();
		result.addAll(elseSequenz.queryUnlinkedBreakSteps());
		return result;
	}

	@Override
	public Shape getShape() {
		return decoratedShape(new Shape(getPanel(), this)
			.withBackgroundColor(Shape.GAP_COLOR)
			.add(leeresFeld)
			.add(new Shape(panelBedingung).add(editContainer.getShape()))
			.add(new Shape(panelElse).add(elseSequenz.ueberschrift.getShape()))
			.add(new Shape(panelIf).add(ifSequenz.ueberschrift.getShape()))
			.add(ifSequenz.getShapeSequence())
			.add(elseSequenz.getShapeSequence())
			.add(createDiamond()));
	}
}
package specman.view;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import specman.ChangeInfo;
import specman.draganddrop.DragSource;
import specman.draganddrop.DropTarget;
import specman.draganddrop.LocalCursor;
import specman.EditException;
import specman.StepNumber;
import specman.SpaltenContainerI;
import specman.SpaltenResizer;
import specman.TextInit;
import specman.model.v002.EditorContentModel_V002;
import specman.model.v002.AbstractStepModel_V002;
import specman.model.v002.IfElseStepModel_V002;
import specman.pdf.Shape;
import specman.editarea.EditContainer;
import specman.editarea.Indentions;
import specman.editarea.InteractiveStepFragment;
import specman.undo.props.UDBL;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ComponentListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;


import static specman.TextInit.initialtext;
import static specman.Specman.editor;
import static specman.draganddrop.DragSource.Type.CaseBranchCreation;

public class IfElseSchrittView extends VerzweigungSchrittView implements ComponentListener, SpaltenContainerI {
	ZweigSchrittSequenzView ifSequenz;
	ZweigSchrittSequenzView elseSequenz;
	boolean mittelpunktRaute = true;
	JPanel leeresFeld;
	JPanel panelBedingung;
	JPanel panelElse;
	JPanel panelIf;

	protected IfElseSchrittView(SchrittSequenzView parent, EditorContentModel_V002 initialerText, StepNumber id, ChangeInfo changeInfo, boolean withDefaultContent) {
		super(parent, initialerText, id, changeInfo, createPanelLayout());
		initIfElsePanel(withDefaultContent ? id : null, withDefaultContent ? changeInfo : null);
	}

	protected IfElseSchrittView(SchrittSequenzView parent, specman.model.v002.EditorContentModel_V002 content, java.util.UUID stepId, ChangeInfo changeInfo) {
		super(parent, content, stepId, changeInfo, createPanelLayout());
		initIfElsePanel(null, null);
		this.id = stepId;
	}

	private void initIfElsePanel(StepNumber initialId, ChangeInfo initialChangeInfo) {
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
		if (initialId != null) {
			initIfSequenz(new ZweigSchrittSequenzView(this, initialId.naechsteEbene(), initialtext("Ja"), initialChangeInfo));
			initElseSequenz(new ZweigSchrittSequenzView(this, initialId.naechsteID().naechsteEbene(), EditContainer.right("Nein"), initialChangeInfo));
		}
	}

	public IfElseSchrittView(SchrittSequenzView parent, IfElseStepModel_V002 model) {
		this(parent, model.content, model.id, model.changeInfo != null ? model.changeInfo.toChangeInfo() : ChangeInfo.UNTRACKED);
		initIfSequenz(new ZweigSchrittSequenzView(this, model.ifSequence));
		initElseSequenz(new ZweigSchrittSequenzView(this, model.elseSequence));
		setBackgroundUDBL(new Color(model.color));
		ifBreitenanteilSetzen(model.ifWidthRatio);
		klappen.init(model.collapsed);
	}

	public IfElseSchrittView(SchrittSequenzView parent, EditorContentModel_V002 initialerText, StepNumber id, ChangeInfo changeInfo) {
		this(parent, initialerText, id, changeInfo, true);
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
		editor().diagrammAktualisieren(null);
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
	public void setNumber(StepNumber number) {
		super.setNumber(number);
		StepNumber ifID = number.naechsteEbene();
		StepNumber elseID = number.naechsteID().naechsteEbene();
		ifSequenz.renummerieren(ifID);
		elseSequenz.renummerieren(elseID);
	}

	public StepNumber newStepIDInSameSequence(RelativeStepPosition direction) {
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
		super.zusammenklappenFuerReview();
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
	public AbstractStepModel_V002 generiereModel(boolean formatierterText) {
		return new IfElseStepModel_V002(
			id,
			getEditorContent(formatierterText),
			getBackground().getRGB(),
			getDecorated(),
			klappen.isSelected(),
			changeInfo,
			ifSequenz.generiereZweigSchrittSequenzModel(formatierterText),
			elseSequenz.generiereZweigSchrittSequenzModel(formatierterText),
			ifBreitenanteil(ifSequenz.ueberschrift.getWidth(), elseSequenz.ueberschrift.getWidth()),
			null);
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

	@Override public int aenderungenUebernehmen() throws EditException {
		int changesMade = super.aenderungenUebernehmen();
		changesMade += elseSequenz.aenderungenUebernehmen();
		changesMade += ifSequenz.aenderungenUebernehmen();
		return changesMade;
	}

	@Override protected int editAenderungenVerwerfen() {
		int changesRejected = super.editAenderungenVerwerfen();
		changesRejected += elseSequenz.ueberschriftAenderungenVerwerfen();
		changesRejected += ifSequenz.ueberschriftAenderungenVerwerfen();
		return changesRejected;
	}

	@Override public int aenderungenVerwerfen() throws EditException {
		int changesRejected = super.aenderungenVerwerfen();
		changesRejected += elseSequenz.aenderungenVerwerfen();
		changesRejected += ifSequenz.aenderungenVerwerfen();
		return changesRejected;
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
	public DropTarget findHeadingDropTarget(LocalCursor localCursor, DragSource dragSource) {
		return dragSource.type() == CaseBranchCreation
			? null
			: super.findHeadingDropTarget(localCursor, dragSource);
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
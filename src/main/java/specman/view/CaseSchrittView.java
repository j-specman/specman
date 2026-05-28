package specman.view;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import specman.Aenderungsart;
import specman.ChangeInfo;
import specman.EditException;
import specman.EditorI;
import specman.SchrittID;
import specman.SpaltenResizer;
import specman.Specman;
import static specman.ChangeSet.changeset;
import specman.TextInit;
import specman.model.v001.AbstractSchrittModel_V001;
import specman.model.v001.CaseSchrittModel_V001;
import specman.model.v001.EditorContentModel_V001;
import specman.model.v001.ZweigSchrittSequenzModel_V001;
import specman.pdf.Shape;
import specman.editarea.Indentions;
import specman.editarea.InteractiveStepFragment;
import specman.undo.UndoableZweigEntfernt;
import specman.undo.props.UDBL;

import javax.swing.JPanel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static specman.TextInit.initialtext;
import static specman.graphics.Styles.BACKGROUND_COLOR_STANDARD;
import static specman.pdf.Shape.GAP_COLOR;
import static specman.Specman.editor;


public class CaseSchrittView extends VerzweigungSchrittView {
	// Alle Components kriegen am Anfang einen sinnfreien Dummy und werden dann
	// über die Methode layoutCojnstraintsSetzen korrekt plaziert. Damit verhindern
	// wir, dass die relativ komplizierte Positionsvergabe redundant im Code auftaucht
	private static final CellConstraints INITIAL_DUMMY = CC.xy(1, 1);

	ZweigSchrittSequenzView sonstSequenz;
	List<ZweigSchrittSequenzView> caseSequenzen;
	JPanel lueckenFueller; //neu
	JPanel panelCase; //neu
	JPanel panelSonst; //neu
	JPanel panelFall1; //neu

	public CaseSchrittView(SchrittSequenzView parent, EditorContentModel_V001 initialerText, SchrittID id, ChangeInfo changeInfo, int numCases) {
		super(parent, initialerText, id, changeInfo, createPanelLayout(numCases));
		panel.add(editContainer, INITIAL_DUMMY);
		/** @author PVN */
		lueckenFueller = new JPanel();
		lueckenFueller.setBackground(TextInit.schrittHintergrund());
		panel.add(lueckenFueller, CC.xy(1, 1));

		panelCase = new JPanel();
		panelCase.setBackground(TextInit.schrittHintergrund());
		panelCase.setLayout(createSpalteLinks());
		panel.add(panelCase, CC.xy(3, 1));

		panelSonst = new JPanel();
		panelSonst.setBackground(TextInit.schrittHintergrund());
		panelSonst.setLayout(createSpalteRechts());
		panel.add(panelSonst, CC.xy(1, 3));

		panelFall1 = new JPanel();
		panelFall1.setBackground(TextInit.schrittHintergrund());
		panelFall1.setLayout(createSpalteLinks());
		panel.add(panelFall1, CC.xy(3, 3));
	}

	protected void initCases(ZweigSchrittSequenzView sonstSequenz, List<ZweigSchrittSequenzView> caseSequenzen) {
		sonstFallAnlegen(sonstSequenz);
		casesAnlegen(caseSequenzen);
		layoutConstraintsSetzen();
		spaltenResizerAnlegen();
	}

	public CaseSchrittView(SchrittSequenzView parent, EditorContentModel_V001 initialerText, SchrittID id, ChangeInfo changeInfo) {
		this(parent, initialerText, id, changeInfo, 2);
		initCases(
				new ZweigSchrittSequenzView(this, id.naechsteEbene(), initialtext("Sonst"), changeInfo),
				new ArrayList<ZweigSchrittSequenzView>(Arrays.asList(
						new ZweigSchrittSequenzView(this, id.naechsteID().naechsteEbene(), initialtext("Fall 1"), changeInfo),
						new ZweigSchrittSequenzView(this, id.naechsteID().naechsteID().naechsteEbene(), initialtext("Fall 2"), changeInfo))));
	}

	private List<ZweigSchrittSequenzView> caseSequenzenAufbauen(List<ZweigSchrittSequenzModel_V001> model) {
		return model.stream()
				.map(sequenzModel -> new ZweigSchrittSequenzView(this, sequenzModel))
				.collect(Collectors.toList());
	}

	public CaseSchrittView(SchrittSequenzView parent, CaseSchrittModel_V001 model) {
		this(parent, model.inhalt, model.id, ChangeInfo.fromModel(model.changeInfo, model.aenderungsart), model.caseSequenzen.size());
		initCases(
				new ZweigSchrittSequenzView(this, model.sonstSequenz),
				caseSequenzenAufbauen(model.caseSequenzen));
		setBackgroundUDBL(new Color(model.farbe));
		spaltenbreitenAnteileSetzen(model.spaltenbreitenAnteile);
		klappen.init(model.zugeklappt);
	}

	@Override
	public void geklappt(boolean auf) {
		// It's not really clear why we have to set these component's visibility at all as the KlappButton
		// turns the layout row of all of them to 0px which should be enough. However, when recreating
		// the resizers on adding or removing a case sequence, there remains a little space even with a
		// 0px layout row.
		sonstSequenz.setVisible(auf);
		caseSequenzen.forEach(seq -> seq.setVisible(auf));
		getSpaltenResizers().forEach(resizer -> resizer.setVisible(auf));
	}

	private void spaltenResizerAnlegen() {
		for (int i = 0; i < caseSequenzen.size(); i++) {
			panel.add(new SpaltenResizer(this, i), CC.xywh(2 + 2*i, 4, 1, 2));
		}
	}

	private List<SpaltenResizer> getSpaltenResizers() {
		return Arrays.stream(panel.getComponents())
			.filter(c -> c instanceof SpaltenResizer)
			.map(c -> (SpaltenResizer)c)
			.collect(Collectors.toList());
	}

	private void spaltenResizerEntfernen() {
		List<SpaltenResizer> currentResizers = getSpaltenResizers();
		currentResizers.forEach(resizer -> panel.remove(resizer));
	}

	private static FormLayout createPanelLayout(int anzahlCases) {
		String spaltenSpec = "10px:grow, " + FORMLAYOUT_GAP + ", 10px:grow, " + FORMLAYOUT_GAP;
		for (int i = 2; i < anzahlCases; i++)
			spaltenSpec += ", 10px:grow, " + FORMLAYOUT_GAP;
		spaltenSpec += ", 10px:grow";
		return new FormLayout(spaltenSpec,
				layoutRowSpec1() + ", " + FORMLAYOUT_GAP + ", fill:pref, " + FORMLAYOUT_GAP + ", " + ZEILENLAYOUT_INHALT_SICHTBAR);
	}

	/** @author PVN */
	public static int spalteUmrechnen(int prozentNeu) {
		int breiteSpaltenLayout = 20*prozentNeu/100;
		return breiteSpaltenLayout;
	}

	@Override
	public void skalieren(int prozentNeu, int prozentAktuell) {
		super.skalieren(prozentNeu, prozentAktuell);
		panelLayout.setRowSpec(1, RowSpec.decode(layoutRowSpec1()));
		int neueSpaltenbreite = spalteUmrechnen(prozentNeu); /** @author PVN */
		panelCase.setLayout(new FormLayout(neueSpaltenbreite + ", 10px:grow", "fill:pref:grow")); /**@author SD */
		panelSonst.setLayout(new FormLayout("10px:grow, " + neueSpaltenbreite, "fill:pref:grow")); /**@author SD*/
		panelFall1.setLayout(new FormLayout(neueSpaltenbreite + ", 10px:grow", "fill:pref:grow")); /**@author SD*/
		panelCase.add(editContainer, CC.xy(2, 1)); //siehe Methode layoutConstraintsSetzen
		panelSonst.add(sonstSequenz.ueberschrift, CC.xy(1, 1)); //siehe Methode layoutConstraintsSetzen
		panelFall1.add(caseSequenzen.get(0).ueberschrift, CC.xy(2, 1)); //siehe Methode layoutConstraintsSetzen
	}

	@Override
	public SchrittID newStepIDInSameSequence(RelativeStepPosition direction) {
		SchrittID id = super.newStepIDInSameSequence(direction);
		for (int i = 0; i < caseSequenzen.size(); i++)
			id = id.naechsteID();
		return id;
	}

	@Override
	public void setId(SchrittID id) {
		super.setId(id);
		sonstSequenz.renummerieren(id.naechsteEbene());
		SchrittID naechsteId = id.naechsteID();
		for (SchrittSequenzView caseSequenz: caseSequenzen) {
			caseSequenz.renummerieren(naechsteId.naechsteEbene());
			naechsteId = naechsteId.naechsteID();
		}
	}

	protected void sonstFallAnlegen(ZweigSchrittSequenzView sonstSequenz) {
		this.sonstSequenz = sonstSequenz;
		sonstSequenz.ueberschrift.addEditAreasFocusListener(this);
		panel.add(sonstSequenz.ueberschrift, INITIAL_DUMMY);
		panel.add(sonstSequenz.getContainer(), INITIAL_DUMMY);
	}

	private void casesAnlegen(List<ZweigSchrittSequenzView> caseSequenzen) {
		this.caseSequenzen = caseSequenzen;
		for (int i = 0; i < caseSequenzen.size(); i++) {
			caseSequenzen.get(i).ueberschrift.addEditAreasFocusListener(this);
			panel.add(caseSequenzen.get(i).ueberschrift, INITIAL_DUMMY);
			panel.add(caseSequenzen.get(i).getContainer(), INITIAL_DUMMY);
		}
	}

	protected int texteinrueckungNeuberechnen() {
		return 0; /**@author PVN */
	}

	protected Point berechneRautenmittelpunkt() { //umbenannt
		return new Point(
				sonstSequenz.getContainer().getWidth(),
				sonstSequenz.ueberschrift.getY());
	}

	@Override
	public int spaltenbreitenAnpassenNachMausDragging(int delta, int spalte) {
		ArrayList<Integer> spaltenBreiten = spaltenbreitenErmitteln();
		if (spaltenBreiten.get(spalte) + delta < 0) {
			delta = -spaltenBreiten.get(spalte);
		}
		else if (spaltenBreiten.get(spalte+1) - delta < 0) {
			delta = spaltenBreiten.get(spalte+1);
		}
		spaltenBreiten.set(spalte, spaltenBreiten.get(spalte) + delta);
		spaltenBreiten.set(spalte+1, spaltenBreiten.get(spalte+1) - delta);
		List<Float> breitenAnteile = spaltenbreitenAnteileBerechnen(spaltenBreiten);
		spaltenbreitenAnteileSetzen(breitenAnteile);
		editor().diagrammAktualisieren(null);
		return delta;
	}

	private void spaltenbreitenAnteileSetzen(List<Float> breitenAnteile) {
		for (int i = 0; i < breitenAnteile.size(); i++) {
			panelLayout.setColumnSpec(1 + 2*i, ColumnSpec.decode("0px:grow(" + breitenAnteile.get(i) + ")"));
		}
	}

	private ArrayList<Integer> spaltenbreitenErmitteln() {
		ArrayList<Integer> spaltenBreiten = new ArrayList<Integer>();
		spaltenBreiten.add(sonstSequenz.getContainer().getWidth());
		caseSequenzen.forEach(sequenz -> spaltenBreiten.add(sequenz.getContainer().getWidth()));
		return spaltenBreiten;
	}

	private List<Float> spaltenbreitenAnteileBerechnen(ArrayList<Integer> spaltenBreiten) {
		int summe = spaltenBreiten.stream().mapToInt(i -> i).sum();
		final float summeInklusiveGaps = summe + caseSequenzen.size() * LINIENBREITE;
		return spaltenBreiten.stream().map(breite -> (float)breite / summeInklusiveGaps).collect(Collectors.toList());
	}

	@Override
	public void setBackgroundUDBL(Color bg) {
		super.setBackgroundUDBL(bg);
		sonstSequenz.ueberschrift.setBackgroundUDBL(bg);
		UDBL.setBackgroundUDBL(lueckenFueller, bg);
		UDBL.setBackgroundUDBL(panelCase, bg);
		UDBL.setBackgroundUDBL(panelSonst, bg);
		UDBL.setBackgroundUDBL(panelFall1, bg);
		UDBL.repaint(panel); // Damit die Linien nachgezeichnet werden
	}

	@Override
	public AbstractSchrittModel_V001 generiereModel(boolean formatierterText) {
		CaseSchrittModel_V001 model = new CaseSchrittModel_V001(
			id,
			getEditorContent(formatierterText),
			getBackground().getRGB(),
			changeInfo,
			klappen.isSelected(),
			sonstSequenz.generiereZweigSchrittSequenzModel(formatierterText),
			new ArrayList<Float>(spaltenbreitenAnteileBerechnen(spaltenbreitenErmitteln())),
			getQuellschrittID(),
			getDecorated());
		caseSequenzen.forEach(sequenz -> model.caseHinzufuegen(sequenz.generiereZweigSchrittSequenzModel(formatierterText)));
		return model;
	}

	@Override
	public List<SchrittSequenzView> unterSequenzen() {
		return sequenzenAuflisten(caseSequenzen, sonstSequenz);
	}

	@Override
	public boolean enthaelt(InteractiveStepFragment fragment) {
		return super.enthaelt(fragment) ||
			headingToBranch(fragment) != null;
	}

	public ZweigSchrittSequenzView headingToBranch(InteractiveStepFragment fragment) {
		if (sonstSequenz.hatUeberschrift(fragment))
			return sonstSequenz;
		for (ZweigSchrittSequenzView caseSequenz: caseSequenzen) {
			if (caseSequenz.hatUeberschrift(fragment))
				return caseSequenz;
		}
		return null;
	}

	public int zweigEntfernen(ZweigSchrittSequenzView zweig) {
		if (zweig == sonstSequenz) {
			System.err.println("Noch nicht fertig: Sonst-Sequenz entfernen");
			return -1;
		}
		/** @author PVN */
		if (zweig == caseSequenzen.get(0) && caseSequenzen.size() <=2 ) {
			System.err.println("Es m\u00FCssen mindestens 2 F\u00E4lle bestehen bleiben");
			return -1;
		}
		/**@author PVN */
		if (zweig == caseSequenzen.get(1) && caseSequenzen.size() <=2) {
			System.err.println("Es m\u00FCssen mindestens 2 F\u00E4lle bestehen bleiben");
			return -1;
		}
		/**@author PVN */
		if (zweig == caseSequenzen.get(0)) {
			panelFall1.remove(caseSequenzen.get(0).ueberschrift);
		}
		int caseIndex = caseSequenzen.indexOf(zweig);
		zweigAusListeUndPanelEntfernen(zweig);
		zweigAnzahlAenderungAbschliessen(spaltenbreitenErmitteln());
		return caseIndex+1; // 0 ist Indikator f�r Sonst-Zweig, ab 1 beginnen die Cases
	}

	public void zweigHinzufuegen(ZweigSchrittSequenzView zweig, int zweigIndex) {
		if (zweigIndex == 0) {
			System.err.println("Noch nicht fertig: Sonst-Sequenz hinzuf�gen");
			return;
		}
		/**@author PVN */
		if (zweigIndex == 1) {
//			System.out.println("index=1");
			panel.add(caseSequenzen.get(0).ueberschrift, CC.xy(5, 3));
		}
		ArrayList<Integer> spaltenBreiten = zweigbreiteInSpaltenbreitenEinpassen(zweig, zweigIndex);
		caseSequenzen.add(zweigIndex-1, zweig); // 0 ist Indikator f�r Sonst-Zweig, ab 1 beginnen die Cases
		panel.add(zweig.getContainer(), INITIAL_DUMMY);
		panel.add(zweig.ueberschrift, INITIAL_DUMMY);

		zweigAnzahlAenderungAbschliessen(spaltenBreiten);
	}

	private void zweigAnzahlAenderungAbschliessen(ArrayList<Integer> spaltenBreiten) {
		spaltenResizerEntfernen();
		panelLayout = createPanelLayout(caseSequenzen.size());
		panel.setLayout(panelLayout);
		layoutConstraintsSetzen();
		spaltenbreitenAnteileSetzen(spaltenbreitenAnteileBerechnen(spaltenBreiten));
		spaltenResizerAnlegen();

		setId(id);
		parent.renumberFollowingSteps(this);
		klappen.refreshGeklappt();

		editor().diagrammAktualisieren(null);
	}

	public ZweigSchrittSequenzView neuenZweigHinzufuegen(ZweigSchrittSequenzView linkerNachbar) {
		int linkerNachbarIndex = caseSequenzen.indexOf(linkerNachbar);
		ZweigSchrittSequenzView neuerZweig = new ZweigSchrittSequenzView
			(this, linkerNachbar.naechsteNachbarSequenzID(), initialtext("Fall " + (linkerNachbarIndex+2)), TextInit.initialChangeInfo());
		neuerZweig.einfachenSchrittAnhaengen();
		zweigHinzufuegen(neuerZweig, linkerNachbarIndex+2);
		if (neuerZweig == caseSequenzen.get(0)) {
			panelFall1.setBackground(TextInit.schrittHintergrund());
		}
		return neuerZweig;
	}
	private ArrayList<Integer> zweigbreiteInSpaltenbreitenEinpassen(ZweigSchrittSequenzView zweig, int zweigIndex) {
		ArrayList<Integer> spaltenBreiten = spaltenbreitenErmitteln();
		int summeSpaltenbreiten = spaltenBreiten.stream().mapToInt(i -> i).sum();
		int zweigbreite = zweig.getContainer().getWidth();
		if (zweigbreite == 0) {
			zweigbreite = summeSpaltenbreiten / (spaltenBreiten.size() + 1);
		}
		for (int i = 0; i < spaltenBreiten.size(); i++) {
			int spaltenBreite = spaltenBreiten.get(i);
			int abgabe = (int) ((float)spaltenBreite / (float)summeSpaltenbreiten * zweigbreite);
			spaltenBreiten.set(i, spaltenBreite-abgabe);
		}
		spaltenBreiten.add(zweigIndex, zweigbreite);
		return spaltenBreiten;
	}

	private void zweigAusListeUndPanelEntfernen(ZweigSchrittSequenzView zweig) {
		caseSequenzen.remove(zweig);
		panel.remove(zweig.getContainer());
		panel.remove(zweig.ueberschrift);
	}

	@Override public void resyncStepnumberStyleUDBL() {
		super.resyncStepnumberStyleUDBL();
		getSonstSequenz().resyncStepnumberStyleADBL();
		for (ZweigSchrittSequenzView caseSequenz : caseSequenzen) {
			caseSequenz.resyncStepnumberStyleADBL();
		}

	}

	@Override public void viewsNachinitialisieren() {
		super.viewsNachinitialisieren();
		sonstSequenz.viewsNachinitialisieren();
		for (ZweigSchrittSequenzView caseSequenz : caseSequenzen) {
			caseSequenz.viewsNachinitialisieren();
		}
	}

	@Override public void alsGeloeschtMarkierenUDBL() {
    EditorI editor = editor();
		ZweigSchrittSequenzView zweig = headingToBranch(editor.getLastFocusedTextArea());
		if (zweig != null) {
			if (zweig.getChangeInfo().isAdded()) {
				int zweigIndex = zweigEntfernen(zweig);
				editor().addEdit(new UndoableZweigEntfernt(zweig, this, zweigIndex));
			}

			//Markieren von Sonstsequenz und fall 1, 2 nicht ermöglichen
			else if (zweig == sonstSequenz) {
				System.err.println("Sonst-Sequenz kann nicht entfernt werden");
			}
			else if (zweig == caseSequenzen.get(0) && caseSequenzen.size() <= 2) {
				System.err.println("Es m\u00FCssen mindestens 2 F\u00E4lle bestehen bleiben");
			}
			else if (zweig == caseSequenzen.get(1) && caseSequenzen.size() <= 2) {
				System.err.println("Es m\u00FCssen mindestens 2 F\u00E4lle bestehen bleiben");
			}
			else {
				zweig.alsGeloeschtMarkierenUDBL();
				if (zweig == caseSequenzen.get(0)) {
					UDBL.setBackgroundUDBL(panelFall1, changeset().panelColor());
					UDBL.repaint(panel);
				}
			}
			return;
		}

		sonstSequenz.alsGeloeschtMarkierenUDBL();
		for (ZweigSchrittSequenzView caseSequenz : caseSequenzen) {
			caseSequenz.alsGeloeschtMarkierenUDBL();
		}
		super.alsGeloeschtMarkierenUDBL();
	}

	@Override public void aenderungsmarkierungenEntfernen() {
		super.aenderungsmarkierungenEntfernen();
		panelFall1.setBackground(BACKGROUND_COLOR_STANDARD);
		sonstSequenz.aenderungsmarkierungenEntfernen();
		for (ZweigSchrittSequenzView caseSequenz : caseSequenzen) {
			caseSequenz.aenderungsmarkierungenEntfernen();
		}
	}

	@Override public AbstractSchrittView findeSchrittZuId(SchrittID id) {
		AbstractSchrittView result = findeSchrittZuIdIncludingSubSequences(
				id, caseSequenzen.toArray(ZweigSchrittSequenzView[]::new));
		return (result != null) ? result : sonstSequenz.findeSchrittZuId(id);
	}

	private void layoutConstraintsSetzen() {
		/** @author PVN */
		panelLayout.setConstraints(lueckenFueller, CC.xy(1, 1));
		panelLayout.setConstraints(panelCase, CC.xywh(3, 1, (1 + caseSequenzen.size()*2)-2, 1));
		panelCase.add(editContainer, CC.xy(2, 1));
		panelLayout.setConstraints(panelSonst, CC.xy(1, 3));
		panelSonst.add(sonstSequenz.ueberschrift, CC.xy(1, 1));
		panelLayout.setConstraints(sonstSequenz.getContainer(), CC.xy(1, 5));
		panelLayout.setConstraints(panelFall1, CC.xy(3, 3));
		panelFall1.add(caseSequenzen.get(0).ueberschrift, CC.xy(2, 1));
		for (int i = 1; i < caseSequenzen.size(); i++) {
			panelLayout.setConstraints(caseSequenzen.get(i).ueberschrift, CC.xy(5 + (i*2)-2, 3));
		}
		for (int j = 0; j < caseSequenzen.size(); j++) {
			panelLayout.setConstraints(caseSequenzen.get(j).getContainer(), CC.xy(3 + j*2, 5));
		}
	}

	@Override
	protected void updateTextfieldDecorationIndentions(Indentions indentions) {
		super.updateTextfieldDecorationIndentions(indentions);
		Indentions defaultCaseIndentions = indentions.withRight(false).withTop(false);
		sonstSequenz.updateTextfieldDecorationIndentions(defaultCaseIndentions);
		Indentions caseIndentions = defaultCaseIndentions.withLeft(false);
		caseSequenzen
			.stream()
			.forEach(caseSequence -> caseSequence.updateTextfieldDecorationIndentions(caseIndentions));
	}

	//TODO
	public ZweigSchrittSequenzView getSonstSequenz() {
		return sonstSequenz;
	}

	//TODO
	public List<ZweigSchrittSequenzView> getCaseSequenzen() {
		return caseSequenzen;
	}
	//TODO
	public int getRautenHeight() {
		return berechneRautenmittelpunkt().y;
	}

	public JPanel getPanelFall1() {
		return panelFall1;
	}

	@Override protected int editAenderungenUebernehmen() {
		int changesMade = super.editAenderungenUebernehmen();
		changesMade += sonstSequenz.ueberschriftAenderungenUebernehmen();
		for (ZweigSchrittSequenzView caseSequenz : caseSequenzen) {
			changesMade += caseSequenz.ueberschriftAenderungenUebernehmen();
		}
		return changesMade;
	}

	@Override public int aenderungenUebernehmen() throws EditException {
		panelFall1.setBackground(BACKGROUND_COLOR_STANDARD);
		setBackgroundUDBL(BACKGROUND_COLOR_STANDARD);
		int changesMade = sonstSequenz.aenderungenUebernehmen();
		List<ZweigSchrittSequenzView> caseSequenzen = new CopyOnWriteArrayList<ZweigSchrittSequenzView>(this.caseSequenzen);
		for (ZweigSchrittSequenzView caseSequenz : caseSequenzen) {
			if (caseSequenz.getChangeInfo().isDeleted()) {
				zweigEntfernen(caseSequenz);
				changesMade++;
			}
			else {
				changesMade += caseSequenz.aenderungenUebernehmen();
			}
		}
		changesMade += super.aenderungenUebernehmen();
		return changesMade;
	}

	@Override protected int editAenderungenVerwerfen() {
		int changesRejected = super.editAenderungenVerwerfen();
		changesRejected += sonstSequenz.ueberschriftAenderungenVerwerfen();
		for (ZweigSchrittSequenzView caseSequenz : caseSequenzen) {
			changesRejected += caseSequenz.ueberschriftAenderungenVerwerfen();
		}
		return changesRejected;
	}

	@Override public int aenderungenVerwerfen() throws EditException {
		//Wir spiegeln die Liste einmal auf eine CopyOnWriteArrayList um zweige während des durchlaufens bearbeiten zu können
		List<ZweigSchrittSequenzView> caseSequenzen = new CopyOnWriteArrayList<ZweigSchrittSequenzView>(this.caseSequenzen);
		int changesRejected = sonstSequenz.aenderungenVerwerfen();
		for (ZweigSchrittSequenzView caseSequenz : caseSequenzen) {
			if(caseSequenz.getChangeInfo().isAdded()) {
				changesRejected += zweigEntfernen(caseSequenz);
			}
			else {
				changesRejected += caseSequenz.aenderungenVerwerfen();
			}
		}
		changesRejected += super.aenderungenVerwerfen();
		return changesRejected;
	}

	@Override
	public List<JTextComponent> getTextAreas() {
		List<JTextComponent> result = super.getTextAreas();
		result.addAll(sonstSequenz.getTextAreas());
		caseSequenzen.forEach(seq -> result.addAll(seq.getTextAreas()));
		return result;
	}

	public List<BreakSchrittView> queryUnlinkedBreakSteps() {
		List<BreakSchrittView> result = sonstSequenz.queryUnlinkedBreakSteps();
		caseSequenzen.forEach(seq -> result.addAll(seq.queryUnlinkedBreakSteps()));
		return result;
	}

	@Override
	public Shape getShape() {
		Shape shape = new Shape(getPanel())
			.withBackgroundColor(GAP_COLOR)
			.add(lueckenFueller)
			.add(new Shape(panelCase).add(editContainer.getShape()))
			.add(new Shape(panelSonst).add(sonstSequenz.ueberschrift.getShape()))
			.add(new Shape(panelFall1).add(caseSequenzen.get(0).ueberschrift.getShape()))
			.add(caseSequenzen.get(0).getShapeSequence())
			.add(sonstSequenz.getShapeSequence());
		for (ZweigSchrittSequenzView caseSequenz : caseSequenzen.subList(1, caseSequenzen.size())) {
			shape
				.add(caseSequenz.getShapeSequence())
				.add(caseSequenz.ueberschrift.getShape());
		}
		return decoratedShape(shape.add(createDiamond()));
	}
}
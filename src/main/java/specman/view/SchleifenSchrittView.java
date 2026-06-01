package specman.view;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import specman.ChangeInfo;
import specman.EditException;
import specman.EditorI;
import specman.SchrittID;
import specman.SpaltenContainerI;
import specman.SpaltenResizer;
import specman.Specman;
import specman.TextInit;
import specman.model.v001.AbstractSchrittModel_V001;
import specman.model.v001.EditorContentModel_V001;
import specman.model.v001.WhileSchrittModel_V001;
import specman.pdf.Shape;
import specman.editarea.Indentions;
import specman.undo.props.UDBL;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.event.ComponentEvent;
import java.util.List;

import static specman.graphics.Styles.DIAGRAMM_LINE_COLOR;
import static specman.pdf.Shape.GAP_COLOR;
import static specman.Specman.editor;

public class SchleifenSchrittView extends AbstractSchrittView implements SpaltenContainerI {
  private static final int CONTENTROW = 3;

	final JPanel panel;
	final JPanel linkerBalken;
	final JPanel untererBalken;
  final BottomFiller filler;
	final KlappButton klappen;
	final FormLayout layout;
	SchrittSequenzView wiederholSequenz;
	int balkenbreite;

	public SchleifenSchrittView(SchrittSequenzView parent, EditorContentModel_V001 initialerText, SchrittID id, ChangeInfo changeInfo, boolean mitUnteremBalken) {
		super(parent, initialerText, id, changeInfo);
		panel = new JPanel();
		panel.setBackground(DIAGRAMM_LINE_COLOR);
		balkenbreite = SPALTENLAYOUT_UMGEHUNG_GROESSE;
		layout = new FormLayout(
				umgehungLayout(balkenbreite) + ", " + FORMLAYOUT_GAP + ", 10dlu:grow",
				"fill:pref, " + FORMLAYOUT_GAP + ", " + ZEILENLAYOUT_INHALT_SICHTBAR);
		panel.setLayout(layout);

		panel.add(editContainer, CC.xywh(2, 1, 2, 1));

		linkerBalken = new JPanel();
		linkerBalken.setLayout(null);
		linkerBalken.setBackground(TextInit.schrittHintergrund());

		if (mitUnteremBalken) {
			layout.appendRow(RowSpec.decode(FORMLAYOUT_GAP));
			layout.appendRow(RowSpec.decode(umgehungLayout()));
			panel.add(linkerBalken, CC.xywh(1, 1, 1, 4));
			untererBalken = new JPanel();
			untererBalken.setLayout(null);
			untererBalken.setBackground(TextInit.schrittHintergrund());
			panel.add(untererBalken, CC.xywh(1, 5, 3, 1));
		}
		else {
			panel.add(linkerBalken, CC.xywh(1, 1, 1, 3));
			untererBalken = null;
		}

    panel.addComponentListener(this);
		panel.add(new SpaltenResizer(this), CC.xy(2, 3));

    filler = new BottomFiller(panel, layout, changeInfo);
    klappen = new KlappButton(this, editContainer.getKlappButtonParent(), layout, CONTENTROW, filler.row);
	}

	public SchleifenSchrittView(SchrittSequenzView parent, WhileSchrittModel_V001 model, boolean mitUnteremBalken) {
		this(parent, model.inhalt, model.id, ChangeInfo.fromModel(model.changeInfo, model.aenderungsart), mitUnteremBalken);
		initWiederholsequenzFromModel(model);
	}

	protected void initWiederholsequenzFromModel(WhileSchrittModel_V001 model) {
		initWiederholsequenz(new SchrittSequenzView(this, model.wiederholSequenz));
		setBackgroundUDBL(new Color(model.farbe));
		balkenbreiteSetzen(model.balkenbreite);
		klappen.init(model.zugeklappt);
	}

	protected void initWiederholsequenz(SchrittSequenzView wiederholSequenz) {
		this.wiederholSequenz = wiederholSequenz;
		panel.add(wiederholSequenz.getContainer(), CC.xy(3, 3));
	}

	@Override
	public int spaltenbreitenAnpassenNachMausDragging(int delta, int spalte) {
		int angepassteBalkenBreite = linkerBalken.getWidth() + delta;
		balkenbreiteSetzen(angepassteBalkenBreite);
		editor().diagrammAktualisieren(null);
		return delta;
	}

	private void balkenbreiteSetzen(int balkenbreite) {
		this.balkenbreite = balkenbreite;
		layout.setColumnSpec(1, ColumnSpec.decode(balkenbreite + "px"));
	}

	protected SchrittSequenzView einschrittigeInitialsequenz(SchrittID id, ChangeInfo changeInfo) {
		SchrittSequenzView sequenz = new SchrittSequenzView(this, id, changeInfo);
		sequenz.einfachenSchrittAnhaengen();
		return sequenz;
	}

	@Override
	public void setId(SchrittID id) {
		super.setId(id);
		wiederholSequenz.renummerieren(id.naechsteEbene());
	}

	@Override
	public void setBackgroundUDBL(Color bg) {
		super.setBackgroundUDBL(bg);
		UDBL.setBackgroundUDBL(linkerBalken, bg);
    UDBL.setBackgroundUDBL(filler, bg);
		if (untererBalken != null) {
			UDBL.setBackgroundUDBL(untererBalken, bg);
		}
	}

	@Override
	public JComponent getDecoratedComponent() { return decorated(panel); }

	@Override
	public boolean isStrukturiert() {
		return true;
	}

	@Override
	void schrittnummerSichtbarkeitSetzen(boolean sichtbar) {
		super.schrittnummerSichtbarkeitSetzen(sichtbar);
		wiederholSequenz.schrittnummerSichtbarkeitSetzen(sichtbar);
	}

	public SchrittSequenzView getSequenz() {
		return wiederholSequenz;
	}


	@Override
	public List<SchrittSequenzView> unterSequenzen() {
		return sequenzenAuflisten(wiederholSequenz);
	}

	@Override
	public void zusammenklappenFuerReview() {
		if (!enthaeltAenderungsmarkierungen()) {
			klappen.init(true);
		}
		super.zusammenklappenFuerReview();
	}

public void skalieren(int prozentNeu, int prozentAktuell) {
		super.skalieren(prozentNeu, prozentAktuell);
		int neueBalkenbreite = groesseUmrechnen(balkenbreite, prozentNeu, prozentAktuell);
		balkenbreiteSetzen(neueBalkenbreite);
		if (untererBalken != null) {
			String unterBalkenLayout = untererBalken.isVisible() ? umgehungLayout() : ZEILENLAYOUT_INHALT_VERBORGEN;
			layout.setRowSpec(5, RowSpec.decode(unterBalkenLayout));
		}
		klappen.scale(prozentNeu, prozentAktuell);
	}

	@Override
	public void geklappt(boolean auf) {
		wiederholSequenz.setVisible(auf);
		if (untererBalken != null) {
			untererBalken.setVisible(auf);
			String unterBalkenLayout = auf ? umgehungLayout() : ZEILENLAYOUT_INHALT_VERBORGEN;
			layout.setRowSpec(5, RowSpec.decode(unterBalkenLayout));
		}
	}

	@Override
	public AbstractSchrittModel_V001 generiereModel(boolean formatierterText) {
		WhileSchrittModel_V001 model = new WhileSchrittModel_V001(
			id,
			getEditorContent(formatierterText),
			getBackground().getRGB(),
			changeInfo,
			klappen.isSelected(),
			wiederholSequenz.generiereSchrittSequenzModel(formatierterText),
			0,
			getQuellschrittID(),
			getDecorated());
		return model;
	}

	@Override public int aenderungenUebernehmen() throws EditException {
		int changesMade = super.aenderungenUebernehmen();
		changesMade += wiederholSequenz.aenderungenUebernehmen();
		return changesMade;
	}

	@Override public int aenderungenVerwerfen() throws EditException {
		int changesRejected = super.aenderungenVerwerfen();
		changesRejected += wiederholSequenz.aenderungenVerwerfen();
		return changesRejected;
	}

@Override
	protected void updateTextfieldDecorationIndentions(Indentions indentions) {
		super.updateTextfieldDecorationIndentions(indentions.withLeft(false));
		// Subsequence does not need consideration because the loop panel forms
		// an additional border shielding the inner steps from any rounded border
		// decorarions outside.
	}

	public SchrittSequenzView getWiederholSequenz() {
		return wiederholSequenz;
	}

	public JPanel getPanel() {
		return panel;
	}
	public JPanel getLinkerBalken(){
		return linkerBalken;
	}
	public JPanel getUntererBalken(){
		return untererBalken;
	}

	@Override
	public void componentResized(ComponentEvent e) {
		super.componentResized(e);
		klappen.updateLocation(editContainer.getStepNumberBounds());
	}

	@Override
	public List<JTextComponent> getTextAreas() {
		List<JTextComponent> result = super.getTextAreas();
		result.addAll(wiederholSequenz.getTextAreas());
		return result;
	}

	public List<BreakSchrittView> queryUnlinkedBreakSteps() {
		return wiederholSequenz.queryUnlinkedBreakSteps();
	}

	@Override
	public Shape getShape() {
		return super
			.getShape()
			.withBackgroundColor(GAP_COLOR)
			.add(linkerBalken)
			.add(untererBalken)
			.add(wiederholSequenz.getShapeSequence());
	}

  // Switching off sub-numbering is not yet supported for loops
  public Boolean getFlatNumbering() { return null; }

}
package specman.view;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import specman.ChangeInfo;
import specman.EditException;
import specman.StepNumber;
import specman.draganddrop.DragSource;
import specman.draganddrop.DropTarget;
import specman.draganddrop.LocalCursor;
import specman.SpaltenContainerI;
import specman.SpaltenResizer;
import specman.TextInit;
import specman.draganddrop.UnsupportedDragSourceException;
import specman.model.v002.EditorContentModel_V002;
import specman.model.v002.WhileStepModel_V002;

import java.util.UUID;
import specman.pdf.Shape;
import specman.editarea.Indentions;
import specman.undo.props.UDBL;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.util.List;

import static specman.draganddrop.DragSource.Type.StepCreation;
import static specman.draganddrop.DragSource.Type.StepMove;
import static specman.graphics.Styles.DIAGRAMM_LINE_COLOR;
import static specman.pdf.Shape.GAP_COLOR;
import static specman.Specman.editor;
import static specman.view.RelativeStepPosition.After;

public class SchleifenSchrittView extends AbstractSchrittView implements SpaltenContainerI {
  private static final int CONTENTROW = 3;

	JPanel panel;
	JPanel linkerBalken;
	JPanel untererBalken;
	BottomFiller filler;
	KlappButton klappen;
	FormLayout layout;
	SchrittSequenzView wiederholSequenz;
	int balkenbreite;

	public SchleifenSchrittView(SchrittSequenzView parent, EditorContentModel_V002 initialerText, StepNumber id, ChangeInfo changeInfo, boolean mitUnteremBalken) {
		super(parent, initialerText, id, changeInfo);
		initPanelAndLayout(mitUnteremBalken);
	}

	private SchleifenSchrittView(SchrittSequenzView parent, EditorContentModel_V002 content, UUID stepId, ChangeInfo changeInfo, boolean mitUnteremBalken) {
		super(parent, content, stepId, changeInfo);
		initPanelAndLayout(mitUnteremBalken);
	}

	private void initPanelAndLayout(boolean mitUnteremBalken) {
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

	protected SchleifenSchrittView(SchrittSequenzView parent, WhileStepModel_V002 model, boolean mitUnteremBalken) {
		this(parent, model.content, model.id, model.changeInfo != null ? model.changeInfo.toChangeInfo() : ChangeInfo.UNTRACKED, mitUnteremBalken);
		initWiederholsequenzFromModel(model);
	}

	protected void initWiederholsequenzFromModel(WhileStepModel_V002 model) {
		initWiederholsequenz(new SchrittSequenzView(this, model.loopSequence));
		setBackgroundUDBL(new Color(model.color));
		balkenbreiteSetzen(model.barWidth);
		klappen.init(model.collapsed);
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

	protected SchrittSequenzView einschrittigeInitialsequenz(StepNumber id, ChangeInfo changeInfo) {
		SchrittSequenzView sequenz = new SchrittSequenzView(this, id, changeInfo);
		sequenz.einfachenSchrittAnhaengen();
		return sequenz;
	}

	@Override
	public void setNumber(StepNumber number) {
		super.setNumber(number);
		wiederholSequenz.renummerieren(number.naechsteEbene());
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
	public WhileStepModel_V002 generiereModel(boolean formatierterText) {
		return new WhileStepModel_V002(
			id,
			getEditorContent(formatierterText),
			getBackground().getRGB(),
			changeInfo,
			klappen.isSelected(),
			wiederholSequenz.generiereSchrittSequenzModel(formatierterText),
			0,
			null,
			getDecorated());
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
	public DropTarget findDropTarget(LocalCursor localCursor, DragSource dragSource) throws UnsupportedDragSourceException {
		dragSource.supported(StepMove, StepCreation);
		if (localCursor.isInAny(linkerBalken, untererBalken)) {
			return new DropTarget(getParent(), this, After);
		}
		// Cursor on the loop text header: insert Before the first step in the loop body
		if (localCursor.isIn(getTextShef())) {
			return new DropTarget(wiederholSequenz);
		}
		return null;
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
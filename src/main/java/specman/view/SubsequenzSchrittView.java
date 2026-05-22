package specman.view;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import specman.*;
import specman.model.v001.AbstractSchrittModel_V001;
import specman.model.v001.EditorContentModel_V001;
import specman.model.v001.SubsequenzSchrittModel_V001;
import specman.pdf.Shape;
import specman.editarea.Indentions;
import specman.undo.props.UDBL;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.event.ComponentEvent;
import java.util.List;

import static specman.styles.Styles.DIAGRAMM_LINE_COLOR;
import static specman.view.KlappButton.ZEILENLAYOUT_FILLER_HIDDEN;

public class SubsequenzSchrittView extends AbstractSchrittView {
	public static final int TEXTEINRUECKUNG = 18;
  private static final int CONTENTROW = 3;

	final JPanel panel;
  final BottomFiller filler;
	final KlappButton klappen;
	final FormLayout layout;
	SchrittSequenzView subsequenz;
  /** flat numbering means: the steps within this sub-sequence are not numbered on a lower level than this step itself
   * as it is usual in Specman. E.g. the steps in a sub-sequence step with number 2.3 have numbers 2.3.1, 2.3.2, and so on.
   * With flat numbering, the sub steps get numbers 2.4, 2.5, and so on. As a consequence, the numbers of steps following this
   * sub-sequence step on the same level get numbers depending on the sub-sequence's size. That's not so nice, but on the
   * other hand switching off the sub-numbering save a numbering level. Which variant is better depends on the situation. */
  boolean flatNumbering;

	protected SubsequenzSchrittView(EditorI editor, SchrittSequenzView parent, EditorContentModel_V001 initialerText, SchrittID id, Aenderungsart aenderungsart, boolean withDefaultContent) {
		super(editor, parent, initialerText, id, aenderungsart);

		editContainer.updateDecorationIndentions(new Indentions(TEXTEINRUECKUNG));

		panel = new JPanel();
		panel.setBackground(DIAGRAMM_LINE_COLOR);
		layout = new FormLayout("10dlu:grow",
				"fill:pref, " + FORMLAYOUT_GAP + ", " + ZEILENLAYOUT_INHALT_SICHTBAR);
		panel.setLayout(layout);

		panel.add(editContainer, CC.xy(1, 1));

    filler = new BottomFiller(panel, layout, aenderungsart);
		klappen = new KlappButton(this, editContainer.getKlappButtonParent(), layout, CONTENTROW, filler.row);

		if (withDefaultContent) {
      initSubsequenz(einschrittigeInitialsequenz(editor, id.naechsteEbene()), false);
		}
	}

  @Override
  public void setBackgroundUDBL(Color bg) {
    super.setBackgroundUDBL(bg);
    UDBL.setBackgroundUDBL(filler, bg);
  }

  public SubsequenzSchrittView(EditorI editor, SchrittSequenzView parent, EditorContentModel_V001 initialerText, SchrittID id, Aenderungsart aenderungsart) {
		this(editor, parent, initialerText, id, aenderungsart, true);
	}

	public SubsequenzSchrittView(EditorI editor, SchrittSequenzView parent, SubsequenzSchrittModel_V001 model) {
		this(editor, parent, model.inhalt, model.id, model.aenderungsart, false);
		initSubsequenz(new SchrittSequenzView(editor, this, model.subsequenz), model.flatNumbering);
		setBackgroundUDBL(new Color(model.farbe));
		klappen.init(model.zugeklappt);
	}

	private SchrittSequenzView einschrittigeInitialsequenz(EditorI editor, SchrittID id) {
		SchrittSequenzView sequenz = new SchrittSequenzView(this, id);
		sequenz.einfachenSchrittAnhaengen(editor);
		return sequenz;
	}

	protected void initSubsequenz(SchrittSequenzView subsequenz, boolean flatNumbering) {
		this.subsequenz = subsequenz;
    this.flatNumbering = flatNumbering;
		panel.add(subsequenz.getContainer(), CC.xy(1, CONTENTROW));
	}

	@Override
	public void setId(SchrittID id) {
		super.setId(id);
    renumberSubsequence();
	}

	@Override
	public JComponent getDecoratedComponent() { return decorated(panel); }

	@Override
	public boolean isStrukturiert() {
		return true;
	}

	public SchrittSequenzView getSequenz() {
		return subsequenz;
	}

	@Override
	public List<SchrittSequenzView> unterSequenzen() {
		return sequenzenAuflisten(subsequenz);
	}

	@Override
	public void zusammenklappenFuerReview() {
		if (!enthaeltAenderungsmarkierungen()) {
			klappen.init(true);
		}
		subsequenz.zusammenklappenFuerReview();
	}

	@Override
	public void skalieren(int prozent, int prozentAktuell) {
		super.skalieren(prozent, prozentAktuell);
		klappen.scale(prozent, prozentAktuell);
	}

	@Override
	public void geklappt(boolean auf) {
		subsequenz.setVisible(auf);
	}

	@Override
	public AbstractSchrittModel_V001 generiereModel(boolean formatierterText) {
		SubsequenzSchrittModel_V001 model = new SubsequenzSchrittModel_V001(
			id,
			getEditorContent(formatierterText),
			getBackground().getRGB(),
			aenderungsart,
			klappen.isSelected(),
			subsequenz.generiereSchrittSequenzModel(formatierterText),
			getQuellschrittID(),
			getDecorated(),
      flatNumbering);
		return model;
	}

	@Override public void resyncStepnumberStyleUDBL() {
		super.resyncStepnumberStyleUDBL();
		subsequenz.resyncStepnumberStyleUDBL();
	}

	@Override public void viewsNachinitialisieren() {
		super.viewsNachinitialisieren();
		subsequenz.viewsNachinitialisieren();
	}

	@Override public AbstractSchrittView findeSchrittZuId(SchrittID id) {
		return findeSchrittZuIdIncludingSubSequences(id, subsequenz);
	}

	@Override public int aenderungenUebernehmen(EditorI editor) throws EditException {
		int changesMade = super.aenderungenUebernehmen(editor);
		changesMade += subsequenz.aenderungenUebernehmen(editor);
		return changesMade;
	}

	@Override public int aenderungenVerwerfen(EditorI editor) throws EditException {
		int changesReverted = super.aenderungenVerwerfen(editor);
		changesReverted += subsequenz.aenderungenVerwerfen(editor);
		return changesReverted;
	}

	@Override public void alsGeloeschtMarkierenUDBL() {
		subsequenz.alsGeloeschtMarkierenUDBL();
		super.alsGeloeschtMarkierenUDBL();
	}

	protected void updateTextfieldDecorationIndentions(Indentions indentions) {
		super.updateTextfieldDecorationIndentions(indentions);
		Indentions substepIndentions = indentions.withTop(false).withRight(false);
		subsequenz.updateTextfieldDecorationIndentions(substepIndentions);
	}

	public JPanel getPanel() { return panel; }

	public SchrittSequenzView getSubsequenz() {
		return subsequenz;
	}

	@Override
	public void componentResized(ComponentEvent e) {
		super.componentResized(e);
		klappen.updateLocation(editContainer.getStepNumberBounds());
	}

	@Override
	public List<JTextComponent> getTextAreas() {
		List<JTextComponent> result = super.getTextAreas();
		result.addAll(subsequenz.getTextAreas());
		return result;
	}

	public List<BreakSchrittView> queryUnlinkedBreakSteps() {
		return subsequenz.queryUnlinkedBreakSteps();
	}

	@Override
	public Shape getShape() {
		return super.getShape()
			.withBackgroundColor(panel.getBackground())
			.add(subsequenz.getShapeSequence())
      .add(filler);
	}

  public Boolean getFlatNumbering() { return flatNumbering; }

  @Override
  public void toggleFlatNumbering(boolean flatNumbering) {
    this.flatNumbering = flatNumbering;
    renumberSubsequence();
    getParent().renumberFollowingSteps(this);

    // Required to ensure repaint and thus width resizing of all effected step number labels
    Specman.instance().diagrammAktualisieren(null);
  }

  private void renumberSubsequence() {
    if (flatNumbering) {
      subsequenz.renummerieren(this.id.sameID());
    } else {
      subsequenz.renummerieren(this.id.naechsteEbene());
    }
  }

  public SchrittID newStepIDInSameSequence(RelativeStepPosition direction) {
    // What about flatNumbering combined with Before? Anything special to do?
    // Up to now I just can't find a horse foot in that.
    if (direction == RelativeStepPosition.Before || !flatNumbering) {
      return super.newStepIDInSameSequence(direction);
    }
    AbstractSchrittView lastStep = subsequenz.getLastStep();
    return lastStep.getId().naechsteID();
  }

  @Override
  /** If this sub-sequence step uses flat numbering, any insertion or removal of steps
   * in its sub-sequence effects the numbers of following steps of this step itself. */
  public void renumberFollowingSteps(SchrittSequenzView modifiedSubsequence) {
    if (flatNumbering) {
      getParent().renumberFollowingSteps(this);
    }
  }
}

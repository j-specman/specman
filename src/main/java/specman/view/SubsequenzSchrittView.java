package specman.view;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import specman.*;
import specman.draganddrop.DragSource;
import specman.draganddrop.DropTarget;
import specman.draganddrop.LocalCursor;
import specman.draganddrop.UnsupportedDragSourceException;
import specman.model.v002.EditorContentModel_V002;
import specman.model.v002.SubsequenceStepModel_V002;
import specman.pdf.Shape;
import specman.editarea.Indentions;
import specman.undo.props.UDBL;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.event.ComponentEvent;
import java.util.List;

import static specman.draganddrop.DragSource.Type.StepCreation;
import static specman.draganddrop.DragSource.Type.StepMove;
import static specman.graphics.Styles.DIAGRAMM_LINE_COLOR;
import static specman.Specman.editor;
import static specman.view.RelativeStepPosition.Before;

public class SubsequenzSchrittView extends AbstractSchrittView {
	public static final int TEXTEINRUECKUNG = 18;
  private static final int CONTENTROW = 3;

	JPanel panel;
	BottomFiller filler;
	KlappButton klappen;
	FormLayout layout;
	SchrittSequenzView subsequenz;
  /** flat numbering means: the steps within this sub-sequence are not numbered on a lower level than this step itself
   * as it is usual in Specman. E.g. the steps in a sub-sequence step with number 2.3 have numbers 2.3.1, 2.3.2, and so on.
   * With flat numbering, the sub steps get numbers 2.4, 2.5, and so on. As a consequence, the numbers of steps following this
   * sub-sequence step on the same level get numbers depending on the sub-sequence's size. That's not so nice, but on the
   * other hand switching off the sub-numbering save a numbering level. Which variant is better depends on the situation. */
  boolean flatNumbering;

	protected SubsequenzSchrittView(SchrittSequenzView parent, EditorContentModel_V002 initialerText, StepNumber id, ChangeInfo changeInfo, boolean withDefaultContent) {
		super(parent, initialerText, id, changeInfo);
		initSubsequenzPanel();
		if (withDefaultContent) {
			initSubsequenz(einschrittigeInitialsequenz(id.naechsteEbene(), changeInfo), false);
		}
	}

	private SubsequenzSchrittView(SchrittSequenzView parent, specman.model.v002.EditorContentModel_V002 content, java.util.UUID stepId, ChangeInfo changeInfo) {
		super(parent, content, stepId, changeInfo);
		initSubsequenzPanel();
	}

	private void initSubsequenzPanel() {
		editContainer.updateDecorationIndentions(new Indentions(TEXTEINRUECKUNG));

		panel = new JPanel();
		panel.setBackground(DIAGRAMM_LINE_COLOR);
		layout = new FormLayout("10dlu:grow",
				"fill:pref, " + FORMLAYOUT_GAP + ", " + ZEILENLAYOUT_INHALT_SICHTBAR);
		panel.setLayout(layout);

		panel.add(editContainer, CC.xy(1, 1));

		filler = new BottomFiller(panel, layout, changeInfo);
		klappen = new KlappButton(this, editContainer.getKlappButtonParent(), layout, CONTENTROW, filler.row);
	}

	public SubsequenzSchrittView(SchrittSequenzView parent, SubsequenceStepModel_V002 model) {
		this(parent, model.content, model.id, model.changeInfo != null ? model.changeInfo.toChangeInfo() : ChangeInfo.UNTRACKED);
		initSubsequenz(new SchrittSequenzView(this, model.subsequence), model.flatNumbering);
		setBackgroundUDBL(new Color(model.color));
		klappen.init(model.collapsed);
		this.id = model.id;
	}

  @Override
  public void setBackgroundUDBL(Color bg) {
    super.setBackgroundUDBL(bg);
    UDBL.setBackgroundUDBL(filler, bg);
  }

  public SubsequenzSchrittView(SchrittSequenzView parent, EditorContentModel_V002 initialerText, StepNumber id, ChangeInfo changeInfo) {
		this(parent, initialerText, id, changeInfo, true);
	}

	private SchrittSequenzView einschrittigeInitialsequenz(StepNumber id, ChangeInfo changeInfo) {
		SchrittSequenzView sequenz = new SchrittSequenzView(this, id, changeInfo);
		sequenz.einfachenSchrittAnhaengen();
		return sequenz;
	}

	protected void initSubsequenz(SchrittSequenzView subsequenz, boolean flatNumbering) {
		this.subsequenz = subsequenz;
    this.flatNumbering = flatNumbering;
		panel.add(subsequenz.getContainer(), CC.xy(1, CONTENTROW));
	}

	@Override
	public void setNumber(StepNumber number) {
		super.setNumber(number);
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
		super.zusammenklappenFuerReview();
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
	public SubsequenceStepModel_V002 generiereModel(boolean formatierterText) {
		return new SubsequenceStepModel_V002(
			id,
			getEditorContent(formatierterText),
			getBackground().getRGB(),
			changeInfo,
			klappen.isSelected(),
			subsequenz.generiereSchrittSequenzModel(formatierterText),
			null,
			getDecorated(),
			flatNumbering);
	}

	@Override public int aenderungenUebernehmen() throws EditException {
		int changesMade = super.aenderungenUebernehmen();
		changesMade += subsequenz.aenderungenUebernehmen();
		return changesMade;
	}

	@Override public int aenderungenVerwerfen() throws EditException {
		int changesRejected = super.aenderungenVerwerfen();
		changesRejected += subsequenz.aenderungenVerwerfen();
		return changesRejected;
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
	public DropTarget findDropTarget(LocalCursor localCursor, DragSource dragSource) throws UnsupportedDragSourceException {
		dragSource.supported(StepMove, StepCreation);
		// Cursor on the subsequence text header: insert Before the first body step
		if (localCursor.isIn(getTextShef())) {
			return new DropTarget(subsequenz);
		}
		return null;
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
    editor().diagrammAktualisieren(null);
  }

  private void renumberSubsequence() {
    if (flatNumbering) {
      subsequenz.renummerieren(this.number.sameID());
    } else {
      subsequenz.renummerieren(this.number.naechsteEbene());
    }
  }

  public StepNumber newStepIDInSameSequence(RelativeStepPosition direction) {
    // What about flatNumbering combined with Before? Anything special to do?
    // Up to now I just can't find a horse foot in that.
    if (direction == Before || !flatNumbering) {
      return super.newStepIDInSameSequence(direction);
    }
    AbstractSchrittView lastStep = subsequenz.getLastStep();
    return lastStep.getNumber().naechsteID();
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

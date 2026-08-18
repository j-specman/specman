package specman.view;

import specman.TextInit;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;

import specman.*;
import specman.model.v002.EditorContentModel_V002;
import specman.model.v002.BreakStepModel_V002;
import specman.pdf.LineShape;
import specman.pdf.Shape;
import specman.undo.props.UDBL;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static specman.view.StepRemovalPurpose.Discard;
import static specman.Specman.editor;

public class BreakSchrittView extends AbstractSchrittView {
	
	JPanel panel;
	FormLayout layout;
	CatchUeberschrift catchHeading;

	public BreakSchrittView(SchrittSequenzView parent, EditorContentModel_V002 content, StepNumber id, ChangeInfo changeInfo) {
		super(parent, content, id, changeInfo);
		initPanel();
	}

	private void initPanel() {
		panel = new JPanel() {
			@Override
			public void paint(Graphics g) {
				super.paint(g);
				dreieckZeichnen((Graphics2D)g);
			}
		};
		panel.setBackground(TextInit.schrittHintergrund());
		layout = new FormLayout(
				umgehungLayout() + ", 10dlu:grow",
				"fill:pref, " + AbstractSchrittView.ZEILENLAYOUT_INHALT_SICHTBAR);
		panel.setLayout(layout);
		panel.add(editContainer, CC.xy(2, 1));
	}

	public BreakSchrittView(SchrittSequenzView parent, BreakStepModel_V002 model) {
		super(parent, model.content, model.id, model.changeInfo != null ? model.changeInfo.toChangeInfo() : ChangeInfo.UNTRACKED);
		initPanel();
		setBackgroundUDBL(new Color(model.color));
		this.id = model.id;
	}

	private void dreieckZeichnen(Graphics2D g) {
		g.setStroke(new BasicStroke(1.5f));
		g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING, 
                RenderingHints.VALUE_ANTIALIAS_ON);
		for (LineShape line: buildTriangle()) {
			g.drawLine(line.start().x, line.start().y, line.end().x, line.end().y);
		}
	}

	private java.util.List<LineShape> buildTriangle() {
		List<LineShape> triangle = new ArrayList<>();
		int hoehe = editContainer.getHeight();
		int dreieckSpitzeY = hoehe / 2;
		int dreieckBasisX = editContainer.getX() - LINIENBREITE;
		triangle.add(new LineShape(dreieckBasisX,  0,  0,  dreieckSpitzeY));
		triangle.add(new LineShape(0,  dreieckSpitzeY, dreieckBasisX, hoehe));
		// If the step is higher than the edit container, we add an extra line below the
		// edit container. This is especially of interest for solitaire break steps in case
		// or if/else steps making up complete sequence while other sequences need a lot of
		// space. The line avoids a strange-looking open triangle.
		if (hoehe < panel.getHeight()) {
			triangle.add(new LineShape(dreieckBasisX, hoehe, panel.getWidth(), hoehe));
		}
		return triangle;
	}

	@Override
	public void setBackgroundUDBL(Color bg) {
		super.setBackgroundUDBL(bg);
		UDBL.setBackgroundUDBL(panel, bg);
	}

	@Override
	public void componentResized(ComponentEvent e) {
		super.componentResized(e);
		// Following call is required to repaint the triangle on size change of the edit container
		panel.repaint();
	}

	@Override
	public JComponent getDecoratedComponent(){
		return decorated(panel);
	}

	@Override
	public BreakStepModel_V002 generiereModel(boolean formatierterText) {
		return new BreakStepModel_V002(
			id,
			getEditorContent(formatierterText),
			getBackground().getRGB(),
			changeInfo,
			null,
			getDecorated()
		);
	}

	public void catchAnkoppeln(CatchUeberschrift catchHeading) {
		this.catchHeading = catchHeading;
	}

	@Override
	public void focusLost(FocusEvent e) {
		if (catchHeading != null && changeInfo.art() != Aenderungsart.Geloescht) {
      try(ScrollPause sp = editor().pauseScrolling()) {
        catchHeading.updateFromBreakStepContent();
      }
		}
	}

	@Override
	public void setNumber(StepNumber number) {
		super.setNumber(number);
		if (catchHeading != null) {
			catchHeading.setId(number);
		}
	}

	@Override
	public void entfernen(SchrittSequenzView container, StepRemovalPurpose purpose) {
		super.entfernen(container, purpose);
		if (purpose == Discard && catchHeading != null) {
      catchHeading.remove();
		}
	}

	@Override
	public void alsGeloeschtMarkierenUDBL() {
		super.alsGeloeschtMarkierenUDBL();
		if (catchHeading != null) {
      catchHeading.removeOrMarkAsDeletedUDBL();
		}
	}

	public void skalieren(int prozentNeu, int prozentAktuell) {
		super.skalieren(prozentNeu, prozentAktuell);
		layout.setColumnSpec(1, ColumnSpec.decode(umgehungLayout()));
	}

	@Override
	public JPanel getPanel() { return panel; }

	@Override
	public Shape getShape() {
		return super.getShape()
			.add(buildTriangle());
	}

	@Override
	public List<BreakSchrittView> queryUnlinkedBreakSteps() {
		return (catchHeading == null) ? Arrays.asList(this) : Arrays.asList();
	}

	public void updateContent(EditorContentModel_V002 content, ChangeSet sourceChangeSet) {
		editContainer.setEditorContent(content);
		ChangeSet breakStepChangeSet = changeInfo.changeSet();
		if (sourceChangeSet != null && breakStepChangeSet != null && sourceChangeSet != breakStepChangeSet) {
			editContainer.mergeChangeSetUDBL(breakStepChangeSet, sourceChangeSet, false);
		}
	}

  public boolean refersToOtherStep() { return catchHeading != null; }

  public void scrollToCatch() {
    if (catchHeading != null) {
      // The user might not have focussed anything in the break step before he scrolled to
      // the catch sequence - so in case he want's to scroll back by CTRL+ALT+Left, we
      // explicitly add the break step to the edit history here.
      editor().appendToEditHistory(editContainer);
      catchHeading.scrollTo();
    }
  }
}

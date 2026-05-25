package specman.view;

import specman.TextInit;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;

import specman.*;
import specman.model.v001.BreakSchrittModel_V001;
import specman.model.v001.AbstractSchrittModel_V001;
import specman.model.v001.EditorContentModel_V001;
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

public class BreakSchrittView extends AbstractSchrittView {
	
	final JPanel panel;
	final FormLayout layout;
	CatchUeberschrift catchHeading;

	public BreakSchrittView(EditorI editor, SchrittSequenzView parent, EditorContentModel_V001 content, SchrittID id, ChangeInfo changeInfo) {
		super(editor, parent, content, id, changeInfo);
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

	public BreakSchrittView(EditorI editor, SchrittSequenzView parent, BreakSchrittModel_V001 model) {
		this(editor, parent, model.inhalt, model.id, ChangeInfo.fromModel(model.changeInfo, model.aenderungsart));
		setBackgroundUDBL(new Color(model.farbe));
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
	public AbstractSchrittModel_V001 generiereModel(boolean formatierterText) {
		BreakSchrittModel_V001 model = new BreakSchrittModel_V001(
			id,
			getEditorContent(formatierterText),
			getBackground().getRGB(),
			changeInfo,
			getQuellschrittID(),
			getDecorated()
		);
		return model;
	}

	public void catchAnkoppeln(CatchUeberschrift catchHeading) {
		this.catchHeading = catchHeading;
	}

	@Override
	public void focusLost(FocusEvent e) {
		if (catchHeading != null && changeInfo.art() != Aenderungsart.Geloescht) {
      try(ScrollPause sp = Specman.instance().pauseScrolling()) {
        catchHeading.updateFromBreakStepContent();
      }
		}
	}

	@Override
	public void setId(SchrittID id) {
		super.setId(id);
		if (catchHeading != null) {
			catchHeading.setId(id);
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

	public void updateContent(EditorContentModel_V001 content) {
		editContainer.setEditorContent(content);
	}

  public boolean refersToOtherStep() { return catchHeading != null; }

  public void scrollToCatch() {
    if (catchHeading != null) {
      // The user might not have focussed anything in the break step before he scrolled to
      // the catch sequence - so in case he want's to scroll back by CTRL+ALT+Left, we
      // explicitly add the break step to the edit history here.
      Specman.instance().appendToEditHistory(editContainer);
      catchHeading.scrollTo();
    }
  }
}

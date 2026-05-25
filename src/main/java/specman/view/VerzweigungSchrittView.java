package specman.view;

import com.jgoodies.forms.layout.FormLayout;
import specman.Aenderungsart;
import specman.ChangeInfo;
import specman.EditorI;
import specman.SchrittID;
import specman.SpaltenContainerI;
import specman.Specman;
import specman.model.v001.EditorContentModel_V001;
import specman.pdf.Shape;
import specman.undo.props.UDBL;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;

import static specman.graphics.Styles.BACKGROUND_COLOR_STANDARD;
import static specman.graphics.Styles.DIAGRAMM_LINE_COLOR;

/** Basisklasse für If, If/Else und Case */
abstract public class VerzweigungSchrittView extends AbstractSchrittView implements SpaltenContainerI {
  protected static final int CONTENTROW = 5;

	final JPanel panel;
  final BottomFiller filler;
	final KlappButton klappen;
	FormLayout panelLayout;

	public VerzweigungSchrittView(EditorI editor, SchrittSequenzView parent, EditorContentModel_V001 initialerText, SchrittID id, ChangeInfo changeInfo, FormLayout panelLayout) {
		super(editor, parent, initialerText, id, changeInfo);
		this.panelLayout = panelLayout;
		panel = new JPanel() {
			@Override
			public void paint(Graphics g) {
				super.paint(g);
				rauteZeichnen((Graphics2D)g);
				restoreDecorationGraphics(g);
			}
		};

		panel.setBackground(DIAGRAMM_LINE_COLOR);
		panel.setLayout(panelLayout);
		panel.addComponentListener(this);
		panel.setEnabled(false);

    filler = new BottomFiller(panel, panelLayout, changeInfo.art());
    klappen = new KlappButton(this, editContainer.getKlappButtonParent(), panelLayout, CONTENTROW, filler.row);
		klappen.addComponentListener(new ComponentAdapter() {
			// Kleine Sch�nheitsgeschichte: Der Klapp-Button liegt �ber der linken Dreieckslinie.
			// Wenn der Button durch Mausbewegungen verschwindet, m�ssen wir daf�r sorgen, das dort,
			// wo der Button war, die Linie nachgezogen wird. Sonst bleibt da eine h�ssliche wei�e
			// L�cke stehen.
			@Override public void componentHidden(ComponentEvent e) {
				panel.repaint();
			}
		});
	}

  @Override
  public void setBackgroundUDBL(Color bg) {
    super.setBackgroundUDBL(bg);
    UDBL.setBackgroundUDBL(filler, bg);
  }

  /** If the step is placed into a {@link RoundedBorderDecorator}, painting the step
	 * sometimes causes a corruption of the rounded corners. So we repaint the decorator
	 * afterwards. However: we can't just call its repaint method which causes a
	 * <i>recursive</i> repaint and thus an endless loop. So we only run a decoration
	 * redraw with an appropriate translated graphics context. */
	private void restoreDecorationGraphics(Graphics g) {
		if (roundedBorderDecorator != null) {
			g.translate(-panel.getX(), -panel.getY());
			roundedBorderDecorator.drawDecoration(g);
		}
	}

	protected static String layoutRowSpec1() {
		int aktuellerZoomfaktor = Specman.instance().getZoomFactor();
		return "fill:[" + (1 * aktuellerZoomfaktor / 100) + "dlu,pref]";
	}

	@Override
	public JComponent getDecoratedComponent() { return decorated(panel); }

	@Override
	public boolean isStrukturiert() {
		return true;
	}

	protected void initialeSchritteAnhaengen(EditorI editor) {
		unterSequenzen().forEach(sequenz -> sequenz.einfachenSchrittAnhaengen(editor));
	}

	@Override
	public void componentResized(ComponentEvent e) {
		super.componentResized(e);
		klappen.updateLocation(editContainer.getStepNumberBounds());
	}

	@Override
	public void focusLost(FocusEvent e) {
		super.focusLost(e);
		panel.repaint(); // Zeichnet Dreieck und Case-Trenner nach, wenn man mit Editieren der Texte fertig ist
	}

	/** @author PVN */
	public static FormLayout createSpalteLinks() {
		return new FormLayout(breiteLayoutspalteBerechnen() + ", 10px:grow", "fill:pref:grow");
	}

	/** @author PVN */
	public static FormLayout createSpalteRechts() {
		return new FormLayout("10px:grow, " + breiteLayoutspalteBerechnen(), "fill:pref:grow");
	}

	/** @author PVN */
	public static double breiteLayoutspalteBerechnen() {
		double breiteSpaltenLayout = 20*Specman.instance().getZoomFactor()/100;
		return breiteSpaltenLayout;
	}

	protected void rauteZeichnen(Graphics2D g) { //umbenannt
		Shape diamond = createDiamond();

		g.setRenderingHint(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);

		//inner white diamond, filled
		int[] polygonX = diamond.xPositionsAsArray();
		int[] polygonY = diamond.yPositionsAsArray();
		g.setColor(BACKGROUND_COLOR_STANDARD);
		g.fillPolygon(polygonX, polygonY, polygonX.length);

		//outer black diamond, not filled
		g.setStroke(new BasicStroke(LINIENBREITE));
		g.setColor(DIAGRAMM_LINE_COLOR);
		g.drawPolygon(polygonX, polygonY, polygonX.length);
	}

	protected specman.pdf.Shape createDiamond() {
		Point mittelpunktRaute = berechneRautenmittelpunkt(); //umbenannt
		int layoutSpaltenbreite = (int)breiteLayoutspalteBerechnen();
		int editContainerHeight = editContainer.getHeight();
		return new Shape()
			.withOutline(true)
			.add(mittelpunktRaute.x - layoutSpaltenbreite, editContainerHeight)
			.add(mittelpunktRaute.x, editContainerHeight - layoutSpaltenbreite)
			.add(mittelpunktRaute.x + layoutSpaltenbreite, editContainerHeight)
			.add(mittelpunktRaute.x, editContainerHeight + layoutSpaltenbreite);
	}

	abstract protected Point berechneRautenmittelpunkt(); //umbenannt

	abstract protected int texteinrueckungNeuberechnen();

	public JPanel getPanel() { return panel; }

	public void skalieren(int prozentNeu, int prozentAktuell) {
		super.skalieren(prozentNeu, prozentAktuell);
		klappen.scale(prozentNeu, prozentAktuell);
	}

  // Switching off sub-numbering is not yet supported for branching steps
  public Boolean getFlatNumbering() { return null; }

}
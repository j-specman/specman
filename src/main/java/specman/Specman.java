package specman;

import com.formdev.flatlaf.FlatLightLaf;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import net.atlanticbb.tantlinger.shef.HTMLEditorPane;
import specman.draganddrop.DragMouseAdapter;
import specman.draganddrop.GlassPane;
import specman.editarea.EditArea;
import specman.editarea.InteractiveStepFragment;
import specman.graphics.IconReader;
import specman.model.v001.*;
import specman.editarea.EditContainer;
import specman.editarea.TextEditArea;
import specman.undo.UndoableSchrittHinzugefuegt;
import specman.undo.manager.SpecmanUndoManager;
import specman.undo.manager.UndoRecording;
import specman.undo.manager.UndoRecordingMode;
import specman.view.*;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static specman.graphics.Styles.DIAGRAMM_LINE_COLOR;

public class Specman extends JFrame implements EditorI, SpaltenContainerI, SpecmanOpContext {
	private static final BasicStroke GESTRICHELTE_LINIE =
			new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND, 1.0f, new float[] {10.0f, 10.0f }, 0f);
	public static String SPECMAN_TITLE = "Specman " + SpecmanVersion.getVersion();

	TextEditArea lastFocusedTextArea;
	private SchrittSequenzView hauptSequenz;
	JPanel contentPane;
	WorkingAreaPanel arbeitsbereich;
	JPanel hauptSequenzContainer;
	SpaltenResizer breitenAnpasser;
	JScrollPane scrollPane;
  PausableViewport viewport;
	EditContainer intro, outro;
	int diagrammbreite = WorkingAreaPanel.INITIAL_DIAGRAMM_WIDTH;
	int zoomFaktor = 100;
	File diagrammDatei;
  FocusHistory focusHistory = new FocusHistory();

	private DiagramToolBar diagramToolBar;
	private StepButtonBar stepButtonBar;
	private SpecmanMenuBar menuBar;

	private KeyboardSpecmanOp keyboardOp;
	private final ExportPDFSpecmanOp exportPDFOp = new ExportPDFSpecmanOp(this);

	private static Specman instance;

	public Specman(File fileToOpen) throws Exception {
		instance = this;
		setApplicationIcon();
		setTitle(SPECMAN_TITLE);

		undoManager = new SpecmanUndoManager(this);

		initComponents();

		initShefController();

		hauptSequenz = new SchrittSequenzView();

		scrollPane = new JScrollPane();
    viewport = new PausableViewport();
    scrollPane.setViewport(viewport);
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(20);
		scrollPane.addMouseWheelListener(createDragMouseAdapter());
		contentPane.add(scrollPane, CC.xy(2, 3));

		arbeitsbereich = new WorkingAreaPanel();

		intro = new EditContainer(this);
		intro.setOpaque(false);
		arbeitsbereich.add(intro, CC.xy(2, 2));

		outro = new EditContainer(this);
		outro.setOpaque(false);
		arbeitsbereich.add(outro, CC.xy(2, 4));

		scrollPane.setViewportView(arbeitsbereich);
		setInitialWindowSizeAndScreenCenteredLocation();
		setVisible(true);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);

		// Falls jemand nicht aufgepasst hat und beim Initialisieren irgendwelche Funktionen verwendet hat,
		// die schon etwas im Undo-Manager hinterlassen.
		undoManager.discardAllEdits();

		this.setGlassPane(new GlassPane(SwingUtilities.convertPoint(contentPane, 0, 0,this).y, getJMenuBar().getHeight()));

		configureKeyboardManager();
		setupQuestionDialogWhenClosingWithoutSaving();

		openInitialFile(fileToOpen);
	}

  private void openInitialFile(File fileToOpen) {
		if (fileToOpen != null) {
			// Das Laden des Diagramms muss hier verzögert erfolgen, nachdem der initiale Layout-Zyklus gelaufen ist.
			// Andernfalls kommt es zu Problemen, wenn man die geöffnete Datei gar nicht bearbeitet, sondern nur direkt
			// einen PDF-Export vornimmt. Der Export übernimmt nämlich das Layout aus dem UI, und wenn das noch nicht
			// vollständig aufgebaut ist, wird der Export Murks (abgeschnittene Texte, zu kleine Boxen...)
			SwingUtilities.invokeLater(() -> diagrammLaden(fileToOpen));
		}
	}

	private void setInitialWindowSizeAndScreenCenteredLocation() {
		setSize(1100, 700);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int w = this.getSize().width;
		int h = this.getSize().height;
		int x = (dim.width-w)/2;
		int y = (dim.height-h)/2;
		this.setLocation(x, y);
	}

	/**
	 * Sets up a KeyEventDispatcher to provide a list of pressed keys used by another container.
	 */
	private void configureKeyboardManager() {
		keyboardOp = new KeyboardSpecmanOp(this);
		keyboardOp.register();
	}

	public void dropWelcomeMessage() {
		if (arbeitsbereich.dropWelcomeMessage()) {
			hauptSequenzInitialisieren();
		}
	}

	private void setApplicationIcon() {
		setIconImage(IconReader.readImageIcon("specman").getImage());
	}

	private void setupQuestionDialogWhenClosingWithoutSaving() {
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (undoManager.hasUnsavedChanges()) {
					int dialogResult = JOptionPane.showConfirmDialog(Specman.instance,
							"Änderungen am Dokument '" + getDiagramFilename() + "' vor dem Schließen speichern?" +
									"\nIhre Änderungen gehen verloren, wenn Sie diese nicht speichern.",
							"Diagramm speichern?", JOptionPane.YES_NO_CANCEL_OPTION);

					if (dialogResult == JOptionPane.CANCEL_OPTION) { // Prevent closing
						return;
					}
					else if (dialogResult == JOptionPane.YES_OPTION) { // Save & Close
						diagrammSpeichern(false);
					}
				}
				dispose();
				System.exit(0);
			}
		});
	}

	private String getDiagramFilename() {
		if (diagrammDatei != null) {
			return diagrammDatei.getName();
		}
		return "Unbekannt";
	}

	@Override
	public int spaltenbreitenAnpassenNachMausDragging(int delta, int spalte) {
		diagrammbreite += delta;
		diagrammbreiteSetzen(diagrammbreite);
		diagrammAktualisieren(null);
		arbeitsbereich.showDragLine(null);
		return delta;
	}

	@Override
	public void vertikalLinieSetzen(int x, SpaltenResizer spaltenResizer) {
		Integer dragX = null;
		if (spaltenResizer != null) {
			Point relativePosition = SwingUtilities.convertPoint(spaltenResizer, new Point(x, 0), arbeitsbereich);
			dragX = (int)relativePosition.getX();
		}
		arbeitsbereich.showDragLine(dragX);
	}

	void hauptSequenzInitialisieren() {
		if (hauptSequenzContainer != null) {
			arbeitsbereich.remove(hauptSequenzContainer);
		}
		else {
			breitenAnpasser = new SpaltenResizer(this);
			breitenAnpasser.setBackground(DIAGRAMM_LINE_COLOR);
			breitenAnpasser.setOpaque(true);
			arbeitsbereich.add(breitenAnpasser, CC.xy(3, 3));
		}
		hauptSequenzContainer = hauptSequenz.getContainer();
		// Rundherum schwarze Linie au�er rechts. Da kommt stattdessen der breitenAnpasser hin
		hauptSequenzContainer.setBorder(new MatteBorder(
			AbstractSchrittView.LINIENBREITE,
			AbstractSchrittView.LINIENBREITE,
			AbstractSchrittView.LINIENBREITE,
			0,
			DIAGRAMM_LINE_COLOR));
		arbeitsbereich.add(hauptSequenzContainer, CC.xy(2, 3));
		diagrammAktualisieren(null);
	}

	@Override public void focusGained(FocusEvent e) {
		setLastFocusedTextArea(e);
    if (e.getSource() instanceof EditArea<?>) {
      focusHistory.append(((EditArea<?>)e.getSource()).getParent());
    }
	}

	@Override public void focusLost(FocusEvent e) {
		setLastFocusedTextArea(e);
	}

	private void setLastFocusedTextArea(FocusEvent e) {
		if (e.getSource() instanceof TextEditArea) {
			setLastFocusedTextArea((TextEditArea) e.getSource());
		}
	}

	@Override
	public void setLastFocusedTextArea(TextEditArea area) {
		lastFocusedTextArea = area;
  }

	void setDiagrammDatei(File diagrammDatei) {
		this.diagrammDatei = diagrammDatei;
		setTitle(getDiagramFilename() + " - "+ SPECMAN_TITLE);
	}

	void fehler(String text) {
		JOptionPane.showMessageDialog(this, text);
	}

  @Override
	public void resyncStepnumberStyleADBL() {
    hauptSequenz.resyncStepnumberStyleADBL();
	}

	public void addImageViaFileChooser() {
		new AddImageSpecmanOp(this).addViaFileChooser();
	}

	public void addTable(int columns, int rows) {
		if (lastFocusedTextArea != null) {
			EditArea nextFocusArea = lastFocusedTextArea.addTable(columns, rows, TextInit.initialArt());
			diagrammAktualisieren(nextFocusArea);
		}
	}

	@Override
	public void toggleListItem(boolean ordered) {
		if (lastFocusedTextArea != null) {
			EditArea nextFocusArea = lastFocusedTextArea.toggleListItemUDBL(ordered, TextInit.initialArt());
			diagrammAktualisieren(nextFocusArea);
		}
	}

	public int skalieren(int prozent) {
		int bisherigerFaktor = zoomFaktor;
		zoomFaktor = prozent;
		zoomFaktorAnzeigeAktualisieren(prozent);
    KlappButton.scaleIcons(prozent, bisherigerFaktor);
		float diagrammbreite100Prozent = (float)diagrammbreite / bisherigerFaktor * 100;
		int neueDiagrammbreite = (int)(diagrammbreite100Prozent * prozent / 100);
		spaltenbreitenAnpassenNachMausDragging(neueDiagrammbreite - diagrammbreite, 0);
		hauptSequenz.skalieren(prozent, bisherigerFaktor);
		intro.skalieren(prozent, bisherigerFaktor);
		outro.skalieren(prozent, bisherigerFaktor);
		return bisherigerFaktor;
	}

	void zoomFaktorAnzeigeAktualisieren(int prozent) {
		diagramToolBar.updateZoomDisplay(prozent);
	}

	void diagrammSpeichern(boolean dateiauswahlErzwingen) {
		new SaveDiagrammSpecmanOp(this).speichern(dateiauswahlErzwingen);
	}

	void diagrammLaden() {
		new LoadDiagrammSpecmanOp(this).laden();
	}

	public void diagrammLaden(File diagramFile) {
		new LoadDiagrammSpecmanOp(this).laden(diagramFile);
	}

	private void diagrammbreiteSetzen(int breite) {
		arbeitsbereich.diagrammbreiteSetzen(breite);
	}

	public void diagrammAktualisieren(EditArea editArea) {
		// Null-Abfrage ist für den Fall, dass der User etwas im Intro oder Outro macht,
		// bevor er überhaupt das Diagramm angefangen hat. Sollte man später noch mal
		// bereinigen, dass das gar nicht geht, solange die Welcome Message noch angezeigt wird.
		if (hauptSequenzContainer != null) {
			hauptSequenzContainer.setVisible(false);
			// Folgende Zeile forciert ein Relayouting, falls z.B. nur eine manuelle Breitenänderung
			// einer If-Else-Spaltenteilung stattgefunden hat.
			diagrammbreiteSetzen(diagrammbreite-1);
			final Point viewPosition = scrollPane.getViewport().getViewPosition();
			SwingUtilities.invokeLater(() -> {
				diagrammbreiteSetzen(diagrammbreite);
				hauptSequenzContainer.setVisible(true);
				if (editArea != null) {
					editArea.requestFocus();
				}
				scrollPane.getViewport().setViewPosition(viewPosition);
			});
		}
	}

	public void newStepPostInit(AbstractSchrittView newStep) {
    resyncStepnumberStyleADBL();
		addEdit(new UndoableSchrittHinzugefuegt(newStep, newStep.getParent()));
		newStep.skalieren(zoomFaktor, 100);
		newStep.initInheritedTextFieldIndentions();
		diagrammAktualisieren(newStep.getFirstEditArea());
	}

	private void initComponents() {
		diagramToolBar = new DiagramToolBar(this);
		stepButtonBar = new StepButtonBar(this);

		contentPane = new JPanel();
		setContentPane(contentPane);
		contentPane.setLayout(new FormLayout("pref, default:grow", "default, default, fill:10px:grow")); //ToDo Sidebar added "pref"

		contentPane.add(diagramToolBar, CC.xywh(1, 1, 2, 1));
		contentPane.add(stepButtonBar, CC.xy(1, 3));
		pack();
		setLocationRelativeTo(getOwner());
	}

	HTMLEditorPane shefEditorPane;
	SpecmanUndoManager undoManager;

	@Override
	public void instrumentWysEditor(JEditorPane ed, String initialText, Integer orientation) {
		shefEditorPane.instrumentWysEditor(ed, initialText, orientation);
	}

  private void initShefController() throws Exception {
		shefEditorPane = new HTMLEditorPane(undoManager);
		menuBar = new SpecmanMenuBar(this, shefEditorPane);
		setJMenuBar(menuBar);
		contentPane.add(shefEditorPane.getFormatToolBar(), CC.xywh(1, 2, 2, 1));
	}

	@Override
	public void addEdit(UndoableEdit edit) {
    	undoManager.addEdit(edit);
	}

	public static EditorI instance() { return instance; }


	public boolean aenderungenVerfolgen() {
		return diagramToolBar.isChangeModeEnabled();
	}

	public void setChangeModeEnabled(boolean enabled) {
		diagramToolBar.setChangeModeEnabled(enabled);
	}

	void addRecentFile(File file) {
		menuBar.addRecentFile(file);
	}

	void resetPdfExportChooser() {
		exportPDFOp.resetChooser();
	}

	void setPdfExportOptions(PDFExportOptionsModel_V001 options) {
		exportPDFOp.setPdfExportOptions(options);
	}

	PDFExportOptionsModel_V001 getPdfExportOptions() {
		return exportPDFOp.getPdfExportOptions();
	}

	public static void main(String[] args) throws Exception {
    setLookAndFeel();
		File initialFileToOpen = readFileFromArgs(args);
		new Specman(initialFileToOpen);
	}

  private static void setLookAndFeel() {
    try {
      UIManager.setLookAndFeel(new FlatLightLaf());
    } catch (UnsupportedLookAndFeelException ex) {
      System.err.println("Failed to initialize LaF");
    }
  }

  private static File readFileFromArgs(String[] args) {
		if (args.length > 0) {
			File file = new File(args[0]);
			if (file.exists()) {
				return file;
			}
		}
		return null;
	}

	public void exportAsGraphviz() {
		SchrittSequenzModel_V001 model = hauptSequenz.generiereSchrittSequenzModel(false);
		try {
			new GraphvizExporter("export.gv").export(model);
		}
		catch(IOException iox) {
      displayException(iox);
		}
	}

	public void exportAsPDF() {
		exportPDFOp.export();
	}

	@Override public int getZoomFactor() {
		return zoomFaktor;
	}

	public SchrittSequenzView getHauptSequenz() {
		return hauptSequenz;
	}

	public void setHauptSequenz(SchrittSequenzView hauptSequenz) {
		this.hauptSequenz = hauptSequenz;
	}

	public SpecmanUndoManager getUndoManager() {
		return undoManager;
	}

	@Override
	public DragMouseAdapter createDragMouseAdapter() {
		return new DragMouseAdapter(this, stepButtonBar);
	}

	@Override public TextEditArea getLastFocusedTextArea() {
		return lastFocusedTextArea;
	}

	public void showError(EditException ex) {
		fehler(ex.getMessage());
	}

	public double scale(double length) {
		return length * getZoomFactor() * 0.01;
	}

	public UndoRecording pauseUndo() {
		return new UndoRecording(this.getUndoManager(), UndoRecordingMode.Paused);
	}

	public UndoRecording composeUndo() {
		return new UndoRecording(this.getUndoManager(), UndoRecordingMode.Composing);
	}

	@Override
	public List<AbstractSchrittView> listAllSteps() {
		return getHauptSequenz().listSteps();
	}

	/**
	 * Finds a step by their StepID and throws an exception if it doesn't exist.
	 */
	@Override
	public AbstractSchrittView findStepByStepID(String stepID) {
		AbstractSchrittView result = getHauptSequenz().findStepByStepID(stepID);
		if (result == null) {
			throw new RuntimeException("Could not find stepnumber '" + stepID + "'."
					+ " Make sure not to search for an outdated stepnumber.");
		}
		return result;
	}

	public boolean isKeyPressed(int keyCode) {
		return keyboardOp.isKeyPressed(keyCode);
	}

	@Override
	public AbstractSchrittView findeSchritt(TextEditArea textEditArea) {
		return hauptSequenz.findeSchritt(textEditArea);
	}

	@Override
	public List<JTextComponent> queryAllTextComponents(JTextComponent tc) {
		List<JTextComponent> result = new ArrayList<>();
		result.addAll(intro.getTextAreas());
		result.addAll(hauptSequenz.getTextAreas());
		result.addAll(outro.getTextAreas());
		return result;
	}

  @Override
  public ScrollPause pauseScrolling() {
    return new ScrollPause(viewport);
  }

  void displayException(Exception x) {
    x.printStackTrace();
    displayErrorMessage(x.getMessage());
  }

  private void displayErrorMessage(String message) {
    JOptionPane.showMessageDialog(this, message, "Fehler", JOptionPane.ERROR_MESSAGE);
  }

  @Override
  public AbstractSchrittView findStep(InteractiveStepFragment fragment) {
    return hauptSequenz.findeSchritt(fragment);
  }

  @Override
  public void scrollBackwardInEditHistory() {
    scrollToHistoricContainer(focusHistory.navigateBack());
  }

  @Override
  public void scrollForwardInEditHistory() {
    scrollToHistoricContainer(focusHistory.navigateForward());
  }

  private void scrollToHistoricContainer(EditContainer editContainer) {
    if (editContainer != null &&
      lastFocusedTextArea != null &&
      editContainer != lastFocusedTextArea.getParent()) {
      editContainer.scrollTo();
    }
  }

  @Override
  public void appendToEditHistory(EditContainer editContainer) {
    focusHistory.append(editContainer);
  }

  @Override
  public void deleteStepADBL(AbstractSchrittView step, InteractiveStepFragment initiatingFragment) {
    new DeleteStepADBLOp(this, step, initiatingFragment).run();
  }

  @Override
  public void moveBranchSequenceLeftADBL(AbstractSchrittView step, InteractiveStepFragment initiatingFragment) {
    new MoveBranchSequenceLeftADBLOp(this, step, initiatingFragment).run();
  }

  @Override
  public void moveBranchSequenceRightADBL(AbstractSchrittView step, InteractiveStepFragment initiatingFragment) {
    new MoveBranchSequenceRightADBLOp(this, step, initiatingFragment).run();
  }

  @Override
  public int showConfirmDialog(String message, String title, int optionType) {
    return JOptionPane.showConfirmDialog(this, message, title, optionType);
  }

}

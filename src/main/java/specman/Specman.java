package specman;

import com.formdev.flatlaf.FlatLightLaf;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import net.atlanticbb.tantlinger.shef.HTMLEditorPane;
import specman.draganddrop.DragMouseAdapter;
import specman.draganddrop.GlassPane;
import specman.editarea.EditArea;
import specman.editarea.InteractiveStepFragment;
import specman.model.v001.*;
import specman.pdf.PDFExportChooser;
import specman.editarea.EditContainer;
import specman.editarea.TextEditArea;
import specman.undo.UndoableDiagrammSkaliert;
import specman.undo.UndoableSchrittHinzugefuegt;
import specman.undo.manager.SpecmanUndoManager;
import specman.undo.manager.UndoRecording;
import specman.undo.manager.UndoRecordingMode;
import specman.view.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import specman.styles.ChangeColorSet;
import static specman.styles.Styles.AENDERUNGSFARBE;
import static specman.styles.Styles.CHANGESETS;
import static specman.styles.Styles.DIAGRAMM_LINE_COLOR;

public class Specman extends JFrame implements EditorI, SpaltenContainerI {
	public static final int INITIAL_DIAGRAMM_WIDTH = 700;
	private static final BasicStroke GESTRICHELTE_LINIE =
			new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND, 1.0f, new float[] {10.0f, 10.0f }, 0f);

	TextEditArea lastFocusedTextArea;
	public SchrittSequenzView hauptSequenz;
	JPanel contentPane;
	JPanel arbeitsbereich;
	JPanel hauptSequenzContainer;
	SpaltenResizer breitenAnpasser;
	JScrollPane scrollPane;
  PausableViewport viewport;
	EditContainer intro, outro;
	FormLayout hauptlayout;
	int diagrammbreite = INITIAL_DIAGRAMM_WIDTH;
	int zoomFaktor = 100;
	Integer dragX;
	File diagrammDatei;
	List<AbstractSchrittView> postInitSchritte;
	RecentFiles recentFiles;
	private JComponent welcomeMessage;
	PDFExportChooser pdfExportChooser;
  PDFExportOptionsModel_V001 pdfExportOptions;
  FocusHistory focusHistory = new FocusHistory();

	//TODO window for dragging
	public final JWindow window = new JWindow();
	private KeyboardSpecmanOp keyboardOp;
	public static String SPECMAN_TITLE = "Specman " + SpecmanVersion.getVersion();

	public Specman(File fileToOpen) throws Exception {
		instance = this;
		setApplicationIcon();
		setTitle(SPECMAN_TITLE);

		recentFiles = new RecentFiles(this);
		undoManager = new SpecmanUndoManager(this);

		initComponents();

		initShefController();

		hauptSequenz = new SchrittSequenzView();

		scrollPane = new JScrollPane();
    viewport = new PausableViewport();
    scrollPane.setViewport(viewport);
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(20);
		scrollPane.addMouseWheelListener(new DragMouseAdapter(this));
		contentPane.add(scrollPane, CC.xy(2, 3));

		arbeitsbereich = new JPanel() {
			@Override
			public void paint(Graphics g) {
				super.paint(g);
				if (dragX != null) {
					Graphics2D g2 = (Graphics2D)g;
					g2.setStroke(GESTRICHELTE_LINIE);
					g.drawLine(dragX, 0, dragX, arbeitsbereich.getHeight());
				}
			}
		};

		hauptlayout = new FormLayout(
				"20px, " + INITIAL_DIAGRAMM_WIDTH + "px, " + AbstractSchrittView.FORMLAYOUT_GAP,
				"10px, fill:pref, fill:default, fill:pref");
		arbeitsbereich.setLayout(hauptlayout);
		arbeitsbereich.setBackground(new Color(247, 247, 253));
		displayWelcomeMessage();

		intro = new EditContainer(this);
		intro.setOpaque(false);
		arbeitsbereich.add(intro, CC.xy(2, 2));

		outro = new EditContainer(this);
		outro.setOpaque(false);
		arbeitsbereich.add(outro, CC.xy(2, 4));

		scrollPane.setViewportView(arbeitsbereich);
		actionListenerHinzufuegen();
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

	private void displayWelcomeMessage() {
		welcomeMessage = new WelcomeMessagePanel();
		arbeitsbereich.add(welcomeMessage, CC.xy(2, 3));
	}

	public void dropWelcomeMessage() {
		if (welcomeMessage != null) {
			arbeitsbereich.remove(welcomeMessage);
			welcomeMessage = null;
			hauptSequenzInitialisieren();
		}
	}

	private void setApplicationIcon() {
		setIconImage(readImageIcon("specman").getImage());
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
					} else if (dialogResult == JOptionPane.YES_OPTION) { // Save & Close
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
		dragX = null;
		arbeitsbereich.repaint();
		return delta;
	}

	@Override
	public void vertikalLinieSetzen(int x, SpaltenResizer spaltenResizer) {
		if (spaltenResizer != null) {
			Point relativePosition = SwingUtilities.convertPoint(spaltenResizer, new Point(x, 0), arbeitsbereich);
			dragX = (int)relativePosition.getX();
		}
		else {
			dragX = null;
		}
		arbeitsbereich.repaint();
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

	private void actionListenerHinzufuegen() {

		exportPDF.addActionListener((e -> {
      exportAsPDF();
    }));

		speichern.addActionListener(e -> diagrammSpeichern(false));
		speichernUnter.addActionListener(e -> diagrammSpeichern(true));
		laden.addActionListener(e -> diagrammLaden());

		exportAsPDFMenuItem.addActionListener(e -> exportAsPDF());

		exportAsGraphvizMenuItem.addActionListener(e -> exportAsGraphviz());

		exitMenuItem.addActionListener(e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));

		review.addActionListener(e -> hauptSequenz.zusammenklappenFuerReview());

		zoom.addActionListener(e -> {
			int prozentNeu = ((ZoomFaktor) zoom.getSelectedItem()).getProzent();
			int prozentAlt = skalieren(prozentNeu);
			undoManager.addEdit(new UndoableDiagrammSkaliert(Specman.this, prozentAlt));
		});



	}

  @Override
	public void resyncStepnumberStyleUDBL() {
    hauptSequenz.resyncStepnumberStyleUDBL();
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
    KlappButton.scaleIcons(prozent, bisherigerFaktor);
		hauptSequenz.skalieren(prozent, bisherigerFaktor);
		intro.skalieren(prozent, bisherigerFaktor);
		outro.skalieren(prozent, bisherigerFaktor);
		return bisherigerFaktor;
	}

	/** Nicht so schön, aber nötig: hier wird der Zoomfaktor in der Combobox auf einen neuen Wert aktualisiert,
	 * ohne dass dabei der AktionListener aufgerufen wird, der dann wiederum einen Eintrag im UndoManager
	 * produzieren würde. Wir brauchen die Umstellung des Werts aber grade für Undo und Redo, wo natürlich
	 * nichts neues eingetragen werden soll. Wir machen das durch Entfernen und wieder Anklemmen der Listener.
	 */
	void zoomFaktorAnzeigeAktualisieren(int prozent) {
		ZoomFaktor faktor = ZoomFaktor.valueOf("Faktor_" + zoomFaktor);
		List<ActionListener> listeners = Arrays.asList(zoom.getActionListeners());
		listeners.forEach(l -> zoom.removeActionListener(l));
		zoom.setSelectedItem(faktor);
		listeners.forEach(l -> zoom.addActionListener(l));
	}

	private void diagrammSpeichern(boolean dateiauswahlErzwingen) {
		new SaveDiagrammSpecmanOp(this).speichern(dateiauswahlErzwingen);
	}

	private void diagrammLaden() {
		new LoadDiagrammSpecmanOp(this).laden();
	}

	public void diagrammLaden(File diagramFile) {
		new LoadDiagrammSpecmanOp(this).laden(diagramFile);
	}

	void quellZielZuweisung(List<AbstractSchrittModel_V001> allModelSteps) {
		for(AbstractSchrittModel_V001 modelStep: allModelSteps) {
			if (modelStep.quellschrittID != null) {
				AbstractSchrittView zielschritt = hauptSequenz.findeSchrittZuId(modelStep.id);
				if(zielschritt instanceof QuellSchrittView) {
					continue;
				}
				else {
					QuellSchrittView quellSchritt = (QuellSchrittView) hauptSequenz.findeSchrittZuId(modelStep.quellschrittID);
					zielschritt.setQuellschrittUDBL(quellSchritt);
					quellSchritt.setZielschritt(zielschritt);
				}
			}
		}
	}

	@Override
	public void schrittFuerNachinitialisierungRegistrieren(AbstractSchrittView schritt) {
		postInitSchritte.add(schritt);
	}

	void neueSchritteNachinitialisieren() {
		for (AbstractSchrittView schritt: postInitSchritte) {
			schritt.nachinitialisieren();
		}
	}

	private void diagrammbreiteSetzen(int breite) {
		hauptlayout.setColumnSpec(2, ColumnSpec.decode(breite + "px"));
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
    resyncStepnumberStyleUDBL();
		addEdit(new UndoableSchrittHinzugefuegt(newStep, newStep.getParent()));
		newStep.skalieren(zoomFaktor, 100);
		newStep.initInheritedTextFieldIndentions();
		diagrammAktualisieren(newStep.getFirstEditArea());
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		toolBar = new JToolBar();
		toolBar.setFloatable(false); //ToDo Sidebar added
		buttonBar = new JToolBar(JToolBar.VERTICAL); //ToDo Sidebar added

		createSimpleStep = new CreateSimpleStepOpButton(this);
		createWhileStep = new CreateWhileStepOpButton(this);
		createWhileWhileStep = new CreateWhileWhileStepOpButton(this);
		createIfElseStep = new CreateIfElseStepOpButton(this);
		createIfStep = new CreateIfStepOpButton(this);
		createCaseStep = new CreateCaseStepOpButton(this);
		createSubsequenceStep = new CreateSubsequenceStepOpButton(this);
		createBreakStep = new CreateBreakStepOpButton(this);
		createCatchStep = new CreateCatchStepOpButton(this);
		createCaseBranch = new CreateCaseBranchOpButton(this);
		exportPDF = new JButton();
		einfaerben = new ToneOpButton(this);
		loeschen = new DeleteStepOpButton(this);
		toggleBorderType = new ToggleBorderTypeOpButton(this);
		review = new JButton();
		birdsview = new BirdsViewSpecmanOpButton(this);
		aenderungenVerfolgen = new JToggleButton();
		aenderungenUebernehmen = new AcceptChangesOpButton(this);
		aenderungenVerwerfen = new RevertChangesOpButton(this);
		aenderungenVerfolgen.setBackground(AENDERUNGSFARBE.panelColor);
		aenderungenUebernehmen.setBackground(AENDERUNGSFARBE.panelColor);
		aenderungenVerwerfen.setBackground(AENDERUNGSFARBE.panelColor);
		zoom = new JComboBox<ZoomFaktor>();
		for (ZoomFaktor faktor: ZoomFaktor.values())
			zoom.addItem(faktor);
		zoom.setSelectedItem(ZoomFaktor.Faktor_100);
		zoom.setMaximumSize(new Dimension(65, 20));
		speichern = new JMenuItem("Speichern");
		speichern.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
		speichernUnter = new JMenuItem("Speichern unter...");
		speichernUnter.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK));
		laden = new JMenuItem("Laden...");
		laden.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
		exportAsPDFMenuItem = new JMenuItem("Als PDF exportieren...");
		exportAsPDFMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK));
		exportAsGraphvizMenuItem = new JMenuItem("Als Graphviz exportieren");
		exitMenuItem = new JMenuItem("Beenden");
		exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK));
		//======== this ========
		contentPane = new JPanel();
		setContentPane(contentPane);
		contentPane.setLayout(new FormLayout("pref, default:grow", "default, default, fill:10px:grow")); //ToDo Sidebar added "pref"

		//======== toolBar ========
		toolbarButtonHinzufuegen(createSimpleStep, "einfacher-schritt", "Create simple step", buttonBar);
		toolbarButtonHinzufuegen(createWhileStep, "while-schritt", "Create while step", buttonBar);
		toolbarButtonHinzufuegen(createWhileWhileStep, "whilewhile-schritt", "Create while-while step", buttonBar);
		toolbarButtonHinzufuegen(createIfElseStep, "ifelse-schritt", "Create if-else step", buttonBar);
		toolbarButtonHinzufuegen(createIfStep, "if-schritt", "Create if step", buttonBar);
		toolbarButtonHinzufuegen(createCaseStep, "case-schritt", "Create case step", buttonBar);
		toolbarButtonHinzufuegen(createSubsequenceStep, "subsequenz-schritt", "Create subsequence step", buttonBar);
		toolbarButtonHinzufuegen(createBreakStep, "break-schritt", "Create break step", buttonBar);
		toolbarButtonHinzufuegen(createCatchStep, "catch-schritt", "Create catch block", buttonBar);
		toolbarButtonHinzufuegen(createCaseBranch, "zweig", "Create case branch", buttonBar);
		toolbarButtonHinzufuegen(einfaerben, "helligkeit", "Hintergrund schattieren", toolBar);
		toolbarButtonHinzufuegen(loeschen, "loeschen", "Schritt löschen", toolBar);
		toolbarButtonHinzufuegen(toggleBorderType, "switch-border", "Rahmen umschalten", toolBar);
		toolBar.addSeparator();
		toolbarButtonHinzufuegen(aenderungenVerfolgen, "aenderungen", "Änderungen verfolgen", toolBar);
		toolbarButtonHinzufuegen(aenderungenUebernehmen, "uebernehmen", "Änderungen übernehmen", toolBar);
		toolbarButtonHinzufuegen(aenderungenVerwerfen, "verwerfen", "Änderungen verwerfen", toolBar);
		toolbarButtonHinzufuegen(review, "review", "Für Review zusammenklappen", toolBar);
		toolBar.addSeparator();
    toolBar.add(zoom);
    toolbarButtonHinzufuegen(birdsview, "birdsview", "Bird's View", toolBar);
    toolbarButtonHinzufuegen(exportPDF, "pdf", "PDF exportieren", toolBar);

		contentPane.add(toolBar, CC.xywh(1, 1, 2, 1));
		contentPane.add(buttonBar, CC.xy(1, 3));
		pack();
		setLocationRelativeTo(getOwner());
		DragMouseAdapter dragButtonAdapter = new DragMouseAdapter(this);
		addDragAdapter(createSimpleStep, dragButtonAdapter);
		addDragAdapter(createWhileStep, dragButtonAdapter);
		addDragAdapter(createWhileWhileStep, dragButtonAdapter);
		addDragAdapter(createIfElseStep, dragButtonAdapter);
		addDragAdapter(createIfStep, dragButtonAdapter);
		addDragAdapter(createCaseStep, dragButtonAdapter);
		addDragAdapter(createSubsequenceStep, dragButtonAdapter);
		addDragAdapter(createBreakStep, dragButtonAdapter);
		addDragAdapter(createCatchStep, dragButtonAdapter);
		addDragAdapter(createCaseBranch, dragButtonAdapter);


	}

	private void addDragAdapter(JButton button, DragMouseAdapter adapter) {
		button.addMouseListener(adapter);
		button.addMouseMotionListener(adapter);
	}

	public static ImageIcon readImageIcon(String iconBasename) {
		String resource = "images/" + iconBasename + ".png";
		try {
			URL imageURL = Specman.class.getClassLoader().getResource(resource);
			Image image = ImageIO.read(imageURL);
			if (image == null) {
				throw new IllegalArgumentException("Can't load image icon " + resource);
			}
			return new ImageIcon(image);
		}
		catch(IOException iox) {
			iox.printStackTrace();
			throw new IllegalArgumentException("Error reading image icon " + resource + ": " + iox.getMessage());
		}
	}

	private void toolbarButtonHinzufuegen(AbstractButton button, String iconBasename, String tooltip, JToolBar tb) {
		button.setIcon(readImageIcon(iconBasename));
		button.setMargin(new Insets(0, 0, 0, 0));
		button.setToolTipText(tooltip);
		tb.add(button);
	}

	HTMLEditorPane shefEditorPane;
	SpecmanUndoManager undoManager;

	@Override
	public void instrumentWysEditor(JEditorPane ed, String initialText, Integer orientation) {
		shefEditorPane.instrumentWysEditor(ed, initialText, orientation);
	}

  private void initShefController() throws Exception {
		shefEditorPane = new HTMLEditorPane(undoManager);
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(baueDateiMenu());
		menuBar.add(shefEditorPane.getEditMenu());
		menuBar.add(shefEditorPane.getFormatMenu());
		menuBar.add(shefEditorPane.getInsertMenu());
		menuBar.add(baueAenderungsfarbenMenu());

		setJMenuBar(menuBar);
		contentPane.add(shefEditorPane.getFormatToolBar(), CC.xywh(1, 2, 2, 1));

	}

	@Override
	public void addEdit(UndoableEdit edit) {
    	undoManager.addEdit(edit);
	}

	private JMenu baueDateiMenu() {
		JMenu dateiMenu = new JMenu("Datei");
		dateiMenu.add(laden);
		dateiMenu.add(recentFiles.menu());
		dateiMenu.add(speichern);
		dateiMenu.add(speichernUnter);
		dateiMenu.add(exportAsPDFMenuItem);
		dateiMenu.add(exportAsGraphvizMenuItem);
		dateiMenu.add(exitMenuItem);
		return dateiMenu;
	}

	private JMenu baueAenderungsfarbenMenu() {
		JMenu menu = new JMenu("Änderungsfarbe");
		menu.setIcon(new ColorDotIcon(AENDERUNGSFARBE.text.color));
		for (ChangeColorSet cs : CHANGESETS) {
			JMenuItem item = new JMenuItem(new ColorDotIcon(cs.text.color));
			item.addActionListener(e -> fehler("Work in progress"));
			menu.add(item);
		}
		return menu;
	}

	private JToolBar toolBar;
	private JToolBar buttonBar; // Sidebar ergänzt
	private CreateSimpleStepOpButton createSimpleStep;
	private CreateWhileStepOpButton createWhileStep;
	private CreateWhileWhileStepOpButton createWhileWhileStep;
	private CreateIfElseStepOpButton createIfElseStep;
	private CreateIfStepOpButton createIfStep;
	private CreateCaseStepOpButton createCaseStep;
	private CreateSubsequenceStepOpButton createSubsequenceStep;
	private CreateBreakStepOpButton createBreakStep;
	private CreateCatchStepOpButton createCatchStep;
	private CreateCaseBranchOpButton createCaseBranch;
	private JButton exportPDF;
	private ToneOpButton einfaerben;
	DeleteStepOpButton loeschen;
	private ToggleBorderTypeOpButton toggleBorderType;
	private JButton review;
	private BirdsViewSpecmanOpButton birdsview;
	private AcceptChangesOpButton aenderungenUebernehmen;
	private RevertChangesOpButton aenderungenVerwerfen;
	private JComboBox<ZoomFaktor> zoom;
	JToggleButton aenderungenVerfolgen;
	private JMenuItem speichern;
	private JMenuItem speichernUnter;
	private JMenuItem laden;
	private JMenuItem exportAsPDFMenuItem;
	private JMenuItem exportAsGraphvizMenuItem;
	private JMenuItem exitMenuItem;

	private static Specman instance;

	public static EditorI instance() { return instance; }


	public boolean aenderungenVerfolgen() {
		return aenderungenVerfolgen.isSelected();
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
		new ExportPDFSpecmanOp(this).export();
	}

	@Override public int getZoomFactor() {
		return zoomFaktor;
	}

	public SchrittSequenzView getHauptSequenz() {
		return hauptSequenz;
	}

	public SpecmanUndoManager getUndoManager() {
		return undoManager;
	}

	public JButton getCreateSimpleStep() { return createSimpleStep; }
	public JButton getCreateWhileStep() { return createWhileStep; }
	public JButton getCreateWhileWhileStep() { return createWhileWhileStep; }
	public JButton getCreateIfElseStep() { return createIfElseStep; }
	public JButton getCreateIfStep() { return createIfStep; }
	public JButton getCreateCaseStep() { return createCaseStep; }
	public JButton getCreateSubsequenceStep() { return createSubsequenceStep; }
	public JButton getCreateBreakStep() { return createBreakStep; }
	public JButton getCreateCatchStep() { return createCatchStep; }
	public JButton getCreateCaseBranch() { return createCaseBranch; }

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
  public void deleteStepUDBL(AbstractSchrittView step, InteractiveStepFragment initiatingFragment) {
    new DeleteStepSpecmanOp(this, step, initiatingFragment).run();
  }

  @Override
  public void moveBranchSequenceLeftUDBL(AbstractSchrittView step, InteractiveStepFragment initiatingFragment) {
    new MoveBranchSequenceLeftSpecmanOp(this, step, initiatingFragment).run();
  }

  @Override
  public void moveBranchSequenceRightUDBL(AbstractSchrittView step, InteractiveStepFragment initiatingFragment) {
    new MoveBranchSequenceRightSpecmanOp(this, step, initiatingFragment).run();
  }

  @Override
  public int showConfirmDialog(String message, String title, int optionType) {
    return JOptionPane.showConfirmDialog(this, message, title, optionType);
  }

}

package specman;

import com.formdev.flatlaf.FlatLightLaf;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import net.atlanticbb.tantlinger.shef.HTMLEditorPane;
import specman.cli.CLIOperation;
import specman.cli.SpecmanCLI;
import specman.i18n.Messages;
import specman.menubar.SpecmanMenuBar;
import specman.draganddrop.DragMouseAdapter;
import specman.draganddrop.GlassPane;
import specman.editarea.EditArea;
import specman.ops.*;
import specman.metatag.generic.GenericMetaTag;
import specman.metatag.generic.config.MetaTagConfig;
import specman.metatag.generic.config.MetaTagConfigListener;
import specman.metatag.generic.config.MetaTagConfigRegistry;
import specman.metatag.generic.config.MetaTagConfigsDialog;
import specman.settings.SettingsDialog;

import specman.editarea.InteractiveStepFragment;
import specman.graphics.IconReader;
import specman.model.v002.PdfExportOptionsModel_V002;
import specman.editarea.EditContainer;
import specman.editarea.TableEditArea;
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
import java.awt.datatransfer.Clipboard;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static specman.graphics.Styles.DIAGRAMM_LINE_COLOR;
import static specman.i18n.Messages.i18n;

public class Specman extends JRootPane implements EditorI, SpaltenContainerI, SpecmanOpContext {
	private static final BasicStroke GESTRICHELTE_LINIE =
			new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND, 1.0f, new float[] {10.0f, 10.0f }, 0f);
	public static String SPECMAN_TITLE = "Specman " + SpecmanVersion.getVersion();

	TextEditArea lastFocusedTextArea;
	private SchrittSequenzView hauptSequenz;
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
  private ChangeSet currentChangeSet = ChangeSet.DEFAULT;
  private final MetaTagConfigRegistry metaTagConfigs = new MetaTagConfigRegistry();

	private DiagramToolBar diagramToolBar;
	private StepButtonBar stepButtonBar;
	private SpecmanMenuBar specmanMenuBar;

	private KeyboardSpecmanOp keyboardOp;
	private final ExportPDFSpecmanOp exportPDFOp = createExportPDFOp();
  private AutoSaveOp autoSave;
  private AutoLoadOp autoLoad;

	private static Specman instance;
	public final String instanceId = java.util.UUID.randomUUID().toString();

	/** The window hosting Specman as its root pane. Null as long as Specman is not shown in a window, e.g. headless. */
	private SpecmanFrame frame;
	private String title = SPECMAN_TITLE;
	/** The former JFrame's default component name. It is persisted in existing diagram files. */
	private String diagrammName = "frame0";

	/**
	 * Specman is the root pane of the application window. It can be handed over to a {@link SpecmanFrame},
	 * but it can also live without one (headless tests). Everything related to the window itself is done
	 * by the {@link SpecmanFrame}.
	 */
	public Specman(File fileToOpen) throws Exception {
		instance = this;
		undoManager = new SpecmanUndoManager(this);

		initComponents();

		initShefController();

		scrollPane = new JScrollPane();
    viewport = new PausableViewport();
    scrollPane.setViewport(viewport);
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(20);
		scrollPane.addMouseWheelListener(createDragMouseAdapter());
		contentPane.add(scrollPane, CC.xy(2, 2));

		initEmptyDiagram();

		configureKeyboardManager();

		openInitialFile(fileToOpen);
    initAutoSaveAndLoad();
	}

	/** Overridable, so that tests can do without the auto-save timer and the working copy watcher. */
	protected void initAutoSaveAndLoad() {
    autoSave = new AutoSaveOp(this);
    autoLoad = new AutoLoadOp(this, autoSave);
	}

	/** Called by the {@link SpecmanFrame} when Specman is shown in it. */
	void attachFrame(SpecmanFrame frame) {
		this.frame = frame;
		frame.setTitle(title);
	}

	/**
	 * The GlassPane's offset calibration refers to the window's coordinate space, so it can only be
	 * installed once the window is visible. It is needed for dragging only.
	 */
	void installGlassPane() {
		setGlassPane(new GlassPane(
			SwingUtilities.convertPoint(contentPane, 0, 0, getWindowOrRootPane()).y,
			getJMenuBar().getHeight()));
	}

	/** The reference component for coordinates used by the drag and drop logic: the window if there is any. */
	public Container getWindowOrRootPane() {
		Window window = SwingUtilities.getWindowAncestor(this);
		return window != null ? window : this;
	}

	public Frame getFrame() {
		return frame;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
		if (frame != null) {
			frame.setTitle(title);
		}
	}

	/**
	 * Called when the user wants to close the window.
	 * @return true if the application may be terminated, false if the user cancelled because of unsaved changes
	 */
	boolean confirmClose() {
		if (undoManager.hasUnsavedChanges()) {
			if (!AbstractInitSpecmanOp.confirmDiscardUnsavedChanges(
					getDiagramFilename(), () -> diagrammSpeichern(false))) {
				return false;
			}
		}
		AutoSaveOp.deleteWorkingCopyFor(diagrammDatei);
		return true;
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

	@Override
	public void initEmptyDiagram() {
		hauptSequenz = new SchrittSequenzView();
		arbeitsbereich = new WorkingAreaPanel();
		intro = new EditContainer();
		intro.setOpaque(false);
		arbeitsbereich.add(intro, CC.xy(2, 2));
		outro = new EditContainer();
		outro.setOpaque(false);
		arbeitsbereich.add(outro, CC.xy(2, 4));
		scrollPane.setViewportView(arbeitsbereich);
		hauptSequenzContainer = null;
		diagrammbreite = WorkingAreaPanel.INITIAL_DIAGRAMM_WIDTH;
		zoomFaktor = 100;
		diagrammDatei = null;
		metaTagConfigs.clear();
		setTitle(SPECMAN_TITLE);
		undoManager.discardAllEdits();
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

	private String getDiagramFilename() {
		if (diagrammDatei != null) {
			return diagrammDatei.getName();
		}
		return i18n("msg.filename.unknown");
	}

	@Override
	public int spaltenbreitenAnpassenNachMausDragging(int delta, int spalte) {
		diagrammbreite += delta;
		diagrammbreiteSetzen(diagrammbreite);
		diagrammAktualisieren(null);
		arbeitsbereich.showDragLine(null);
		return delta;
	}

	/** Shows a vertical drag line at x, given in the coordinates of the source component; null as source hides the line. */
	@Override
	public void vertikalLinieSetzen(int x, Component source) {
		Integer dragX = null;
		if (source != null) {
			Point relativePosition = SwingUtilities.convertPoint(source, new Point(x, 0), arbeitsbereich);
			dragX = (int)relativePosition.getX();
		}
		arbeitsbereich.showDragLine(dragX);
	}

	@Override
	public void hauptSequenzInitialisieren() {
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
		updateFocusDependentActions();
  }

	/**
	 * Enables the toolbar's step buttons only if the last focused text area belongs to a step, and the insert
	 * buttons only if a text area has been focused at all.
	 */
	public void updateFocusDependentActions() {
		if (diagramToolBar == null) {
			return;
		}
		AbstractSchrittView step = lastFocusedTextArea != null && hauptSequenz != null
			? hauptSequenz.findeSchritt(lastFocusedTextArea) : null;
		diagramToolBar.updateFocusDependentButtons(step != null, lastFocusedTextArea != null);
	}

	@Override
	public void setDiagrammDatei(File diagrammDatei) {
		this.diagrammDatei = diagrammDatei;
		setTitle(getDiagramFilename() + " - "+ SPECMAN_TITLE);
	}

	@Override
	public void discardAllUndoEdits() {
		undoManager.discardAllEdits();
	}

	@Override
	public String getDiagrammName() {
		return diagrammName;
	}

	@Override
	public int getDiagrammbreite() {
		return diagrammbreite;
	}

	public void fehler(String messageKey, Object... args) {
		JOptionPane.showMessageDialog(this, i18n(messageKey, args));
	}

	@Override
	public void showMessage(String messageKey, Object... args) {
		SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, i18n(messageKey, args)));
	}

	@Override
	public void showToast(String message) {
		if (frame != null) {
			SwingUtilities.invokeLater(() -> ToastNotification.show(frame, message));
		}
	}

	@Override
	public void showToast(String briefMessage, String detailMessage) {
		if (frame != null) {
			SwingUtilities.invokeLater(() -> ToastNotification.show(frame, briefMessage, detailMessage));
		}
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
			EditArea nextFocusArea = lastFocusedTextArea.addTable(columns, rows);
			diagrammAktualisieren(nextFocusArea);
		}
	}

	@Override
	public void toggleListItem(boolean ordered) {
		if (lastFocusedTextArea != null) {
			EditArea nextFocusArea = lastFocusedTextArea.toggleListItemUDBL(ordered);
			diagrammAktualisieren(nextFocusArea);
		}
	}

	public int skalieren(int prozent) {
		int bisherigerFaktor = zoomFaktor;
		zoomFaktor = prozent;
		zoomFaktorAnzeigeAktualisieren(prozent);
		float diagrammbreite100Prozent = (float)diagrammbreite / bisherigerFaktor * 100;
		int neueDiagrammbreite = (int)(diagrammbreite100Prozent * prozent / 100);
		spaltenbreitenAnpassenNachMausDragging(neueDiagrammbreite - diagrammbreite, 0);
		hauptSequenz.skalieren(prozent, bisherigerFaktor);
		intro.skalieren(prozent, bisherigerFaktor);
		outro.skalieren(prozent, bisherigerFaktor);
		return bisherigerFaktor;
	}

	@Override
	public void zoomFaktorAnzeigeAktualisieren(int prozent) {
		diagramToolBar.updateZoomDisplay(prozent);
	}

	@Override
	public void diagrammSpeichern(boolean dateiauswahlErzwingen) {
		new SaveDiagrammSpecmanOp(this).speichern(dateiauswahlErzwingen);
	}

	@Override
	public void diagrammNeu() {
		new NewDiagrammSpecmanOp(this).create();
	}

	@Override
	public void diagrammLaden() {
		new LoadDiagrammSpecmanOp(this).load();
	}

	public void diagrammLaden(File diagramFile) {
		new LoadDiagrammSpecmanOp(this).loadFromDiagrammOrWorkingCopy(diagramFile);
	}

	private void diagrammbreiteSetzen(int breite) {
		arbeitsbereich.diagrammbreiteSetzen(breite);
	}

	public void diagrammAktualisieren(EditArea editArea) {
		// Steps may have been deleted or restored, so the focused text area may no longer belong to a step.
		updateFocusDependentActions();
		// Null-Abfrage ist für den Fall, dass der User etwas im Intro oder Outro macht,
		// bevor er überhaupt das Diagramm angefangen hat. Sollte man später noch mal
		// bereinigen, dass das gar nicht geht, solange die Welcome Message noch angezeigt wird.
		if (hauptSequenzContainer != null) {
			hauptSequenzContainer.setVisible(false);
			// Folgende Zeile forciert ein Relayouting, falls z.B. nur eine manuelle Breitenänderung
			// einer If-Else-Spaltenteilung stattgefunden hat - das wirkt sich ohne diesen Nudge
			// (und ohne das setVisible(false/true) drumherum) sonst nicht sofort sichtbar aus.
			diagrammbreiteSetzen(diagrammbreite - 1);
			// Separately, adding/loading/changing steps can leave some newly constructed nested
			// FormLayout containers painted with stale bounds (most visibly on step number
			// labels) - the diagrammbreite nudge above does not fix this. Only an actual resize
			// event does, because that's the only thing that reliably retriggers every affected
			// component's own componentResized() handling. So we synthesize one here too: nudge
			// the window size by 1px, restored together with the diagrammbreite nudge below.
			// A maximized window must not be resized: that would restore it to its normal size.
			Container window = getWindowOrRootPane();
			Dimension size = window.getSize();
			boolean nudgeWindow = !isWindowMaximized();
			if (nudgeWindow) {
				window.setSize(size.width + 1, size.height);
			}
			final Point viewPosition = scrollPane.getViewport().getViewPosition();
			SwingUtilities.invokeLater(() -> {
				diagrammbreiteSetzen(diagrammbreite);
				if (nudgeWindow) {
					window.setSize(size.width, size.height);
				}
				hauptSequenzContainer.setVisible(true);
				if (editArea != null) {
					editArea.requestFocus();
				}
				scrollPane.getViewport().setViewPosition(viewPosition);
			});
		}
	}

	private boolean isWindowMaximized() {
		return frame != null && (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
	}

	@Override
	public WorkingAreaPanel getArbeitsbereich() {
		return arbeitsbereich;
	}

	@Override
	public void scrollBy(int pages) {
		JScrollBar bar = scrollPane.getVerticalScrollBar();
		int pageSize = scrollPane.getViewport().getHeight();
		bar.setValue(bar.getValue() + pages * pageSize);
	}

	@Override
	public File getDiagrammDatei() {
		return diagrammDatei;
	}

	@Override
	public JScrollPane getScrollPane() {
		return scrollPane;
	}

	@Override
	public SpaltenResizer getBreitenAnpasser() {
		return breitenAnpasser;
	}

	@Override
	public JPanel getHauptSequenzContainer() {
		return hauptSequenzContainer;
	}

	@Override
	public Image createDiagramImage(int width, int height) {
		// A lightweight root pane delegates to its window, without a window the result is null
		Image image = createImage(width, height);
		return image != null ? image : new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	}

	public void newStepPostInit(AbstractSchrittView newStep) {
    resyncStepnumberStyleADBL();
		addEdit(new UndoableSchrittHinzugefuegt(newStep, newStep.getParent()));
		newStep.skalieren(zoomFaktor, 100);
		newStep.initInheritedTextFieldIndentions();
		diagrammAktualisieren(newStep.getFirstEditArea());
	}

	private void initComponents() {
		stepButtonBar = new StepButtonBar(this);

		contentPane = new JPanel();
		setContentPane(contentPane);
		contentPane.setLayout(new FormLayout("pref, default:grow", "default, fill:10px:grow")); //ToDo Sidebar added "pref"

		contentPane.add(stepButtonBar, CC.xy(1, 2));
	}

	HTMLEditorPane shefEditorPane;
	SpecmanUndoManager undoManager;

	@Override
	public void instrumentWysEditor(JEditorPane ed, String initialText, Integer orientation) {
		shefEditorPane.instrumentWysEditor(ed, initialText, orientation);
	}

  private void initShefController() throws Exception {
		shefEditorPane = new HTMLEditorPane(undoManager);
		specmanMenuBar = new SpecmanMenuBar(this, shefEditorPane);
		setJMenuBar(specmanMenuBar);
		diagramToolBar = new DiagramToolBar(this, shefEditorPane);
		// the line spans both columns, so it also runs over the step button bar
		contentPane.add(diagramToolBar.inScrollPane(
			BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor"))), CC.xywh(1, 1, 2, 1));
		updateFocusDependentActions();
	}

	@Override
	public void addEdit(UndoableEdit edit) {
    	undoManager.addEdit(edit);
	}

	public static EditorI editor() { return instance; }


	public boolean aenderungenVerfolgen() {
		return diagramToolBar.isChangeModeEnabled();
	}

	public void setChangeModeEnabled(boolean enabled) {
		diagramToolBar.setChangeModeEnabled(enabled);
	}

	public void updateChangeSet(ChangeSet changeSet) {
		currentChangeSet = changeSet;
		specmanMenuBar.updateChangeSet(changeSet);
	}

	@Override
	public void addRecentFile(File file) {
		specmanMenuBar.addRecentFile(file);
	}

	@Override
	public void setDiagrammName(String name) {
		this.diagrammName = name;
	}

	@Override
	public void clearFocusHistory() {
		focusHistory.clear();
	}

	@Override
	public void setZoomFaktor(int prozent) {
		zoomFaktor = prozent;
	}

	@Override
	public void setDiagrammbreite(int breite) {
		diagrammbreite = breite;
	}

	@Override
	public void resetPdfExportChooser() {
		exportPDFOp.resetChooser();
	}

	@Override
	public void setPdfExportOptions(PdfExportOptionsModel_V002 options) {
		exportPDFOp.setPdfExportOptions(options);
	}

	@Override
	public PdfExportOptionsModel_V002 getPdfExportOptions() {
		return exportPDFOp.getPdfExportOptions();
	}

	/**
	 * Swing requires all component creation and visibility changes to happen on the Event Dispatch
	 * Thread (EDT). Without invokeAndWait the constructor — including setVisible(true) — would run
	 * on the main thread, causing a race condition that sporadically prevents the window from
	 * appearing. invokeAndWait (rather than invokeLater) is used so that main() does not return
	 * before the window is fully initialized.
	 */
	public static void main(String[] args) throws Exception {
		if (CLIOperation.fromArgs(args) != null) {
			SpecmanCLI.run(args);
			return;
		}
    Messages.applyLanguagePreference();
    setLookAndFeel();
		File initialFileToOpen = readFileFromArgs(args);
		SwingUtilities.invokeAndWait(() -> {
			try {
				SpecmanFrame frame = new SpecmanFrame();
				Specman specman = new Specman(initialFileToOpen);
				frame.open(specman);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
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

	@Override
	public void exit() {
		Window window = SwingUtilities.getWindowAncestor(this);
		if (window != null) {
			window.dispatchEvent(new java.awt.event.WindowEvent(window, java.awt.event.WindowEvent.WINDOW_CLOSING));
		}
	}

	@Override
	public void exportAsGraphviz() {
		new ExportGraphvizSpecmanOp(this).export();
	}

	/** Called while Specman is being constructed. Overrides must not depend on their own fields. */
	protected ExportPDFSpecmanOp createExportPDFOp() {
		return new ExportPDFSpecmanOp(this);
	}

	public void exportAsPDF() {
		exportPDFOp.export();
	}

  @Override
  public void openSettings() {
    new SettingsDialog(frame).setVisible(true);
    diagramToolBar.reloadFileSettings();
    checkWorkingCopyNow();
  }

  /** Checks for a newer working copy, if auto load is enabled. The load starts after the debounce delay of the auto load. */
  public void checkWorkingCopyNow() {
    if (autoLoad != null) {
      autoLoad.checkNow();
    }
  }

  @Override
  public void openMetaTagConfigs() {
    new MetaTagConfigsDialog(frame, metaTagConfigs, queryMetaTagConfigsInUse()).setVisible(true);
  }

  @Override
  public void setMetaTagConfigs(List<MetaTagConfig> configs) {
    metaTagConfigs.clear();
    for (MetaTagConfig config : configs) {
      metaTagConfigs.add(config);
    }
  }

  @Override
  public List<MetaTagConfig> metaTagConfigs() { return metaTagConfigs.all(); }

  @Override
  public MetaTagConfig findMetaTagConfig(String name) { return metaTagConfigs.get(name); }

  @Override
  public Set<String> queryMetaTagConfigsInUse() {
    Set<String> names = new HashSet<>();
    for (AbstractSchrittView step : listAllSteps()) {
      names.addAll(step.getTagConfigNames());
    }
    return names;
  }

  @Override
  public List<GenericMetaTag> queryTagsWithFreetext() {
    List<GenericMetaTag> result = new ArrayList<>();
    for (AbstractSchrittView step : listAllSteps()) {
      for (GenericMetaTag tag : step.getTextShef().getTags()) {
        String freetext = tag.getFreetext();
        if (!tag.isDeleted() && freetext != null && !freetext.isBlank()) {
          result.add(tag);
        }
      }
    }
    return result;
  }

  @Override
  public void addMetaTagConfigListener(MetaTagConfigListener listener) { metaTagConfigs.addListener(listener); }

  @Override
  public void removeMetaTagConfigListener(MetaTagConfigListener listener) { metaTagConfigs.removeListener(listener); }

  @Override
  public boolean hasUnsavedChanges() {
    return undoManager.hasUnsavedChanges();
  }

  @Override
  public void markAsUnsavedWorkingCopy() {
    undoManager.markAsUnsavedWorkingCopy();
  }

  @Override
  public void notifyWorkingCopyInitialized(long timestamp) {
    autoLoad.workingCopyInitialized(timestamp);
  }

	@Override
	public void zusammenklappenFuerReview() {
		getHauptSequenz().zusammenklappenFuerReview();
	}

	@Override
	public void alleSchritteKlappen(boolean zuklappen) {
		getHauptSequenz().alleKlappen(zuklappen);
	}

	@Override public int getZoomFactor() {
		return zoomFaktor;
	}

	@Override public String instanceId() { return instanceId; }

	@Override
	public SchrittSequenzView getHauptSequenz() {
		return hauptSequenz;
	}

	@Override
	public EditContainer getIntro() {
		return intro;
	}

	@Override
	public EditContainer getOutro() {
		return outro;
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
		JOptionPane.showMessageDialog(this, ex.getMessage());
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
		return getHauptSequenz().findStepByStepID(stepID);
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

  @Override
  public void displayException(Exception x) {
    x.printStackTrace();
    displayErrorMessage(x.getMessage());
  }

  private void displayErrorMessage(String message) {
    JOptionPane.showMessageDialog(this, message, i18n("msg.error.title"), JOptionPane.ERROR_MESSAGE);
  }

  @Override
  public AbstractSchrittView findStep(InteractiveStepFragment fragment) {
    return hauptSequenz.findeSchritt(fragment);
  }

  @Override
  public boolean unfoldToShow(InteractiveStepFragment fragment) {
    AbstractSchrittView step = findStep(fragment);
    return step != null && step.unfoldToShow(fragment);
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

  @Override public Clipboard clipboard() { return Toolkit.getDefaultToolkit().getSystemClipboard(); }

  @Override public void copyStepToClipboard(AbstractSchrittView step) { new CopyStepOp(step).run(); }
  @Override public void cutStepToClipboard(AbstractSchrittView step, InteractiveStepFragment initiatingFragment) { new CutStepOp(this, step, initiatingFragment).run(); }

  @Override public void pasteStepsAfter(AbstractSchrittView referenceStep) { new PasteStepsOp(this, referenceStep).run(); }

  @Override public void copyTableToClipboard(TableEditArea table) { new CopyTableOp(table).run(); }
  @Override public void cutTableToClipboard(TableEditArea table) { new CutTableOp(this, table).run(); }
  @Override public void pasteTableInto(TextEditArea initiatingTextArea) { new PasteTableOp(this, initiatingTextArea).run(); }

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

  @Override
  public ChangeSet changeset() { return currentChangeSet; }

}

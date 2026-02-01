package specman;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formdev.flatlaf.FlatLightLaf;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import net.atlanticbb.tantlinger.shef.HTMLEditorPane;
import org.jetbrains.annotations.Nullable;
import specman.draganddrop.DragMouseAdapter;
import specman.draganddrop.GlassPane;
import specman.editarea.EditArea;
import specman.editarea.InteractiveStepFragment;
import specman.editarea.markups.MarkupType;
import specman.model.ModelEnvelope;
import specman.model.v001.*;
import specman.modelops.MoveBranchSequenceLeftOperation;
import specman.modelops.MoveBranchSequenceRightOperation;
import specman.pdf.PDFExportChooser;
import specman.pdf.PDFRenderer;
import specman.pdf.Shape;
import specman.editarea.EditContainer;
import specman.editarea.TextEditArea;
import specman.modelops.DeleteStepOperation;
import specman.undo.UndoableDiagrammSkaliert;
import specman.undo.UndoableSchrittEingefaerbt;
import specman.undo.UndoableSchrittHinzugefuegt;
import specman.undo.UndoableToggleStepBorder;
import specman.undo.UndoableZweigHinzugefuegt;
import specman.undo.manager.SpecmanUndoManager;
import specman.undo.manager.UndoRecording;
import specman.undo.manager.UndoRecordingMode;
import specman.view.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static specman.Aenderungsart.Hinzugefuegt;
import static specman.Aenderungsart.Untracked;
import static specman.editarea.TextStyles.AENDERUNGSMARKIERUNG_HINTERGRUNDFARBE;
import static specman.editarea.TextStyles.BACKGROUND_COLOR_STANDARD;
import static specman.editarea.TextStyles.DIAGRAMM_LINE_COLOR;
import static specman.view.RelativeStepPosition.After;

/**
 * @author User #3
 */
public class Specman extends JFrame implements EditorI, SpaltenContainerI {
	public static final int INITIAL_DIAGRAMM_WIDTH = 700;
	private static final String PROJEKTDATEI_EXTENSION = ".nsd";
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
	private final Set<Integer> pressedKeys = new HashSet<>();
	public static final String SPECMAN_TITLE = "Specman";

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
		//TODO
		scrollPane.addMouseWheelListener(new DragMouseAdapter(this));
		contentPane.add(scrollPane, CC.xy(2, 3));
		// ToDo Sidebar change from getContentPane().add(scrollPane, CC.xy(1, 3));

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

  private void locationToScreen(Component c) {
    Point location = c.getLocation();
    SwingUtilities.convertPointToScreen(location, this);
    System.out.println(location);
  }

  private void openInitialFile(File fileToOpen) {
		if (fileToOpen != null) {
			diagrammLaden(fileToOpen);
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
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
			if (e.getID() == KeyEvent.KEY_PRESSED) {
				pressedKeys.add(e.getKeyCode());
			} else if (e.getID() == KeyEvent.KEY_RELEASED) {
				pressedKeys.remove(e.getKeyCode());
			}
			return false;
		});
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


	private void hauptSequenzInitialisieren() {
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

	private void setDiagrammDatei(File diagrammDatei) {
		this.diagrammDatei = diagrammDatei;
		setTitle(getDiagramFilename() + " - "+ SPECMAN_TITLE);
	}

	private void fehler(String text) {
		JOptionPane.showMessageDialog(this, text);
	}

	private void actionListenerHinzufuegen() {
		einfachenSchrittAnhaengen.addActionUDBLListener(e -> {
      dropWelcomeMessage();
      AbstractSchrittView referenceStep = hauptSequenz.findeSchritt(lastFocusedTextArea);
      AbstractSchrittView schritt = (referenceStep != null)
        ? referenceStep.getParent().einfachenSchrittZwischenschieben(After, referenceStep, Specman.this)
        : hauptSequenz.einfachenSchrittAnhaengen(Specman.this);
      newStepPostInit(schritt);
    });

		whileSchrittAnhaengen.addActionUDBLListener(e -> {
      dropWelcomeMessage();
      AbstractSchrittView referenceStep = hauptSequenz.findeSchritt(lastFocusedTextArea);
      AbstractSchrittView schritt = (referenceStep != null)
        ? referenceStep.getParent().whileSchrittZwischenschieben(After, referenceStep, Specman.this)
        : hauptSequenz.whileSchrittAnhaengen(Specman.this);
      newStepPostInit(schritt);
    });

		whileWhileSchrittAnhaengen.addActionUDBLListener(e -> {
      dropWelcomeMessage();
      AbstractSchrittView referenceStep = hauptSequenz.findeSchritt(lastFocusedTextArea);
      AbstractSchrittView schritt = (referenceStep != null)
          ? referenceStep.getParent().whileWhileSchrittZwischenschieben(After, referenceStep, Specman.this)
          : hauptSequenz.whileWhileSchrittAnhaengen(Specman.this);
      newStepPostInit(schritt);
		});

		ifElseSchrittAnhaengen.addActionUDBLListener(e -> {
      dropWelcomeMessage();
      AbstractSchrittView referenceStep = hauptSequenz.findeSchritt(lastFocusedTextArea);
      AbstractSchrittView schritt = (referenceStep != null)
          ? referenceStep.getParent().ifElseSchrittZwischenschieben(After, referenceStep, Specman.this)
          : hauptSequenz.ifElseSchrittAnhaengen(Specman.this);
      newStepPostInit(schritt);
    });

		ifSchrittAnhaengen.addActionUDBLListener(e -> {
      dropWelcomeMessage();
      AbstractSchrittView referenceStep = hauptSequenz.findeSchritt(lastFocusedTextArea);
      AbstractSchrittView schritt = (referenceStep != null)
          ? referenceStep.getParent().ifSchrittZwischenschieben(After, referenceStep, Specman.this)
          : hauptSequenz.ifSchrittAnhaengen(Specman.this);
      newStepPostInit(schritt);
		});

		caseSchrittAnhaengen.addActionUDBLListener(e -> {
      dropWelcomeMessage();
      AbstractSchrittView referenceStep = hauptSequenz.findeSchritt(lastFocusedTextArea);
      AbstractSchrittView schritt = (referenceStep != null)
          ? referenceStep.getParent().caseSchrittZwischenschieben(After, referenceStep, Specman.this)
          : hauptSequenz.caseSchrittAnhaengen(Specman.this);
      newStepPostInit(schritt);
		});

		subsequenzSchrittAnhaengen.addActionUDBLListener(e -> {
      dropWelcomeMessage();
      AbstractSchrittView referenceStep = hauptSequenz.findeSchritt(lastFocusedTextArea);
      AbstractSchrittView schritt = (referenceStep != null)
          ? referenceStep.getParent().subsequenzSchrittZwischenschieben(After, referenceStep, Specman.this)
          : hauptSequenz.subsequenzSchrittAnhaengen(Specman.this);
      newStepPostInit(schritt);
		});

		breakSchrittAnhaengen.addActionUDBLListener(e -> {
      dropWelcomeMessage();
      AbstractSchrittView referenceStep = hauptSequenz.findeSchritt(lastFocusedTextArea);
      AbstractSchrittView schritt = (referenceStep != null)
          ? referenceStep.getParent().breakSchrittZwischenschieben(After, referenceStep, Specman.this)
          : hauptSequenz.breakSchrittAnhaengen(Specman.this);
      newStepPostInit(schritt);
		});

		catchSchrittAnhaengen.addActionUDBLListener(e -> {
      AbstractSchrittView referenceStep = hauptSequenz.findeSchritt(lastFocusedTextArea);
      CatchUeberschrift referenceCatchHeading = lastFocusedTextArea.containingCatchHeading();
      if (referenceStep != null) { // Avoids placing a catch step as the first one in the diagramm
        dropWelcomeMessage();
        new CatchLinkDialog(null, referenceStep.getParent(), referenceCatchHeading);
      }
		});

		caseAnhaengen.addActionUDBLListener(e -> {
      dropWelcomeMessage();
      AbstractSchrittView schritt = hauptSequenz.findeSchritt(lastFocusedTextArea);
      if (!(schritt instanceof CaseSchrittView)) {
        fehler("Kein Case-Schritt ausgewählt");
        return;
      }
      CaseSchrittView caseSchritt = (CaseSchrittView)schritt;
      ZweigSchrittSequenzView ausgewaehlterZweig = caseSchritt.headingToBranch(lastFocusedTextArea);
      if (ausgewaehlterZweig == null) {
        fehler("Kein Zweig ausgewählt");
        return;
      }
      ZweigSchrittSequenzView neuerZweig = caseSchritt.neuenZweigHinzufuegen(Specman.this, ausgewaehlterZweig);
      resyncStepnumberStyleUDBL();
      addEdit(new UndoableZweigHinzugefuegt(Specman.this, neuerZweig, caseSchritt));
      schritt.skalieren(zoomFaktor, 100);
      diagrammAktualisieren(schritt.getFirstEditArea());
		});

		exportPDF.addActionListener((e -> {
      exportAsPDF();
    }));

		einfaerben.addActionUDBLListener(e -> {
      if (lastFocusedTextArea != null) {
        AbstractSchrittView schritt = hauptSequenz.findeSchritt(lastFocusedTextArea);
        Color aktuelleHintergrundfarbe = schritt.getBackground();
        int farbwert = aktuelleHintergrundfarbe.getRed() == 240 ? 255 : 240;
        Color neueHintergrundfarbe = new Color(farbwert, farbwert, farbwert);
        schritt.setBackgroundUDBL(neueHintergrundfarbe);
        addEdit(new UndoableSchrittEingefaerbt(schritt, aktuelleHintergrundfarbe, neueHintergrundfarbe));
      }
    });

		loeschen.addActionUDBLListener(e -> {
      try {
        if (lastFocusedTextArea == null) {
          return;
        }
        new DeleteStepOperation(lastFocusedTextArea).execute();
      }
      catch (EditException ex) {
        showError(ex);
      }
    });

		toggleBorderType.addActionUDBLListener(e -> {
      AbstractSchrittView schritt = hauptSequenz.findeSchritt(lastFocusedTextArea);
      if (schritt != null) {
        SchrittSequenzView sequenz = schritt.getParent();
        sequenz.toggleBorderType(schritt);
        addEdit(new UndoableToggleStepBorder(Specman.this, schritt, sequenz));
        diagrammAktualisieren(schritt.getFirstEditArea());
      }
		});

		speichern.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				diagrammSpeichern(false);
			}
		});

		speichernUnter.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				diagrammSpeichern(true);
			}
		});

		laden.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				diagrammLaden();
			}
		});

		exportAsPDFMenuItem.addActionListener(e -> exportAsPDF());

		exportAsGraphvizMenuItem.addActionListener(e -> exportAsGraphviz());

		exitMenuItem.addActionListener(e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));

		review.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				hauptSequenz.zusammenklappenFuerReview();
			}
		});

		zoom.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				int prozentNeu = ((ZoomFaktor)zoom.getSelectedItem()).getProzent();
				int prozentAlt = skalieren(prozentNeu);
				undoManager.addEdit(new UndoableDiagrammSkaliert(Specman.this, prozentAlt));
			}
		});

		birdsview.addActionListener(e -> {
            if (hauptSequenzContainer == null) {
                return;
            }
            final int breite = hauptSequenzContainer.getBounds().width;
            final int hoehe = hauptSequenzContainer.getBounds().height;
            final Image i = createImage(breite, hoehe);
            Graphics g = i.getGraphics();
            hauptSequenzContainer.paint(g);
            final JPanel p = new JPanel();
            p.setLayout(new BorderLayout());
            final Image scaledImage = i.getScaledInstance(breite / 5, hoehe / 5,  Image.SCALE_SMOOTH);
            ImageIcon icon = new ImageIcon(scaledImage);
            final JLabel l = new JLabel(icon);
            p.add(l, BorderLayout.CENTER);
            final JDialog d = new JDialog();
            d.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    int skalierteBreite = 0;
                    int skalierteHoehe = 0;
                    float breitenFaktor = (float)p.getSize().width / breite;
                    float hoehenFaktor = (float)p.getSize().height / hoehe;
                    if (breitenFaktor > hoehenFaktor) {
                        skalierteHoehe = p.getSize().height;
                        skalierteBreite = (int)(breite * hoehenFaktor);
                    } else {
                        skalierteBreite = p.getSize().width;
                        skalierteHoehe = (int)(hoehe * breitenFaktor);
                    }
                    if (skalierteBreite > 0 && skalierteHoehe > 0) {
                        Image neuSkaliert = i.getScaledInstance(skalierteBreite, skalierteHoehe,  Image.SCALE_SMOOTH);
                        l.setIcon(new ImageIcon(neuSkaliert));
                    }
                }
            });
            d.getContentPane().add(p);
            d.pack();
            d.setVisible(true);
        });

		aenderungenUebernehmen.addActionListener(e -> {
			try (UndoRecording ur = composeUndo()) {
				int changesMade = 0;
				changesMade += intro.aenderungenUebernehmen();
				intro.aenderungsmarkierungenEntfernen(null);
				changesMade += hauptSequenz.aenderungenUebernehmen(Specman.this);
				changesMade += outro.aenderungenUebernehmen();
				outro.aenderungsmarkierungenEntfernen(null);
				if (changesMade > 0) {
					diagrammAktualisieren(null);
				} else {
					JOptionPane.showMessageDialog(this, "Das Diagramm enthält keine Änderungen.");
				}
			}
			catch (EditException ex) {
				showError(ex);
			}
		});

		aenderungenVerwerfen.addActionListener(e -> {
			try (UndoRecording ur = composeUndo()) {
				int changesReverted = hauptSequenz.aenderungenVerwerfen(Specman.this);
				if (changesReverted > 0) {
					diagrammAktualisieren(null);
				} else {
					JOptionPane.showMessageDialog(this, "Das Diagramm enthält keine Änderungen.");
				}
			}
			catch (EditException ex) {
				showError(ex);
			}
    });

	}

  @Override
	public void resyncStepnumberStyleUDBL() {
    hauptSequenz.resyncStepnumberStyleUDBL();
	}

	public void addImageViaFileChooser() {
		try {
			if (lastFocusedTextArea != null) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setCurrentDirectory(new File("."));
				fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Images", "jpg", "png", "gif", "bmp"));
				fileChooser.setAcceptAllFileFilterUsed(true);
				int result = fileChooser.showOpenDialog(arbeitsbereich);
				if (result == JFileChooser.APPROVE_OPTION) {
					File selectedFile = fileChooser.getSelectedFile();
						if (selectedFile != null && selectedFile.exists()) {
							BufferedImage image = ImageIO.read(selectedFile);
							lastFocusedTextArea.addImage(image);
					}
				}
			}
		}
		catch(IOException iox) {
			throw new RuntimeException(iox);
		}
	}

	public void addTable(int columns, int rows) {
		if (lastFocusedTextArea != null) {
			EditArea nextFocusArea = lastFocusedTextArea.addTable(columns, rows, initialArt());
			diagrammAktualisieren(nextFocusArea);
		}
	}

	@Override
	public void toggleListItem(boolean ordered) {
		if (lastFocusedTextArea != null) {
			EditArea nextFocusArea = lastFocusedTextArea.toggleListItemUDBL(ordered, initialArt());
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
	private void zoomFaktorAnzeigeAktualisieren(int prozent) {
		ZoomFaktor faktor = ZoomFaktor.valueOf("Faktor_" + zoomFaktor);
		List<ActionListener> listeners = Arrays.asList(zoom.getActionListeners());
		listeners.forEach(l -> zoom.removeActionListener(l));
		zoom.setSelectedItem(faktor);
		listeners.forEach(l -> zoom.addActionListener(l));
	}

	private void diagrammSpeichern(boolean dateiauswahlErzwingen) {
		try(ScrollPause sp = pauseScrolling()) {
			if (diagrammDatei == null || dateiauswahlErzwingen) {
				File verzeichnis = (diagrammDatei != null) ? diagrammDatei.getParentFile() : null;
				JFileChooser fileChooser = new JFileChooser(verzeichnis);
				fileChooser.setFileFilter(new FileNameExtensionFilter("Nassi Diagramme", "nsd"));
				if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
					return;
				String ausgewaehlterDateiname = fileChooser.getSelectedFile().getAbsolutePath();
				if (!ausgewaehlterDateiname.endsWith(PROJEKTDATEI_EXTENSION))
					ausgewaehlterDateiname += PROJEKTDATEI_EXTENSION;
				File ausgewaehlteDatei = new File(ausgewaehlterDateiname);
				if (!ausgewaehlteDatei.equals(diagrammDatei) && ausgewaehlteDatei.exists()) {
					int confirmErgebnis = JOptionPane.showConfirmDialog(this,
							"Die ausgewählte Datei existiert bereits.\nSoll die Datei überschrieben werden?",
							"Datei überschreiben?", JOptionPane.OK_CANCEL_OPTION);
					if (confirmErgebnis == JOptionPane.CANCEL_OPTION)
						return;
				}
				setDiagrammDatei(new File(ausgewaehlterDateiname));
			}
      // Generating the model includes cleaning up text edit areas which in turn runs setText which
      // in turn causes the scroll position to be changed. Therefore, the temporarily pause scrolling.
			StruktogrammModel_V001 model = generiereStruktogrammModel(true);
			ModelEnvelope wrappedModel = wrapModel(model);

			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.enableDefaultTyping();
			byte[] json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(wrappedModel);
			FileOutputStream fos = new FileOutputStream(diagrammDatei);
			fos.write(json);
			fos.close();

			recentFiles.add(diagrammDatei);
			undoManager.discardAllEdits();
		}
		catch (JsonProcessingException jpx) {
      displayException(jpx);
		}
    catch (IOException e) {
      displayException(e);
		}
	}

	private ModelEnvelope wrapModel(StruktogrammModel_V001 model) {
		ModelEnvelope envelope = new ModelEnvelope();
		envelope.model = model;
		envelope.modelType = model.getClass().getName();
		envelope.specmanVersion = SpecmanVersion.getVersion();
		return envelope;
	}

	private void diagrammLaden() {
		File verzeichnis = (diagrammDatei != null) ? diagrammDatei.getParentFile() : null;
		JFileChooser fileChooser = new JFileChooser(verzeichnis);
		fileChooser.setFileFilter(new FileNameExtensionFilter("Nassi Diagramme", "nsd"));
		if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			diagrammLaden(fileChooser.getSelectedFile());
			pdfExportChooser = null;
		}
	}

	public void diagrammLaden(File diagramFile) {
		try {
      focusHistory.clear();
			aenderungenVerfolgen.setSelected(false);
			dropWelcomeMessage();
			postInitSchritte = new ArrayList<AbstractSchrittView>();
			setDiagrammDatei(diagramFile);

			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.enableDefaultTyping();
			ModelEnvelope envelope = objectMapper.readValue(diagrammDatei, ModelEnvelope.class);
			StruktogrammModel_V001 model = (StruktogrammModel_V001)envelope.model;

			zoomFaktor = model.zoomFaktor;
			zoomFaktorAnzeigeAktualisieren(zoomFaktor);
      KlappButton.scaleIcons(zoomFaktor, 0);
			diagrammbreite = model.breite;
			intro.setEditorContent(model.intro);
			outro.setEditorContent(model.outro);
      pdfExportOptions = model.pdfExportOptions;
			setName(model.name);
			hauptSequenz = new SchrittSequenzView(this, null, model.hauptSequenz);

			hauptSequenzInitialisieren();
			neueSchritteNachinitialisieren();
			quellZielZuweisung(model.queryAllSteps());
			hauptSequenz.viewsNachinitialisieren();
      intro.viewsNachinitialisieren();
      intro.registerAllExistingStepnumbers();
      outro.viewsNachinitialisieren();
      outro.registerAllExistingStepnumbers();
			aenderungenVerfolgen.setSelected(model.changeModeenabled);
			recentFiles.add(diagramFile);
			undoManager.discardAllEdits();
		}
		catch (IOException e) {
      displayException(e);
		}
	}

	private void quellZielZuweisung(List<AbstractSchrittModel_V001> allModelSteps) {
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

	private void neueSchritteNachinitialisieren() {
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
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					diagrammbreiteSetzen(diagrammbreite);
					hauptSequenzContainer.setVisible(true);
					if (editArea != null) {
						editArea.requestFocus();
					}
					scrollPane.getViewport().setViewPosition(viewPosition);
				}
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

		einfachenSchrittAnhaengen = new ActionUDBLButton();
		whileSchrittAnhaengen = new ActionUDBLButton();
		whileWhileSchrittAnhaengen = new ActionUDBLButton();
		ifElseSchrittAnhaengen = new ActionUDBLButton();
		ifSchrittAnhaengen = new ActionUDBLButton();
		caseSchrittAnhaengen = new ActionUDBLButton();
		subsequenzSchrittAnhaengen = new ActionUDBLButton();
		breakSchrittAnhaengen = new ActionUDBLButton();
		catchSchrittAnhaengen = new ActionUDBLButton();
		caseAnhaengen = new ActionUDBLButton();
		exportPDF = new JButton();
		einfaerben = new ActionUDBLButton();
		loeschen = new ActionUDBLButton();
		toggleBorderType = new ActionUDBLButton();
		review = new JButton();
		birdsview = new JButton();
		aenderungenVerfolgen = new JToggleButton();
		aenderungenUebernehmen = new JButton();
		aenderungenVerwerfen = new JButton();
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
		toolbarButtonHinzufuegen(einfachenSchrittAnhaengen, "einfacher-schritt", "Einfachen Schritt anhängen", buttonBar);
		toolbarButtonHinzufuegen(whileSchrittAnhaengen, "while-schritt", "While-Schritt anhängen", buttonBar);
		toolbarButtonHinzufuegen(whileWhileSchrittAnhaengen, "whilewhile-schritt", "While-While-Schritt anhängen", buttonBar);
		toolbarButtonHinzufuegen(ifElseSchrittAnhaengen, "ifelse-schritt", "If-Else-Schritt anhängen", buttonBar);
		toolbarButtonHinzufuegen(ifSchrittAnhaengen, "if-schritt", "If-Schritt anhängen", buttonBar);
		toolbarButtonHinzufuegen(caseSchrittAnhaengen, "case-schritt", "Case-Schritt anhängen", buttonBar);
		toolbarButtonHinzufuegen(subsequenzSchrittAnhaengen, "subsequenz-schritt", "Subsequenz anhängen", buttonBar);
		toolbarButtonHinzufuegen(breakSchrittAnhaengen, "break-schritt", "Break anhängen", buttonBar);
		toolbarButtonHinzufuegen(catchSchrittAnhaengen, "catch-schritt", "Catchblock anhängen", buttonBar);
		toolbarButtonHinzufuegen(caseAnhaengen, "zweig", "Case anhängen", buttonBar);
		//toolBar.addSeparator();   //ToDo
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

		//ToDo SideBar Change from contentPane.add(toolBar, CC.xywh(1, 1, 1, 1));
		contentPane.add(toolBar, CC.xywh(1, 1, 2, 1));
		contentPane.add(buttonBar, CC.xy(1, 3));
		pack();
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
		//TODO
		DragMouseAdapter dragButtonAdapter = new DragMouseAdapter(this);
		addDragAdapter(einfachenSchrittAnhaengen,dragButtonAdapter);
		addDragAdapter(whileSchrittAnhaengen,dragButtonAdapter);
		addDragAdapter(whileWhileSchrittAnhaengen,dragButtonAdapter);
		addDragAdapter(ifElseSchrittAnhaengen,dragButtonAdapter);
		addDragAdapter(ifSchrittAnhaengen,dragButtonAdapter);
		addDragAdapter(caseSchrittAnhaengen,dragButtonAdapter);
		addDragAdapter(subsequenzSchrittAnhaengen,dragButtonAdapter);
		addDragAdapter(breakSchrittAnhaengen,dragButtonAdapter);
		addDragAdapter(catchSchrittAnhaengen,dragButtonAdapter);
		//TODO caseAnhängen
		addDragAdapter(caseAnhaengen,dragButtonAdapter);


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

	private JToolBar toolBar;
	private JToolBar buttonBar; // Sidebar ergänzt
	private ActionUDBLButton einfachenSchrittAnhaengen;
	private ActionUDBLButton whileSchrittAnhaengen;
	private ActionUDBLButton whileWhileSchrittAnhaengen;
	private ActionUDBLButton ifElseSchrittAnhaengen;
	private ActionUDBLButton ifSchrittAnhaengen;
	private ActionUDBLButton caseSchrittAnhaengen;
	private ActionUDBLButton subsequenzSchrittAnhaengen;
	private ActionUDBLButton breakSchrittAnhaengen;
	private ActionUDBLButton catchSchrittAnhaengen;
	private ActionUDBLButton caseAnhaengen;
	private JButton exportPDF;
	private ActionUDBLButton einfaerben;
	private ActionUDBLButton loeschen;
	private ActionUDBLButton toggleBorderType;
	private JButton review;
	private JButton birdsview;
	private JButton aenderungenUebernehmen;
	private JButton aenderungenVerwerfen;
	private JComboBox<ZoomFaktor> zoom;
	private JToggleButton aenderungenVerfolgen;
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

	public StruktogrammModel_V001 generiereStruktogrammModel(boolean formatierterText) {
		StruktogrammModel_V001 model = new StruktogrammModel_V001(
			getName(),
			diagrammbreite,
			zoomFaktor,
			aenderungenVerfolgen(),
			hauptSequenz.generiereSchrittSequenzModel(formatierterText),
			intro.editorContent2Model(formatierterText),
			outro.editorContent2Model(formatierterText),
      pdfExportOptions);
		return model;
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
		if (pdfExportChooser == null) {
			pdfExportChooser = new PDFExportChooser();
		}
    pdfExportChooser.initFromModel(pdfExportOptions);
		int result = pdfExportChooser.showSaveDialog(scrollPane, diagrammDatei);
		if (result == JFileChooser.APPROVE_OPTION) {
			pdfExportChooser.safeUserPreferences();
      pdfExportOptions = pdfExportChooser.getExportOptions();
		  File selectedFile = pdfExportChooser.getSelectedFile();
			if (selectedFile != null) {
				Point scrollPosition = scrollPane.getViewport().getViewPosition();
				Point workingAreaLocation = arbeitsbereich.getLocation();
				workingAreaLocation.translate(scrollPosition.x, scrollPosition.y);
				Shape all = new Shape(workingAreaLocation)
					.add(intro.getShape())
					.add(hauptSequenz.getShapeSequence())
					.add(breitenAnpasser.getShape())
					.add(outro.getShape());
        try {
          new PDFRenderer(selectedFile.getAbsolutePath(),
            pdfExportChooser.getSelectedPageSize(),
            pdfExportChooser.isPortrait(),
            pdfExportChooser.getPaging(), zoomFaktor).render(all);
        }
        catch(IOException iox) {
          displayException(iox);
        }
				if (pdfExportChooser.displayResult()) {
					try {
						Desktop.getDesktop().open(selectedFile);
					}
					catch(IOException iox) {
            displayException(iox);
					}
				}
			}
    }
	}

	public int zoomFaktor() { return zoomFaktor; }

	public static boolean istHauptSequenz(SchrittSequenzView schrittSequenzView) {
		return Specman.instance.hauptSequenz == schrittSequenzView;
	}

	public static EditorContentModel_V001 initialtext(String text) { return initialtext(text, null); }

	public static EditorContentModel_V001 initialtext(String text, @Nullable String align) {
    List<Markup_V001> markups = new ArrayList<>();
    if (instance().aenderungenVerfolgen()) {
      markups.add(new Markup_V001(0, text.length()-1, MarkupType.Changed));
    }
    String styledText = (align != null)
      ? "<div align='" + align + "'>" + text + "</div>"
      : text;
    TextEditAreaModel_V001 textModel = new TextEditAreaModel_V001(styledText, text, markups, initialArt());
		return new EditorContentModel_V001(textModel);
	}

	public static Color schrittHintergrund() {
		return (instance() != null && instance().aenderungenVerfolgen()) ?
			AENDERUNGSMARKIERUNG_HINTERGRUNDFARBE : BACKGROUND_COLOR_STANDARD;
	}

	//Methode um die Aenderugnsart von neuen Schritten auf hinzugefügt zu ändern, wenn die Änderungsverfolgung aktiviert ist
	public static Aenderungsart initialArt() {
		return (instance() != null && instance().aenderungenVerfolgen()) ?
				Hinzugefuegt : Untracked;
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

	public JButton getEinfachenSchrittAnhaengen() {
		return einfachenSchrittAnhaengen;
	}

	public JButton getWhileSchrittAnhaengen() {
		return whileSchrittAnhaengen;
	}

	public JButton getWhileWhileSchrittAnhaengen() {
		return whileWhileSchrittAnhaengen;
	}

	public JButton getIfElseSchrittAnhaengen() {
		return ifElseSchrittAnhaengen;
	}

	public JButton getIfSchrittAnhaengen() {
		return ifSchrittAnhaengen;
	}

	public JButton getCaseSchrittAnhaengen() {
		return caseSchrittAnhaengen;
	}

	public JButton getSubsequenzSchrittAnhaengen() {
		return subsequenzSchrittAnhaengen;
	}

	public JButton getBreakSchrittAnhaengen() {
		return breakSchrittAnhaengen;
	}

	public JButton getCatchSchrittAnhaengen() {
		return catchSchrittAnhaengen;
	}

	public JButton getCaseAnhaengen() {
		return caseAnhaengen;
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

	/**
	 * Returns a step if the provided stepnumber matches.
	 * This could be optimized by skipping all steps after the position where the step is supposed to be.
	 * <p>
	 * However, this case shouldn't be called since that means we are searching for a non-existing step which
	 * currently isn't possible - Except due to a bug, that's why we throw the Exception above.
	 */
	public boolean isKeyPressed(int keyCode) {
		return pressedKeys.contains(keyCode);
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

  private void displayException(Exception x) {
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
    try(UndoRecording ur = composeUndo()) {
      new DeleteStepOperation(step, initiatingFragment).execute();
    }
    catch (EditException ex) {
      showError(ex);
    }
  }

  @Override
  public void moveBranchSequenceLeftUDBL(AbstractSchrittView step, InteractiveStepFragment initiatingFragment) {
    try(UndoRecording ur = composeUndo()) {
      new MoveBranchSequenceLeftOperation(step, initiatingFragment).execute();
    }
    catch (EditException ex) {
      showError(ex);
    }
  }

  @Override
  public void moveBranchSequenceRightUDBL(AbstractSchrittView step, InteractiveStepFragment initiatingFragment) {
    try(UndoRecording ur = composeUndo()) {
      new MoveBranchSequenceRightOperation(step, initiatingFragment).execute();
    }
    catch (EditException ex) {
      showError(ex);
    }
  }

  @Override
  public int showConfirmDialog(String message, String title, int optionType) {
    return JOptionPane.showConfirmDialog(this, message, title, optionType);
  }

}

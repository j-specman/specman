package specman.editarea;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import net.atlanticbb.tantlinger.shef.HTMLEditorPane;
import specman.Aenderungsart;
import specman.ChangeSet;
import specman.EditorI;
import specman.SchrittID;
import specman.TextInit;
import specman.ChangeInfo;
import static specman.ChangeSet.changeset;
import specman.editarea.document.WrappedDocument;
import specman.editarea.document.WrappedPosition;
import specman.editarea.stepnumberlabel.StepnumberLabel;
import specman.model.v001.AbstractEditAreaModel_V001;
import specman.model.v001.EditorContentModel_V001;
import specman.model.v001.ImageEditAreaModel_V001;
import specman.model.v001.ListItemEditAreaModel_V001;
import specman.model.v001.TableEditAreaModel_V001;
import specman.model.v001.TextEditAreaModel_V001;
import specman.undo.UndoableListItemDissolved;
import specman.undo.props.UDBL;
import specman.pdf.Shape;
import specman.undo.UndoableEditAreaRemoved;
import specman.undo.UndoableEditAreaAdded;
import specman.undo.manager.UndoRecording;
import specman.view.AbstractSchrittView;
import specman.view.CatchUeberschrift;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ComponentListener;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import specman.graphics.Styles;
import static specman.graphics.Styles.BACKGROUND_COLOR_STANDARD;
import static specman.graphics.Styles.SCHRITTNR_FONTSIZE;
import static specman.graphics.Styles.labelFont;
import static specman.model.v001.EditorContentModel_V001.empty;
import static specman.Specman.editor;

/** Zentrales grafisches Containerpanel für einen zusammenhängenden Text mit einem Nummernlabel
 * für Schrittbeschreibungen. Normalerweise besteht diese Beschreibung aus einem einzelnen HTML
 * Text-Editorbereich (siehe Klasse {@link TextEditArea}). Sollen aber in einer Beschreibung auch
 * Bilder oder Tabellen auftauchen, dann werden diese als separate grafische Elemente in den Container
 * aufgenommen. Im Prinzip können Bilder und Tabellen auch in dem HTML enthalten sein (siehe Klasse
 * {@link HTMLEditorPane}), aber das hat einige entscheidende Limitierungen.
 * <ul>
 *   <li>Bilder werden innerhalb des HTMLs von HTMLEditorPane ziemlich pixelig skaliert</li>
 *   <li>Bilder werden im HTML außerdem ausschließlich im Sinne von Referenzen auf separate
 *    Dateien integriert. Die selbstgebaute Komponente {@link ImageEditArea} sorgt für eine schönere
 *    Skalierung und kann die Grafiken auch ins Diagramm <i>eingebettet</i> verwalten, wie man das
 *    von Microsoft Word kennt. Das ist meistens sinnvoller, weil sich ja sonst Inhalte der Beschreibung
 *    verändern können, ohne dass der User an dem Dokument gearbeitet hat.</li>
 *   <li>Tabellen direkt im HTMLEditorPane geben dem User zu wenig Bearbeitungskomfort.</li>
 * </ul>
 * Außerdem lässt der direkte Support von Bildern und Tabellen im HTMLEditorPane keine vernünftige
 * Integration mit dem Änderungsmodus zu, um Veränderungen für den User zu visualisieren. Und
 * schließlich passt das auch nicht mit dem PDF-Export zusammen, der für eine pixelgenaue
 * Platzierung von Texten die Inhalte von Textfeldern Zeile für Zeile rendered. Das hat zur Folge,
 * dass das Textrendering ausschließlich auf Texten operieren kann. Grafische Bestandteile müssen
 * also grundsätzlich separate Bereiche bilden.
 * <p>
 * Der Container kümmert sich auch um die abgesetzte Darstellung von Schritten und um ein damit
 * zusammenhängendes ärgerliches Grafikproblem in Swing:
 * Wenn sich ein Textfeld im Randbereich eines Schritts mit abgerundeten Ecken befinden, dann
 * muss es ein wenig eingerückt werden, damit der editierbare Bereich nicht unter der Abrundung
 * liegt. Das würde man normalerweise mit einer Border oder einem Margin für das Textfeld lösen.
 * Leider entsteht dann aber eine Unschönheit: sobald man das Textfeld anklickt und darin editiert,
 * legt Swing das Feld samt seiner Umrandung zeitweise in den Vordergrund. Die Abrundung wird also
 * für die Dauer des Editierens mit der Hintergrundfarbe der Border überdeckt. Die Klasse hier
 * löst dieses Problem. Sie ist selbst ein Panel, in dem das Textfeld über ein FormLayout auf
 * Abstand zum Rand gehalten wird. Auf diese Weise tritt der Effekt nicht auf.
 * <p>
 * Mit dem gleichen Kniff löst die Klasse das Problem, wenn sich das Textfeld am oberen oder unteren
 * Rand einer abgerundeten Umrahmung befindet. Die Rahmenlinie wird nämlich mit Antialiazing gezeichnet
 * (siehe Klasse {@link specman.view.RoundedBorderDecorator}, was zu einem leichten "Verschwimmen" der
 * Horizontallinien führt. In dem Fall muss das äußerste Pixel des oberen bzw. unteren Randabstands
 * des Textfeldes von dem Abstandspanel hier kommen und nicht von einer Border, weil sonst die Rahmenlinien
 * während des Editierens leicht angeknabbert aussehen.
 * <p>
 * Kann man dann nicht <i>alles</i> über die Klasse hier machen, statt dem Textfeld überhaupt noch
 * eine Border zu geben? Leider Nein, denn das Textfeld besitzt ja auch noch sein Schrittnummer-Label,
 * und dieses muss am Rand seiner eigenen Border platziert werden, um bündig mit den umgebenden
 * Rahmenlinien des Schrittes platziert zu werden, zu dem das Textfeld gehört. Wir müssen also
 * situationsbedingt beide Techniken mischen.
 */
public class EditContainer extends JPanel {
	private final static RowSpec EDITAREA_LAYOUT_ROWSPEC = RowSpec.decode("fill:pref");

	// ACHTUNG: Das ist hier noch auf halbem Wege. Später wird es eine Liste von EditAreas geben
	private final List<EditArea> editAreas = new ArrayList<>();
	private final List<FocusListener> editAreasFocusListeners = new ArrayList<>();
	private final List<ComponentListener> editAreasComponentListeners = new ArrayList<>();
	private final StepnumberLabel schrittNummer;
	private FormLayout layout;
	private Indentions indentions;
	private boolean schrittNummerSichtbar = true;

	public EditContainer(TextEditArea initialContent, SchrittID schrittId) {
		this(new EditorContentModel_V001(), schrittId);
		addEditArea(initialContent, 0);
    skalieren(editor().getZoomFactor(), 0);
	}

	public EditContainer(EditorContentModel_V001 initialContent, SchrittID schrittId) {
		if (schrittId != null) {
			schrittNummer = new StepnumberLabel(schrittId);
			setEnabled(false);
		} else {
			schrittNummer = null;
		}

		initLayoutAndEditAreas(initialContent);
		updateDecorationIndentions(new Indentions());

    skalieren(editor().getZoomFactor(), 0);

    addEditAreasFocusListener(editor());
  }

	private void initLayout(EditorContentModel_V001 content) {
		layout = new FormLayout("0px,10px:grow,0px", "0px,0px");
		setLayout(layout);
	}

	private void initLayoutAndEditAreas(EditorContentModel_V001 content) {
		editAreas.stream().forEach(ea -> remove(ea.asComponent()));
		editAreas.clear();
		initLayout(content);
		int index = 0;
		for (AbstractEditAreaModel_V001 editAreaModel: content.areas) {
			EditArea editArea;
			if (editAreaModel instanceof TextEditAreaModel_V001) {
				TextEditAreaModel_V001 textEditAreaModel = (TextEditAreaModel_V001)editAreaModel;
				editArea = new TextEditArea(textEditAreaModel, Styles.DEFAULTFONT);
			}
			else if (editAreaModel instanceof ImageEditAreaModel_V001) {
				ImageEditAreaModel_V001 imageEditAreaModel = (ImageEditAreaModel_V001)editAreaModel;
				editArea = new ImageEditArea(imageEditAreaModel);
			}
			else if (editAreaModel instanceof TableEditAreaModel_V001) {
				TableEditAreaModel_V001 tableEditAreaModel = (TableEditAreaModel_V001)editAreaModel;
				editArea = new TableEditArea(tableEditAreaModel);
			}
			else if (editAreaModel instanceof ListItemEditAreaModel_V001) {
				ListItemEditAreaModel_V001 listItemEditAreaModel = (ListItemEditAreaModel_V001)editAreaModel;
				editArea = listItemEditAreaModel.ordered
					? new OrderedListItemEditArea(listItemEditAreaModel)
					: new UnorderedListItemEditArea(listItemEditAreaModel);
			}
			else {
				throw new RuntimeException("Was soll das denn sein? " + editAreaModel);
			}
			addEditArea(editArea, index++);
		}
		if (schrittNummer != null) {
			if (editAreas.isEmpty()) {
				System.err.println("Can't set step number in " + schrittNummer);
			}
			else {
				editAreas.get(0).addSchrittnummer(schrittNummer);
			}
		}
		skalieren(editor().getZoomFactor(), 0);
	}

	public EditContainer() {
		this(empty(), null);
	}

	public EditContainer(ChangeInfo changeInfo) {
		this(new EditorContentModel_V001("", changeInfo), null);
	}

	/** Entfernt im Rahmen der Übernahme oder Rücknahme von Änderungen alle Einfärbungen,
	 * die im Änderungsmodus entstanden und nicht <i>inhaltlicher</i> Natur sind.
	 * <ul>
	 *   <li>Wurde ein Edit Container im Änderungsmodus hinzugefügt, verschoben oder gelöscht,
	 *   hat er einen hellgelben Hintergrund bekommen, der jetzt wieder auf weis geändert werden
	 *   muss</li>
	 *   <li>In einem {@link EditContainer} eines gelöschten Schritts wurde die Schrift
	 *   im Änderungsmodus auf grau mit schwarzem Hintergrund gesetzt, was im Falle einer
	 *   Rücknahme der Löschung wieder geändert werden muss.</li>
	 *   <li>Letzteres gilt auch für eine etwaige, Im {@link EditContainer} enthaltene
	 *   Schrittnummer.</li>
	 * </ul>
	 * Einfärbungen für <i>inhaltliche</i> Änderungen spielen hier keine Rolle. Diese werden
	 * bereits <i>vor</i> dem Aufruf der Methode hier über {@link #aenderungenUebernehmen}
	 * bzw. {@link #aenderungenVerwerfen()} behandelt. */
	public void aenderungsmarkierungenEntfernen(SchrittID id) {
		editAreas.forEach(EditArea::aenderungsmarkierungenEntfernen);
		setBackground(BACKGROUND_COLOR_STANDARD);
		if (schrittNummer != null) {
			schrittNummer.setStandardStyle(id);
		}
	}

	public void setZielschrittStilUDBL(SchrittID quellschrittId, ChangeSet changeSet) {
		schrittNummer.setTargetStyleUDBL(quellschrittId, changeSet);
	}

	public void setQuellStil(SchrittID zielschrittID, ChangeSet changeSet) {
		editAreas.forEach(ea -> ea.setQuellStil(changeSet));
		setBackground(changeSet.panelColor());
		schrittNummer.setSourceStyle(zielschrittID);
	}

	public void setGeloeschtMarkiertStilUDBL(SchrittID id, ChangeSet changeSet) {
		setBackgroundUDBL(changeSet.panelColor());
		modifyableEditAreas().forEach(ea -> ea.setGeloeschtMarkiertStilUDBL(changeSet));
		if (schrittNummer != null) {
			schrittNummer.setDeletedStyleUDBL(id);
		}
	}

	public void setId(SchrittID id) {
		schrittNummer.setStepNumber(id);
	}

	public EditorContentModel_V001 editorContent2Model(boolean formatierterText) {
		List<AbstractEditAreaModel_V001> areaModels = editAreas.stream().map(ea -> ea.toModel(formatierterText)).collect(Collectors.toList());
		return new EditorContentModel_V001(areaModels);
	}

	public String getPlainText() {
		StringBuilder sb = new StringBuilder();
		editAreas.forEach(ea -> sb.append(ea.getPlainText()));
		return sb.toString();
	}

	public void updateBounds() {
		int maxEditWidth = getMaxEditAreasWidth();
		// The method may be triggered by component resizing events which in certain
		// constellations may find all edit areas having size 0. In these situations
		// we completely omit any size recalculations which otherwize would cause
		// the containers and/or step number label to disappear. We rely on that there
		// will follow additional calls with reasonable initialized components sizes.
		if (maxEditWidth > 0) {
			if (schrittNummer != null) {
				if (schrittNummerSichtbar) {
					Dimension schrittnummerGroesse = schrittNummer.getPreferredSize();
					schrittNummer.setBounds(maxEditWidth - schrittnummerGroesse.width,
						0,
						schrittnummerGroesse.width,
						schrittnummerGroesse.height - 2);
				} else {
					schrittNummer.setBounds(0, 0, 0, 0);
				}
			}
		}
	}

	private int getMaxEditAreasWidth() {
		int maxWidth = 0;
		for (EditArea area: editAreas) {
			maxWidth = Math.max(maxWidth, area.getWidth());
		}
		return maxWidth;
	}

	public void schrittnummerAnzeigen(boolean sichtbar) {
		if (schrittNummer != null) {
			schrittNummerSichtbar = sichtbar;
			updateBounds(); // Sorgt dafür, dass der Label auch optisch sofort verschwindet
		}
	}

	public void skalieren(int prozentNeu, int prozentAktuell) {
		editAreas.forEach(ea -> ea.skalieren(prozentNeu, prozentAktuell));
		if (schrittNummer != null) {
			schrittNummer.setFont(labelFont.deriveFont((float) SCHRITTNR_FONTSIZE * prozentNeu / 100));
		}
		if (indentions != null) {
			updateDecorationIndentions(indentions);
		}
	}

	public static EditorContentModel_V001 right(String text) {
		return TextInit.initialtext(text, "right");
	}

	public static EditorContentModel_V001 center(String text) {
		return TextInit.initialtext(text, "center");
	}

	public Container getKlappButtonParent() { return schrittNummer.getParent(); }

	public void addEditAreasFocusListener(FocusListener focusListener) {
		editAreasFocusListeners.add(focusListener);
		editAreas.forEach(ea -> ea.addFocusListener(focusListener));
	}

	@Override
	public void setOpaque(boolean isOpaque) {
		super.setOpaque(isOpaque);
		// Null-Check ist notwendig, weil javax.swing.LookAndFeel bereits
		// im Konstruktor einen Aufruf von setBackground vornimmt.
		if (editAreas != null) {
			editAreas.forEach(ea -> ea.setOpaque(isOpaque));
		}
	}

	@Override
	public Color getBackground() {
		// Null- und Size-Check ist notwendig, weil javax.swing.LookAndFeel bereits
		// im Konstruktor einen Aufruf von getBackground vornimmt.
		return editAreas != null && editAreas.size() > 0 ? editAreas.get(0).getBackground() : super.getBackground();
	}

	@Override
	public void setBackground(Color bg) {
		super.setBackground(bg);
		if (editAreas != null) {
			editAreas.forEach(ea -> ea.setEditBackgroundUDBL(bg));
		}
	}

	public void updateDecorationIndentions(Indentions indentions) {
		this.indentions = indentions.withIndividuals(this.indentions);

		layout.setRowSpec(1, indentions.topInset());
		layout.setRowSpec(editAreas.size()+2, indentions.bottomInset());
		layout.setColumnSpec(1, indentions.leftInset());
		layout.setColumnSpec(3, indentions.rightInset());

		// TODO JL: Das ist hier noch nicht sauber. Der oberste und unterste Editbereich haben
		//  unterschiedliche Top- und Bottom-Insets
		editAreas.forEach(ea -> ea.setEditDecorationIndentions(this.indentions));
	}

	public Rectangle getStepNumberBounds() { return schrittNummer.getBounds(); }

	public void resyncStepnumberAsTargetUDBL(SchrittID quellschrittId) { schrittNummer.resyncSourceSuffixUDBL(quellschrittId); }
	public void resyncStepnumberAsSourceUDBL(SchrittID zielschrittID) { schrittNummer.resyncTargetSuffixUDBL(zielschrittID); }

	/** Required for iterations that may modify the list of edit areas. Working directly on the
	 * list whould cause concurrent operation exceptions in these cases. The usage of this method
	 * is sometimes unintuitive. E.g. removing change marks actually may cause an {@link ImageEditArea}
	 * to be removed from the list. */
	private List<EditArea> modifyableEditAreas() { return new ArrayList<>(editAreas); }

	public int aenderungenUebernehmen() {
        return modifyableEditAreas().stream().mapToInt(EditArea::aenderungenUebernehmen).sum();
	}

	public int aenderungenVerwerfen() {
		int changes = modifyableEditAreas().stream().mapToInt(EditArea::aenderungenVerwerfen).sum();
		if (changes != 0) {
			// This second phase is required for the case that discarding changes caused the removal
			// of edit areas which and in turn may cause the merge of text edit areas. In this case
			// the text from the trailing text area may still have change marks which are removed
			// by this second run.
			changes += aenderungenVerwerfen();
		}
		return changes;
	}

	public boolean enthaelt(InteractiveStepFragment fragment) {
		return editAreas.stream().anyMatch(ea -> ea.enthaelt(fragment))
			|| schrittNummer == fragment;
	}

	public InteractiveStepFragment asInteractiveFragment() { return editAreas.get(0); }

	public EditArea addTableUDBL(TextEditArea initiatingTextArea, int columns, int rows) {
		EditorI editor = editor();
		TableEditArea tableEditArea;
		try (UndoRecording ur = editor.composeUndo()) {
			int initiatingTextAreaIndex = indexOf(initiatingTextArea);
			WrappedPosition initiatingCaretPosition = initiatingTextArea.getWrappedCaretPosition();
			tableEditArea = new TableEditArea(columns, rows, TextInit.initialChangeInfo());
			addEditArea(tableEditArea, initiatingTextAreaIndex+1);
			TextEditArea cutOffTextArea = initiatingTextArea.split(initiatingCaretPosition);
			if (cutOffTextArea != null) {
				addEditArea(cutOffTextArea, initiatingTextAreaIndex+2);
			}
			editor.addEdit(new UndoableEditAreaAdded(this, initiatingTextArea, tableEditArea, cutOffTextArea));
		}
		updateBounds();
		return tableEditArea;
	}

	public void addImageUDBL(BufferedImage image, TextEditArea initiatingTextArea) {
		EditorI editor = editor();
		try (UndoRecording ur = editor.composeUndo()) {
			int initiatingTextAreaIndex = indexOf(initiatingTextArea);
			WrappedPosition initiatingCaretPosition = initiatingTextArea.getWrappedCaretPosition();
			ImageEditArea imageEditArea = new ImageEditArea(image, TextInit.initialChangeInfo());
			addEditArea(imageEditArea, initiatingTextAreaIndex+1);
			TextEditArea cutOffTextArea = initiatingTextArea.split(initiatingCaretPosition);
			if (cutOffTextArea != null) {
				addEditArea(cutOffTextArea, initiatingTextAreaIndex+2);
			}
			editor.addEdit(new UndoableEditAreaAdded(this, initiatingTextArea, imageEditArea, cutOffTextArea));
			editor.diagrammAktualisieren(initiatingTextArea);
		}
		updateBounds();
	}

	public EditArea addListItemUDBL(TextEditArea initiatingTextArea, boolean ordered) {
		EditorI editor = editor();
		AbstractListItemEditArea liEditArea;
		try (UndoRecording ur = editor.composeUndo()) {
			int initiatingTextAreaIndex = indexOf(initiatingTextArea);
			ParagraphCutter cutter = new ParagraphCutter().cutAtCaret(initiatingTextArea);
			liEditArea = ordered
				? new OrderedListItemEditArea(cutter.caretTextArea(), TextInit.initialChangeInfo())
				: new UnorderedListItemEditArea(cutter.caretTextArea(), TextInit.initialChangeInfo());
			addEditArea(liEditArea, initiatingTextAreaIndex+1);
			editor.addEdit(new UndoableEditAreaAdded(this, initiatingTextArea, liEditArea, null));
			if (cutter.trailingTextArea() != null) {
				addEditArea(cutter.trailingTextArea(), initiatingTextAreaIndex+2);
				editor.addEdit(new UndoableEditAreaAdded(this, initiatingTextArea, cutter.trailingTextArea(), null));
			}
		}
		updateBounds();
		return liEditArea;
	}

	public void addListItem(AbstractListItemEditArea initiatingListItemEditArea, AbstractListItemEditArea splitListItemEditArea) {
		int initiatingIndex = indexOf(initiatingListItemEditArea);
		addEditArea(splitListItemEditArea, initiatingIndex+1);
	}

	public TextEditArea addTextEditArea(ImageEditArea initiatingImageEditArea) {
		int initiatingIndex = indexOf(initiatingImageEditArea);
		TextEditAreaModel_V001 addedModel = new TextEditAreaModel_V001("", "", new ArrayList<>(), TextInit.initialChangeInfo());
		TextEditArea newEditArea = new TextEditArea(addedModel, getFont());
		addEditArea(newEditArea, initiatingIndex+1);
		return newEditArea;
	}

	public void appendTextEditArea(TextEditArea area) {
		addEditArea(area, editAreas.size());
	}

	private void addEditArea(EditArea editArea, int index) {
		editAreasFocusListeners.forEach(fl -> editArea.asComponent().addFocusListener(fl));
		editAreasComponentListeners.forEach(cl -> editArea.asComponent().addComponentListener(cl));
		editAreas.add(index, editArea);
		layout.setRowSpec(index+2, EDITAREA_LAYOUT_ROWSPEC);
		add(editArea.asComponent(), CC.xy(2, index+2));
		for (int followerIndex = index+1; followerIndex < editAreas.size(); followerIndex++) {
			EditArea followerArea = editAreas.get(followerIndex);
			layout.setRowSpec(followerIndex+2, EDITAREA_LAYOUT_ROWSPEC);
			layout.setConstraints(followerArea.asComponent(), CC.xy(2, followerIndex + 2));
		}
		layout.appendRow(RowSpec.decode("0px"));
	}

	public void removeEditAreaUDBL(EditArea editarea) {
		EditorI editor = editor();
		try (UndoRecording ur = editor.composeUndo()) {
			TextEditArea leadingTextArea = null;
			TextEditArea trailingTextArea = null;
			int editAreaIndex = removeEditAreaComponent(editarea);
			if (editAreaIndex > 0) {
				leadingTextArea = editAreas.get(editAreaIndex-1).asTextArea();
				if (editAreas.size() > editAreaIndex) {
					trailingTextArea = editAreas.get(editAreaIndex).asTextArea();
					if (leadingTextArea != null && trailingTextArea != null) {
						removeEditAreaComponent(trailingTextArea);
						leadingTextArea.appendText(trailingTextArea);
						leadingTextArea.requestFocus();
					}
				}
			}
			editor.addEdit(new UndoableEditAreaRemoved(this, leadingTextArea, editarea, trailingTextArea));
		}
		updateBounds();
	}

	@Override
	public void requestFocus() {
		EditArea firstArea = getFirstEditArea();
		if (firstArea != null) {
			firstArea.requestFocus();
		}
		else {
			super.requestFocus();
		}
	}

	public void removeEditAreaByUndoRedo(EditArea editArea, TextEditArea cutOffTextArea) {
		try (UndoRecording ur = editor().pauseUndo()) {
			removeEditAreaComponent(editArea);
			if (cutOffTextArea != null) {
				removeEditAreaComponent(cutOffTextArea);
			}
		}
		updateBounds();
	}

	int removeEditAreaComponent(EditArea area) {
		int index = indexOf(area);
		editAreas.remove(area);
		remove(area.asComponent());
		for (int followerIndex = index; followerIndex < editAreas.size(); followerIndex++) {
			EditArea followerArea = editAreas.get(followerIndex);
			layout.setConstraints(followerArea.asComponent(), CC.xy(2, followerIndex + 2));
		}
		layout.removeRow(editAreas.size()+3);
		return index;
	}

	public void setEditorContent(EditorContentModel_V001 content) {
		initLayoutAndEditAreas(content);
	}

	public void mergeChangeSetUDBL(ChangeSet target, ChangeSet source, boolean withMarkups) {
		editAreas.forEach(ea -> ea.mergeChangeSetUDBL(target, source, withMarkups));
	}

	public void addEditComponentListener(ComponentListener componentListener) {
		editAreasComponentListeners.add(componentListener);
		editAreas.forEach(ea -> addComponentListener(componentListener));
	}

	public boolean enthaeltAenderungsmarkierungen() {
		return editAreas.stream().anyMatch(ea -> ea.enthaeltAenderungsmarkierungen());
	}

	public specman.pdf.Shape getShape() {
		Shape shape = new Shape(this);
		for (int i = 0; i < editAreas.size(); i++) {
			Shape areaShape = editAreas.get(i).getShape();
			if (i == 0 && schrittNummer != null && schrittNummerSichtbar) {
				areaShape.add(schrittNummer.getShape());
			}
			shape.add(areaShape);
		}
		return shape;
	}

	public void findStepnumberLinkIDs(HashMap<TextEditArea, List<String>> stepnumberLinkMap) {
		for (EditArea editArea : editAreas) {
			editArea.findStepnumberLinkIDs(stepnumberLinkMap);
		}
	}

	public void setBackgroundUDBL(Color bg) {
		UDBL.setBackgroundUDBL(this, bg);
		if (editAreas != null) {
			editAreas.forEach(ea -> ea.setEditBackgroundUDBL(bg));
		}
	}

	public boolean isMarkedAs(Aenderungsart aenderungsart) {
		return editAreas.stream().allMatch(ea -> {
			ChangeInfo ci = ea.getChangeInfo();
			return ci.art() == aenderungsart && ci.changeSet() == changeset();
		});
	}

	public List<JTextComponent> getTextAreas() {
    List<JTextComponent> result = new ArrayList<>();
    editAreas.stream().forEach(ea -> result.addAll(ea.getTextAreas()));
    return result;
	}

	public int indexOf(EditArea initiatingEditArea) {
		return editAreas.indexOf(initiatingEditArea);
	}

	public List<EditArea> removeEditAreaComponents(int fromIndex) {
		List<EditArea> removedAreas = new ArrayList<>();
		while(editAreas.size() > fromIndex) {
			EditArea area = editAreas.get(fromIndex);
			removeEditAreaComponent(area);
			removedAreas.add(area);
		}
		return removedAreas;
	}

	public void addEditAreas(List<EditArea> areas) {
		int addIndex = editAreas.size();
		for(EditArea area: areas) {
			addEditArea(area, addIndex++);
		}
	}

	public Integer getFirstLineHeight() {
		TextEditArea firstArea = editAreas.get(0).asTextArea();
		return firstArea.getFirstLineHeight();
	}

	public int getItemNumber(OrderedListItemEditArea item) {
		int itemIndex = indexOf(item);
		int itemNumber = 1;
		while(editAreas.get(itemIndex-itemNumber).isOrderedListItemArea()) {
			itemNumber++;
		}
		return itemNumber;
	}

	private boolean hasAreas() { return editAreas != null && !editAreas.isEmpty(); }

	public int getBaseline() {
		FontMetrics metrics = getFontMetrics(getFont());
		return metrics.getHeight() - metrics.getDescent() + getTopMargin();
	}

	@Override
	public Font getFont() { return hasAreas() ? getFirstEditArea().asComponent().getFont() : super.getFont(); }

	public int getTopMargin() {
		return hasAreas() ? getFirstEditArea().asTextArea().getMargin().top : 0;
	}

	public EditArea getFirstEditArea() {
		return getEditAreaAt(0);
	}

	public EditArea getLastEditArea() {
		return hasAreas() ? editAreas.get(editAreas.size()-1) : null;
	}

	public EditArea getEditAreaAt(int index) {
		return hasAreas() ? editAreas.get(index) : null;
	}

	public void addEditAreaByUndoRedo(EditArea initiatingTextArea, EditArea imageEditArea, TextEditArea cutOffTextArea) {
		try (UndoRecording ur = editor().pauseUndo()) {
			int initiatingTextAreaIndex = indexOf(initiatingTextArea);
			addEditArea(imageEditArea, initiatingTextAreaIndex+1);
			if (cutOffTextArea != null) {
				addEditArea(cutOffTextArea, initiatingTextAreaIndex+2);
			}
		}
		updateBounds();
	}

	public void mergeListItemAreasByUndoRedo(AbstractListItemEditArea initiatingArea, AbstractListItemEditArea splitArea) {
		try (UndoRecording ur = editor().pauseUndo()) {
			// The very first edit content in the split list item is ignored. It was created by
			// splitting the last text area of the initiating list item which is already restored
			// by undo of its text modification.
			List<EditArea> areasToMerge = splitArea.content.removeEditAreaComponents(1);
			initiatingArea.content.addEditAreas(areasToMerge);
			initiatingArea.getParent().removeEditAreaByUndoRedo(splitArea, null);
		}
		updateBounds();
	}

	public EditArea tryDissolveEditAreaUDBL(TextEditArea initiatingTextEditArea) {
		// The text is the leading one in a list item
		if (getParent() instanceof AbstractListItemEditArea) {
			// Dissolve the list item by merging its content into the upper edit container
			AbstractListItemEditArea liEditArea = (AbstractListItemEditArea)getParent();
			EditArea lastLiftUp = liEditArea.getParent().dissolveListItemEditAreaUDBL(liEditArea);
			editor().diagrammAktualisieren(lastLiftUp);
      return lastLiftUp;
		}
		// The preceeding edit area is a list item
		else {
			int areaIndex = indexOf(initiatingTextEditArea);
			if (areaIndex > 0) {
				EditArea preceedingArea = editAreas.get(areaIndex-1);
				if (preceedingArea.isListItemArea()) {
					TextEditArea nextToFocus = mergeTextIntoListItem(preceedingArea.asListItemArea(), initiatingTextEditArea);
					editor().diagrammAktualisieren(nextToFocus);
          return nextToFocus;
				}
			}
		}
    return null;
	}

	private TextEditArea mergeTextIntoListItem(AbstractListItemEditArea target, TextEditArea initiatingTextEditArea) {
		EditorI editor = editor();
		try (UndoRecording ur = editor.composeUndo()) {
			TextEditArea nextToFocus = target.appendText(initiatingTextEditArea);
			removeEditAreaComponent(initiatingTextEditArea);
			// TODO JL: Undo recording!
			return nextToFocus;
		}
	}

	/** Remove the passed list item edit area from this edit container and add its content areas directly instead.
	 * If the preceeding edit area is a text area, merge the first text edit area of the list item with it.
	 * If the succeeding edit area is a text area and the list item's last edit area es well, merge them. */
	public EditArea dissolveListItemEditAreaUDBL(AbstractListItemEditArea liEditArea) {
		EditorI editor = editor();
		List<EditArea> liftUpAreas = liEditArea.content.modifyableEditAreas();
		EditArea lastLiftUpArea = null;
		TextEditArea followingTextEditArea = null;
		try (UndoRecording ur = editor.composeUndo()) {
			boolean leadingTextMergeRequired = leadingTextMergeOnDissolve(liEditArea);
			boolean trailingTextMergeRequired = trailingTextMergeOnDissolve(liEditArea);
			int liEditAreaIndex = removeEditAreaComponent(liEditArea);
			if (leadingTextMergeRequired) {
				TextEditArea preceedingText = editAreas.get(liEditAreaIndex-1).asTextArea();
				TextEditArea firstLiText = liEditArea.getFirstEditArea().asTextArea();
				preceedingText.appendText(firstLiText);
				liEditArea.removeEditAreaComponent(firstLiText);
				lastLiftUpArea = preceedingText;
			}
			List<EditArea> areas = liEditArea.removeEditAreaComponents(0);
			for (int i = 0; i < areas.size(); i++) {
				lastLiftUpArea = areas.get(i);
				addEditArea(lastLiftUpArea, liEditAreaIndex + i);
			}
			if (trailingTextMergeRequired) {
				int lastLiftUpAreaIndex = indexOf(lastLiftUpArea);
				followingTextEditArea = editAreas.get(lastLiftUpAreaIndex+1).asTextArea();
        TextEditArea lastLiftUpTextArea = lastLiftUpArea.asTextArea();
        lastLiftUpTextArea.appendText(followingTextEditArea);
				removeEditAreaComponent(followingTextEditArea);
        if (!leadingTextMergeRequired) {
          WrappedDocument lastLiftUpAreaDoc = lastLiftUpTextArea.getWrappedDocument();
          lastLiftUpTextArea.setCaretPosition(lastLiftUpAreaDoc.start().unwrap());
        }
			}
			editor.addEdit(new UndoableListItemDissolved(this, liEditArea, liEditAreaIndex, liftUpAreas, followingTextEditArea));
		}
		updateBounds();
		return lastLiftUpArea;
	}

	/** Returns true, if dissolving the passed edit area requires a merge of its
	 * last content edit area with a text area being located directly after the
	 * list item itself. */
	private boolean trailingTextMergeOnDissolve(AbstractListItemEditArea liEditArea) {
		int liEditAreaIndex = indexOf(liEditArea);
		return liEditAreaIndex < editAreas.size() - 1 &&
			editAreas.get(liEditAreaIndex + 1).isTextArea() &&
			liEditArea.getLastEditArea().isTextArea();
	}

	/** Returns true, if dissolving the passed edit area requires a merge of its
	 * first content text area with a text area being located directly before the
	 * list item itself. */
	private boolean leadingTextMergeOnDissolve(AbstractListItemEditArea liEditArea) {
		int liEditAreaIndex = indexOf(liEditArea);
		return liEditAreaIndex > 0 &&
			editAreas.get(liEditAreaIndex - 1).isTextArea() &&
			liEditArea.getFirstEditArea().isTextArea();
	}

	public void undoDissolveListItemEditArea(AbstractListItemEditArea liEditArea, int liEditAreaIndex, List<EditArea> liftUpAreas, TextEditArea followingTextEditArea) {
		try (UndoRecording ur = editor().pauseUndo()) {
			addEditArea(liEditArea, liEditAreaIndex);
			int lastLiftUpIndex = liEditAreaIndex + 1;
			for (EditArea liftUpArea : liftUpAreas) {
				if (indexOf(liftUpArea) != -1) {
					lastLiftUpIndex = indexOf(liftUpArea);
					removeEditAreaComponent(liftUpArea);
				}
			}
			if (followingTextEditArea != null) {
				addEditArea(followingTextEditArea, lastLiftUpIndex);
			}
			liEditArea.content.addEditAreas(liftUpAreas);
		}
	}

	public EditArea redoDissolveListItemEditArea(AbstractListItemEditArea liEditArea, int liEditAreaIndex, List<EditArea> liftUpAreas, TextEditArea followingTextEditArea) {
		try (UndoRecording ur = editor().pauseUndo()) {
			if (leadingTextMergeOnDissolve(liEditArea)) {
				liftUpAreas = liftUpAreas.subList(1, liftUpAreas.size());
			}
			removeEditAreaComponent(liEditArea);
			for (EditArea liftUpArea : liftUpAreas) {
				addEditArea(liftUpArea, liEditAreaIndex++);
			}
			if (followingTextEditArea != null) {
				removeEditAreaComponent(followingTextEditArea);
			}
			return getFirstEditArea();
		}
	}

  public void registerAllExistingStepnumbers() {
    EditorI editor = editor();
    HashMap<TextEditArea, List<String>> stepnumberLinks = new HashMap<>();
    findStepnumberLinkIDs(stepnumberLinks);
    for (Map.Entry<TextEditArea, List<String>> listEntry : stepnumberLinks.entrySet()) {
      TextEditArea referencingTextEditArea = listEntry.getKey();
      List<String> stepIDs = listEntry.getValue();

      for (String stepID : stepIDs) {
        if (!StepnumberLink.isStepnumberLinkDefect(stepID)) {
          AbstractSchrittView step = editor.findStepByStepID(stepID);
          step.registerStepnumberLink(referencingTextEditArea);
        }
      }
    }

  }

  public void scrollTo() {
    // By Dec. 25, the first edit area is always a text area
    TextEditArea firstArea = getFirstEditArea().asTextArea();
    scrollRectToVisible(firstArea.getBounds());
    // Setting the caret position may already cause scrolling, but only if the position
    // is different from the one which is already set in that text edit area. That's why
    // the scrolling above is also necessary.
    firstArea.setCaretPosition(1);
    // It is important to request the focus as otherwise the viewport may immediately
    // scroll back to the edit area where we came from.
    firstArea.requestFocus();
  }

  public CatchUeberschrift containingCatchHeading() {
    Container container = getParent();
    if (container instanceof CatchUeberschrift ) {
      return (CatchUeberschrift)container;
    }
    // TODO JL: check also for containing table edit area and list edit area and traverse upwards
    return null;
  }

  public void viewsNachinitialisieren() {
    editAreas.forEach(ea -> ea.viewsNachinitialisieren());
  }
}

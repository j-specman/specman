package specman.editarea;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import specman.Aenderungsart;
import specman.EditorI;
import specman.SpaltenContainerI;
import specman.SpaltenResizer;
import specman.Specman;
import specman.editarea.focusmover.CrossEditAreaFocusMoverFromImage;
import specman.editarea.stepnumberlabel.StepnumberLabel;
import specman.model.v001.ImageEditAreaModel_V001;
import specman.pdf.Shape;
import specman.pdf.ShapeImage;
import specman.undo.UndoableEditAreaAdded;
import specman.undo.manager.UndoRecording;
import specman.undo.props.UDBL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import static specman.Aenderungsart.Geloescht;
import static specman.Aenderungsart.Untracked;
import static specman.styles.Styles.AENDERUNGSFARBE;
import static specman.view.AbstractSchrittView.FORMLAYOUT_GAP;
import static specman.view.AbstractSchrittView.LINIENBREITE;

public class ImageEditArea extends JPanel implements EditArea<ImageEditAreaModel_V001>,
  FocusListener, MouseListener, KeyListener, ComponentListener, SpaltenContainerI {
  public static final String PERSISTED_IMAGETYPE = "png";

  private static final String AFTERIMAGELINE_GAP = FORMLAYOUT_GAP;
  private static final int IRRELEVANT_COLUMNRESIZE_INDEX = -1;
  static final Color FOCUS_BORDER_COLOR = Color.GRAY;
  private static final int BORDER_THICKNESS = 1;
  private static final Border SELECTED_BORDER = new CompoundBorder(
    new EmptyBorder(new Insets(BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS)),
    new LineBorder(FOCUS_BORDER_COLOR, BORDER_THICKNESS));
  private static final Border UNSELECTED_BORDER =
    new EmptyBorder(new Insets(BORDER_THICKNESS*2, BORDER_THICKNESS*2, BORDER_THICKNESS*2, BORDER_THICKNESS*2));
  private static final Border UNSELECTED_CHANGED_BORDER = new LineBorder(AENDERUNGSFARBE.text.color, BORDER_THICKNESS*2);

  private BufferedImage fullSizeImage;
  private ImageIcon scaledIcon;
  private float totalScalePercent;
  private float individualScalePercent;
  private JLabel image;
  private ImageEditAreaGlassPane focusGlass;
  private Aenderungsart aenderungsart;

  ImageEditArea(BufferedImage fullSizeImage, Aenderungsart aenderungsart) {
    this.fullSizeImage = fullSizeImage;
    this.aenderungsart = aenderungsart;
    this.individualScalePercent = 1;
    postInit();
  }

  public ImageEditArea(ImageEditAreaModel_V001 imageEditAreaModel) {
    try {
      InputStream input = new ByteArrayInputStream(imageEditAreaModel.imageData);
      this.fullSizeImage = ImageIO.read(input);
      this.aenderungsart = imageEditAreaModel.aenderungsart;
      this.individualScalePercent = imageEditAreaModel.individualScalePercent;
      postInit();
    }
    catch(IOException iox) {
      throw new RuntimeException(iox);
    }
  }

  private void postInit() {
    EditorI editor = Specman.instance();
    setLayout(new FormLayout("pref, " + AFTERIMAGELINE_GAP + ", pref:grow", "fill:pref:grow"));
    this.image = new JLabel();
    add(image, CC.xy(1, 1));
    this.add(new SpaltenResizer(this, IRRELEVANT_COLUMNRESIZE_INDEX), CC.xy(2, 1));
    setBackground(aenderungsart.toBackgroundColor());
    image.setBorder(changetype2border());
    addComponentListener(this);
    updateListenersByAenderungsart();
  }

  @Override
  public void addSchrittnummer(StepnumberLabel schrittNummer) {
    add(schrittNummer);
  }

  private void updateListenersByAenderungsart() {
    removeMouseListener(this);
    removeFocusListener(this);
    removeKeyListener(this);
    if (aenderungsart != Aenderungsart.Geloescht) {
      addMouseListener(this);
      addFocusListener(this);
      addKeyListener(this);
    }
  }

  @Override public void keyTyped(KeyEvent e) {}
  @Override public void keyReleased(KeyEvent e) {}
  @Override public void keyPressed(KeyEvent e) {
    switch (e.getKeyCode()) {
      case 'C':
        keyCopyPressed(e);
        break;
      case 'X':
        keyCutPressed(e);
        break;
      case KeyEvent.VK_BACK_SPACE:
      case KeyEvent.VK_DELETE:
        removeAreaByKeypressUDBL();
        e.consume();
        break;
      case KeyEvent.VK_ENTER:
        appendTextEditAreaByKeypressUDBL();
        e.consume();
        break;
      case KeyEvent.VK_DOWN:
        new CrossEditAreaFocusMoverFromImage(this).moveFocusToSucceedingEditArea();
        e.consume();
        break;
      case KeyEvent.VK_UP:
        new CrossEditAreaFocusMoverFromImage(this).moveFocusToPreceedingEditArea();
        e.consume();
        break;
    }
  }

  private void keyCopyPressed(KeyEvent e) {
    if (e.isControlDown()) {
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      clipboard.setContents(new ImageClipboardSelection(fullSizeImage), null);
      e.consume();
    }
  }

  private void keyCutPressed(KeyEvent e) {
    if (e.isControlDown()) {
      keyCopyPressed(e);
      removeAreaByKeypressUDBL();
    }
  }

  private void appendTextEditAreaByKeypressUDBL() {
    EditorI editor = Specman.instance();
    try (UndoRecording ur = editor.pauseUndo()){
      // Change back to unselected border before starting to record undoable operations
      setImageBorderByChangetypeUDBL();
    }
    try (UndoRecording ur = editor.composeUndo()){
      EditContainer editContainer = getParent();
      TextEditArea editArea = editContainer.addTextEditArea(ImageEditArea.this);
      editor.addEdit(new UndoableEditAreaAdded(editContainer, this, editArea, null));
      editor.diagrammAktualisieren(editArea);
    }
  }

  private void removeAreaByKeypressUDBL() {
    EditorI editor = Specman.instance();
    try (UndoRecording ur = editor.pauseUndo()){
      // Change back to unselected border before starting to record undoable operations
      setImageBorderByChangetypeUDBL();
    }
    try (UndoRecording ur = editor.composeUndo()){
      if (aenderungsart == Untracked && editor.aenderungenVerfolgen()) {
        setGeloeschtMarkiertStilUDBL();
      }
      else {
        getParent().removeEditAreaUDBL(ImageEditArea.this);
        editor.diagrammAktualisieren(null);
      }
    }
  }

  @Override public void mouseClicked(MouseEvent e) { requestFocus(); }
  @Override public void mousePressed(MouseEvent e) {}
  @Override public void mouseReleased(MouseEvent e) {}
  @Override public void mouseEntered(MouseEvent e) {}
  @Override public void mouseExited(MouseEvent e) {}

  @Override
  public void focusGained(FocusEvent e) {
    if (aenderungsart != Aenderungsart.Geloescht) {
      image.setBorder(SELECTED_BORDER);
      addGlassPanel();
    }
  }

  @Override
  public void focusLost(FocusEvent e) {
    if (aenderungsart != Aenderungsart.Geloescht) {
      image.setBorder(changetype2border());
      removeGlassPanel();
    }
  }

  private void removeGlassPanel() {
    if (focusGlass != null) {
      remove(focusGlass);
      focusGlass = null;
    }
  }

  private void addGlassPanel() {
    if (focusGlass == null) {
      focusGlass = new ImageEditAreaGlassPane(aenderungsart);
      add(focusGlass, CC.xy(1, 1));
      // Removing and re-attaching the image causes it to be drawn *below* the focus glass
      remove(image);
      add(image, CC.xy(1, 1));

      // Force the glasspanel to appear
      revalidate();
    }
  }

  private void setImageBorderByChangetypeUDBL() {
    setImageBorderUDBL(changetype2border());
  }

  private Border changetype2border() {
    return aenderungsart == Aenderungsart.Hinzugefuegt ? UNSELECTED_CHANGED_BORDER : UNSELECTED_BORDER;
  }

  public void setImageBorderUDBL(Border border) { UDBL.setBorderUDBL(image, border); }

  @Override
  public EditContainer getParent() { return (EditContainer) super.getParent(); }

  @Override
  public void setQuellStil() {
    // Not required for images - source steps only contain an empty text area
  }

  @Override
  public void aenderungsmarkierungenEntfernen() {
    aenderungsart = Untracked;
  }

  @Override
  public void setGeloeschtMarkiertStilUDBL() {
    if (aenderungsart == Untracked) {
      updateChangetypeAndDependentStylingUDBL(Aenderungsart.Geloescht);
      focusGlass.toDeleted();
    }
    else if (aenderungsart == Geloescht) {
      addGlassPanel();
      focusGlass.toDeleted();
    }
    else if (aenderungsart == Aenderungsart.Hinzugefuegt) {
      getParent().removeEditAreaUDBL(this); // Includes recording of required undos
    }
  }

  private void updateChangetypeAndDependentStylingUDBL(Aenderungsart aenderungsart) {
    setAenderungsartUDBL(aenderungsart);
    setImageBorderByChangetypeUDBL();
    setEditBackgroundUDBL(null);
  }

  public Aenderungsart getAenderungsart() { return aenderungsart; }

  public void setAenderungsart(Aenderungsart aenderungsart) {
    this.aenderungsart = aenderungsart;
    updateListenersByAenderungsart();
    if (aenderungsart == Aenderungsart.Geloescht) {
      addGlassPanel();
    }
    else {
      removeGlassPanel();
    }
  }

  public void setAenderungsartUDBL(Aenderungsart aenderungsart) {
    UDBL.setAenderungsart(this, aenderungsart);
  }

  @Override
  public Component asComponent() { return this; }

  @Override
  public ImageEditAreaModel_V001 toModel(boolean formatierterText) {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ImageIO.write(fullSizeImage, PERSISTED_IMAGETYPE, bytes);
      return new ImageEditAreaModel_V001(bytes.toByteArray(), PERSISTED_IMAGETYPE, aenderungsart, individualScalePercent);
    }
    catch (IOException iox) {
      throw new RuntimeException(iox);
    }
  }

  @Override
  public String getPlainText() { return ""; }

  @Override
  public void skalieren(int prozentNeu, int prozentAktuell) {
    // Nothing to do: image is automacically resized by the pack() methode if necessary
  }

  @Override
  public int aenderungenUebernehmen() {
    int changesMade = aenderungsart.asNumChanges();
    switch (aenderungsart) {
      case Hinzugefuegt -> updateChangetypeAndDependentStylingUDBL(Untracked);
      case Geloescht -> getParent().removeEditAreaUDBL(this);
    }
    aenderungsart = Untracked;
    return changesMade;
  }

  @Override
  public int aenderungenVerwerfen() {
    int changesReverted = aenderungsart.asNumChanges();
    switch(aenderungsart) {
      case Hinzugefuegt -> getParent().removeEditAreaUDBL(this);
      case Geloescht -> updateChangetypeAndDependentStylingUDBL(Untracked);
    }
    aenderungsart = Untracked;
    return changesReverted;
  }

  @Override public String getText() { return "image"; }
  @Override public TextEditArea asTextArea() { return null; }
  @Override public ImageEditArea asImageArea() { return this; }
  @Override public boolean isImageEditArea() { return true; }

  @Override
  public boolean enthaeltAenderungsmarkierungen() { return aenderungsart != null; }

  @Override
  public void findStepnumberLinkIDs(HashMap<TextEditArea, List<String>> stepnumberLinkMap) {
    // There are no stepnumberLinks in an ImageArea
  }

  @Override
  public void setEditBackgroundUDBL(Color bg) {
    setBackgroundUDBL(aenderungsart.toBackgroundColor());
  }

  public void setBackgroundUDBL(Color bg) {
    UDBL.setBackgroundUDBL(this, bg);
  }

  @Override
  public void setEditDecorationIndentions(Indentions indentions) {
    Border border = new EmptyBorder(
      indentions.topBorder(),
      indentions.leftBorder(),
      indentions.bottomBorder(),
      indentions.rightBorder());
    setBorder(border);
  }

  @Override
  public boolean enthaelt(InteractiveStepFragment fragment) { return false; }

  public Shape getShape() {
    return new Shape(this)
      .add(new Shape(BORDER_THICKNESS, 0)
        .withImage(new ShapeImage(this)));
  }

  public BufferedImage getFullSizeImage() { return fullSizeImage; }

  public float getTotalScalePercent() { return totalScalePercent; }

  private void adaptImageSize() {
    int availableWidth = getWidth() - 3 * LINIENBREITE;
    if (availableWidth > 0) {
      int maximumZoomedWidth = (int)(fullSizeImage.getWidth() * Specman.instance().getZoomFactor() / 100 * individualScalePercent);
      int scaledWidth = Math.min(availableWidth, maximumZoomedWidth);
      if (scaledIcon == null || scaledWidth != scaledIcon.getIconWidth()) {
        totalScalePercent = (float)scaledWidth / (float)fullSizeImage.getWidth();
        scaledIcon = new ImageIcon(fullSizeImage
          .getScaledInstance((int)(fullSizeImage.getWidth() * totalScalePercent),
            (int)(fullSizeImage.getHeight() * totalScalePercent), Image.SCALE_SMOOTH));
        image.setIcon(scaledIcon);
      }
    }
  }

  @Override
  /** On component resize we scale the image if necessary, depending on the available width.
   * If the component width exceeds the image width, we display the image in full size.
   * Otherwise, we scale it down to fit into the available width. */
  public void componentResized(ComponentEvent e) {
    adaptImageSize();
  }

  @Override public void componentMoved(ComponentEvent e) {}
  @Override public void componentShown(ComponentEvent e) {}
  @Override public void componentHidden(ComponentEvent e) {}

  @Override
  public int spaltenbreitenAnpassenNachMausDragging(int delta, int spalte) {
    float newIndividualScalePercent = 1 + (float) delta / scaledIcon.getIconWidth();
    individualScalePercent *= newIndividualScalePercent;
    adaptImageSize();
    return delta;
  }

  @Override
  public List<JTextComponent> getTextAreas() { return List.of(); }

  @Override
  public void viewsNachinitialisieren() {
    if (aenderungsart == Geloescht) {
      setGeloeschtMarkiertStilUDBL();
    }
  }
}
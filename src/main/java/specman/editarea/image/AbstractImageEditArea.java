package specman.editarea.image;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import org.jetbrains.annotations.NotNull;
import specman.ChangeInfo;
import specman.ChangeSet;
import specman.EditorI;
import specman.SpaltenContainerI;
import specman.SpaltenResizer;
import specman.editarea.EditArea;
import specman.editarea.EditContainer;
import specman.editarea.ImageClipboardSelection;
import specman.editarea.Indentions;
import specman.editarea.InteractiveStepFragment;
import specman.editarea.TextEditArea;
import specman.editarea.focusmover.CrossEditAreaFocusMoverFromImage;
import specman.model.v002.AbstractImageEditAreaModel_V002;
import specman.pdf.Shape;
import specman.pdf.ShapeBitmapImage;
import specman.pdf.ShapeImage;
import specman.undo.UndoableEditAreaAdded;
import specman.undo.manager.UndoRecording;
import specman.undo.props.UDBL;
import specman.visitor.DiagramVisitor;
import specman.visitor.StopTraversalException;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
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

import static specman.ChangeSet.changeset;
import static specman.Specman.editor;
import static specman.view.AbstractSchrittView.FORMLAYOUT_GAP;
import static specman.view.AbstractSchrittView.LINIENBREITE;

/** Everything an edit area showing a picture shares - focus, border, change tracking, undo, key handling and
 * scaling to the available width. Subclasses only say how big the picture naturally is and how to draw it
 * at a given scale, see {@link #intrinsicWidth()} and {@link #createScaledIcon(float)}. */
public abstract class AbstractImageEditArea extends JPanel implements EditArea<AbstractImageEditAreaModel_V002>,
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

  private Icon scaledIcon;
  private float totalScalePercent;
  protected float individualScalePercent;
  private JLabel image;
  private ImageEditAreaGlassPane focusGlass;
  /** Where the arrow key navigation came in horizontally, in this area's coordinates. Null if it didn't,
   * e.g. after a mouse click. See comment on {@link specman.editarea.focusmover.AbstractCrossEditAreaFocusMover}
   * for details on the arrow key navigation concept. */
  private Integer navigationX;
  private ChangeInfo changeInfo;

  protected AbstractImageEditArea(ChangeInfo changeInfo, float individualScalePercent) {
    this.changeInfo = changeInfo;
    this.individualScalePercent = individualScalePercent;
    postInit();
  }

  /** @return the width of the picture at 100%, in pixels */
  protected abstract int intrinsicWidth();

  /** @return the picture drawn at the given fraction of its natural size */
  protected abstract Icon createScaledIcon(float totalScalePercent);

  /** @return a bitmap of the picture at its natural size, e.g. for the clipboard and the PDF export */
  public abstract BufferedImage getFullSizeImage();

  private void postInit() {
    setLayout(new FormLayout("pref, " + AFTERIMAGELINE_GAP + ", pref:grow", "fill:pref:grow"));
    this.image = new JLabel();
    add(image, CC.xy(1, 1));
    this.add(new SpaltenResizer(this, IRRELEVANT_COLUMNRESIZE_INDEX), CC.xy(2, 1));
    setBackground(panelColor());
    image.setBorder(changetype2border());
    addComponentListener(this);
    updateListenersByChangeInfo();
  }

  private void updateListenersByChangeInfo() {
    removeMouseListener(this);
    removeFocusListener(this);
    removeKeyListener(this);
    if (!changeInfo.isDeleted()) {
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
        deleteUDBL();
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

  /** Called by the arrow key navigation when moving the focus into this image,
   * so that the next move can continue at the same x position. See comment on
   * {@link specman.editarea.focusmover.AbstractCrossEditAreaFocusMover} for
   * details. */
  public void setNavigationX(int x) {
    navigationX = x;
  }

  /** The x position, in this area's coordinates, the arrow key navigation continues from:
   * where it came in, or the image's center. */
  public int getNavigationX() {
    return navigationX != null ? navigationX : getWidth() / 2;
  }

  private void keyCopyPressed(KeyEvent e) {
    if (e.isControlDown()) {
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      clipboard.setContents(new ImageClipboardSelection(getFullSizeImage()), null);
      e.consume();
    }
  }

  private void keyCutPressed(KeyEvent e) {
    if (e.isControlDown()) {
      keyCopyPressed(e);
      deleteUDBL();
    }
  }

  private void appendTextEditAreaByKeypressUDBL() {
    EditorI editor = editor();
    try (UndoRecording ur = editor.pauseUndo()){
      // Change back to unselected border before starting to record undoable operations
      setImageBorderByChangetypeUDBL();
    }
    try (UndoRecording ur = editor.composeUndo()){
      EditContainer editContainer = getParent();
      TextEditArea editArea = editContainer.addTextEditArea(AbstractImageEditArea.this);
      editor.addEdit(new UndoableEditAreaAdded(editContainer, this, editArea, null));
      editor.diagrammAktualisieren(editArea);
    }
  }

  /** Removes the area. In change mode, an area which existed before is marked as deleted instead. */
  public void deleteUDBL() {
    EditorI editor = editor();
    try (UndoRecording ur = editor.pauseUndo()){
      // Change back to unselected border before starting to record undoable operations
      setImageBorderByChangetypeUDBL();
    }
    try (UndoRecording ur = editor.composeUndo()){
      if (changeInfo.isUntracked() && editor.aenderungenVerfolgen()) {
        setGeloeschtMarkiertStilUDBL(changeset());
      }
      else {
        getParent().removeEditAreaUDBL(AbstractImageEditArea.this);
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
    if (!changeInfo.isDeleted()) {
      image.setBorder(SELECTED_BORDER);
      addGlassPanel();
    }
  }

  @Override
  public void focusLost(FocusEvent e) {
    navigationX = null;
    if (!changeInfo.isDeleted()) {
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
      focusGlass = new ImageEditAreaGlassPane(changeInfo);
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
    return changeInfo.isAdded()
        ? new LineBorder(changeInfo.changeSet().textColor(), BORDER_THICKNESS*2)
        : UNSELECTED_BORDER;
  }

  public void setImageBorderUDBL(Border border) { UDBL.setBorderUDBL(image, border); }

  @Override
  public EditContainer getParent() { return (EditContainer) super.getParent(); }

  @Override
  public void setQuellStil(ChangeSet changeSet) {
    // Not required for images - source steps only contain an empty text area
  }

  @Override
  public void aenderungsmarkierungenEntfernen() {
    changeInfo = changeInfo.untrack(changeset());
  }

  public void mergeChangeSetUDBL(@NotNull ChangeSet target, @NotNull ChangeSet source, boolean withMarkups) {
    if (changeInfo.changedBy(source)) {
      setChangeInfoUDBL(changeInfo.reassign(target));
      setEditBackgroundUDBL(null);
    }
  }

  @Override
  public void setGeloeschtMarkiertStilUDBL(ChangeSet triggerSet) {
    if (changeInfo.isUntracked()) {
      updateChangetypeAndDependentStylingUDBL(changeInfo.deleted(triggerSet));
      focusGlass.toDeleted();
    }
    else if (changeInfo.isDeleted()) {
      addGlassPanel();
      focusGlass.toDeleted();
    }
    else if (changeInfo.isAdded()) {
      getParent().removeEditAreaUDBL(this); // Includes recording of required undos
    }
  }

  private void updateChangetypeAndDependentStylingUDBL(ChangeInfo newChangeInfo) {
    setChangeInfoUDBL(newChangeInfo);
    setImageBorderByChangetypeUDBL();
    setEditBackgroundUDBL(null);
  }

  public ChangeInfo getChangeInfo() { return changeInfo; }


  public void setChangeInfo(ChangeInfo changeInfo) {
    this.changeInfo = changeInfo;
    updateListenersByChangeInfo();
    if (changeInfo.isDeleted()) {
      addGlassPanel();
    }
    else {
      removeGlassPanel();
    }
  }

  public void setChangeInfoUDBL(ChangeInfo changeInfo) {
    UDBL.setChangeInfo(this, changeInfo);
  }

  @Override
  public Component asComponent() { return this; }

  @Override
  public String getPlainText() { return ""; }

  @Override
  public int aenderungenUebernehmen() {
    ChangeSet currentSet = changeset();
    int changesMade = changeInfo.numChangesBy(currentSet);
    if (changeInfo.addedBy(currentSet)) {
      updateChangetypeAndDependentStylingUDBL(ChangeInfo.untracked());
    }
    else if (changeInfo.deletedBy(currentSet)) {
      getParent().removeEditAreaUDBL(this);
    }
    changeInfo = changeInfo.untrack(currentSet);
    return changesMade;
  }

  @Override
  public int aenderungenVerwerfen() {
    ChangeSet currentSet = changeset();
    int changesRejected = changeInfo.numChangesBy(currentSet);
    if (changeInfo.addedBy(currentSet)) {
      getParent().removeEditAreaUDBL(this);
    }
    else if (changeInfo.deletedBy(currentSet)) {
      updateChangetypeAndDependentStylingUDBL(ChangeInfo.untracked());
    }
    changeInfo = changeInfo.untrack(currentSet);
    return changesRejected;
  }

  @Override public String getText() { return "image"; }
  @Override public TextEditArea asTextArea() { return null; }
  @Override public AbstractImageEditArea asImageArea() { return this; }
  @Override public boolean isImageEditArea() { return true; }

  /** @return the change set of the image's own change mark, or null if the image is not changed */
  public ChangeSet firstChangeSet() {
    return changeInfo.isChange() ? changeInfo.changeSet() : null;
  }

  @Override
  public void setEditBackgroundUDBL(Color bg) {
    setBackgroundUDBL(panelColor());
  }

  /** The background of the area, which marks its changes. Subclasses with changes of their own content
   * extend this. Note that it is also asked while the base class is being constructed. */
  protected Color panelColor() {
    return changeInfo.panelColor();
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
    Shape shape = new Shape(this)
      .add(new Shape(imageX(), imageY())
        .withImage(createShapeImage()));
    if (changeInfo.isDeleted() && focusGlass != null) {
      shape.add(focusGlass.getShape());
    }
    return shape;
  }

  /** How the picture gets into the PDF. Bitmap by default, subclasses with a vector representation override this. */
  protected ShapeImage createShapeImage() {
    return new ShapeBitmapImage(this);
  }

  /** The image sits inside the image label's border, left-aligned and vertically centered.
   * We need to take these details into account for precise image placement in PDF rendering. */
  private int imageX() {
    return image.getX() + image.getInsets().left;
  }

  /** Precise calculation of the image's Y position, required for exact placement in PDF rendering. */
  private int imageY() {
    Insets insets = image.getInsets();
    int innerHeight = image.getHeight() - insets.top - insets.bottom;
    return image.getY() + insets.top + (innerHeight - scaledIcon.getIconHeight()) / 2;
  }

  public float getTotalScalePercent() { return totalScalePercent; }

  /** Scales the image to the available width, but not beyond its natural size times the zoom factor
   * and the individual scale. */
  protected void adaptImageSize() {
    int availableWidth = getWidth() - 3 * LINIENBREITE;
    if (availableWidth > 0) {
      int maximumZoomedWidth = (int)(intrinsicWidth() * editor().getZoomFactor() / 100 * individualScalePercent);
      int scaledWidth = Math.min(availableWidth, maximumZoomedWidth);
      if (scaledIcon == null || scaledWidth != scaledIcon.getIconWidth()) {
        totalScalePercent = (float)scaledWidth / (float)intrinsicWidth();
        scaledIcon = createScaledIcon(totalScalePercent);
        image.setIcon(scaledIcon);
      }
    }
  }

  /** Subclasses call this after their picture changed, so it is scaled and shown anew. */
  protected void imageChanged() {
    scaledIcon = null;
    adaptImageSize();
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
  public void accept(DiagramVisitor visitor) throws StopTraversalException { visitor.visitImageArea(this); }

  public void viewsNachinitialisieren() {
    if (changeInfo.isDeleted()) {
      setGeloeschtMarkiertStilUDBL(changeInfo.changeSet());
    }
  }
}

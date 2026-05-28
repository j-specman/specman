package specman.view;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import specman.ChangeInfo;
import specman.SchrittID;
import static specman.ChangeSet.changeset;
import specman.editarea.EditContainer;
import specman.model.v001.CoCatchModel_V001;
import specman.model.v001.EditorContentModel_V001;
import specman.pdf.LineShape;
import specman.pdf.Shape;
import specman.undo.props.UDBL;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Arrays;

import static specman.graphics.Styles.BACKGROUND_COLOR_STANDARD;
import static specman.view.AbstractSchrittView.LINIENBREITE;
import static specman.view.AbstractSchrittView.ZEILENLAYOUT_INHALT_SICHTBAR;
import static specman.view.AbstractSchrittView.umgehungLayout;

public class CatchUeberschrift extends JPanel implements ComponentListener {
  final EditContainer ueberschrift;
  final CatchSchrittSequenzView catchSequence;
  BreakSchrittView linkedBreakStep;
  final FormLayout layout;
  ChangeInfo changeInfo;

  public CatchUeberschrift(EditContainer ueberschrift, BreakSchrittView linkedBreakStep, CatchSchrittSequenzView catchSequence, ChangeInfo changeInfo) {
    this.ueberschrift = ueberschrift;
    this.linkedBreakStep = linkedBreakStep;
    this.catchSequence = catchSequence;
    this.changeInfo = changeInfo;
    this.setBackground(changeInfo.panelColor());
    this.ueberschrift.setBackground(changeInfo.panelColor());
    if (isDeleted()) {
      ueberschrift.setGeloeschtMarkiertStilUDBL(linkedBreakStep.id, changeInfo.changeSet());
    }
    layout = new FormLayout(umgehungLayout() + ", 10px:grow", ZEILENLAYOUT_INHALT_SICHTBAR);
    setLayout(layout);
    add(ueberschrift, CC.xy(2, 1));
    addComponentListener(this);
  }

  @Override
  public void paint(Graphics g) {
    super.paint(g);
    dreieckZeichnen((Graphics2D)g);
  }

  private void dreieckZeichnen(Graphics2D g) {
    g.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON);
    for (LineShape line: buildTriangle()) {
      g.drawLine(
        line.start().x, line.start().y,
        line.end().x, line.end().y);
    }
  }

  private java.util.List<LineShape> buildTriangle() {
    // Not sure why we have to shift the triangle a little.
    int triangleLeft = -1;
    int hoehe = getHeight();
    int dreieckSpitzeY = hoehe / 2;
    int dreieckBasisX = ueberschrift.getX() - LINIENBREITE;
    return Arrays.asList(
      new LineShape(triangleLeft,  triangleLeft,  dreieckBasisX,  dreieckSpitzeY),
      new LineShape(dreieckBasisX,  dreieckSpitzeY, triangleLeft, hoehe));
  }

  public void skalieren(int prozentNeu, int prozentAktuell) {
    ueberschrift.skalieren(prozentNeu, prozentAktuell);
    layout.setColumnSpec(1, ColumnSpec.decode(umgehungLayout()));
  }

  public int aenderungenUebernehmen() {
    int numChanges = changeInfo.numChanges();
    if (isDeleted()) {
      remove();
    }
    else {
      numChanges += ueberschrift.aenderungenUebernehmen();
    }
    changeInfo = ChangeInfo.untracked();
    return numChanges;
  }

  public void aenderungsmarkierungenEntfernen() {
    ueberschrift.aenderungsmarkierungenEntfernen(linkedBreakStep.id);
    setBackground(BACKGROUND_COLOR_STANDARD);
  }

  public int aenderungenVerwerfen() {
    int numChanges = 0;
    if (changeInfo.isAdded()) {
      catchSequence.removeUDBL(this);
      numChanges += changeInfo.numChanges();
      changeInfo = ChangeInfo.untracked();
    }
    return numChanges + ueberschrift.aenderungenVerwerfen();
  }

  public void alsGeloeschtMarkierenUDBL() {
    changeInfo = changeInfo.deleted(changeset());
    ueberschrift.setGeloeschtMarkiertStilUDBL(linkedBreakStep.id, changeInfo.changeSet());
    UDBL.setBackgroundUDBL(this, changeset().panelColor());
  }

  @Override public void componentResized(ComponentEvent e) { ueberschrift.updateBounds(); }
  @Override public void componentMoved(ComponentEvent e) {}
  @Override public void componentShown(ComponentEvent e) {}
  @Override public void componentHidden(ComponentEvent e) {}

  public Shape getShape() {
    return new Shape(this)
      .add(ueberschrift.getShape())
      .add(buildTriangle());
  }

  public void disconnectLinkedBreakStep() {
    linkedBreakStep.catchAnkoppeln(null);
  }

  public void updateLinkedBreakStepContent() {
    if (!isDeleted()) {
      EditorContentModel_V001 content = ueberschrift.editorContent2Model(true);
      linkedBreakStep.updateContent(content);
    }
  }

  public void updateFromBreakStepContent() {
    if (!catchSequence.isDeleted() && !this.isDeleted()) {
      EditorContentModel_V001 breakStepContent = linkedBreakStep.getEditorContent(true);
      ueberschrift.setEditorContent(breakStepContent);
    }
  }

  public SchrittID linkedBreakStepId() {
    return linkedBreakStep.id;
  }

  public void scrollToBreak() {
    linkedBreakStep.scrollTo();
  }

  public CatchSchrittSequenzView containingCatchSequence() {
    return catchSequence;
  }

  public void setId(SchrittID id) {
    ueberschrift.setId(id);
    if (isPrimaryHeading()) {
      catchSequence.setId(id);
    }
  }

  public boolean isPrimaryHeading() {
    return catchSequence.isPrimaryHeading(this);
  }

  public void remove() {
    catchSequence.removeUDBL(this);
  }

  public void removeOrMarkAsDeletedUDBL() {
    catchSequence.removeOrMarkAsDeletedUDBL(this);
  }

  public void scrollTo() { ueberschrift.scrollTo(); }

  public void connectLinkedBreakStep() {
    linkedBreakStep.catchAnkoppeln(this);
  }

  public boolean allowsMoveUp() { return catchSequence.allowsMoveUp(this); }

  public boolean allowsMoveDown() { return catchSequence.allowsMoveDown(this); }

  public void moveUpUDBL() { catchSequence.moveUpUDBL(this); }

  public void moveDownUDBL() { catchSequence.moveDownUDBL(this); }

  public CoCatchModel_V001 toModel(boolean formatierterText) {
    return new CoCatchModel_V001(
      linkedBreakStepId(),
      ueberschrift.editorContent2Model(formatierterText),
      changeInfo);
  }

  public boolean allowsDeletion() {
    return catchSequence.allowsDeletion(this);
  }

  public boolean isDeleted() {
    return changeInfo.isDeleted();
  }
}

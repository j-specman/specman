package classman.editor;

import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ElementStyleModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.TextChangeModel_V001;
import classman.render.DiagramRenderer;
import classman.render.NoteBoxLayouter;
import classman.render.Scene;

import java.awt.geom.Rectangle2D;

/** The diagram which the style dialog of a note shows: one note. With a change set, the end of its text is changed. */
final class NotePreview {
  private static final String TEXT = "Lorem ipsum dolor sit amet";
  private static final String TEXT_BEFORE = "Lorem ipsum";

  private NotePreview() {
  }

  /** @return the picture of the note in the style. With a change set it has the frame of an added note, and a part of
   * its text is marked, which a real diagram never shows together: the frame stands for everything in the note. Here
   * both are shown, so that the user sees all marks next to the colors of the style. */
  static Scene scene(DiagramRenderer renderer, ElementStyleModel_V001 style, String changeSet) {
    Scene laidOut = renderer.layout(of(style, changeSet));
    if (changeSet == null) {
      return laidOut;
    }
    Rectangle2D.Double area = laidOut.elementAreas.get(0).area;
    return FramedScene.of(laidOut,
      NoteBoxLayouter.frameOf(area.width, area.height, renderer.strongColorOf(changeSet)), area);
  }

  /** @param changeSet the change set which marks the text, null for no mark
   * @return a diagram with a note which has a copy of the style. Its text is Latin filler, the same in every
   * language, so the preview needs no texts of its own. */
  private static ClassDiagramModel_V001 of(ElementStyleModel_V001 style, String changeSet) {
    NoteModel_V001 note = new NoteModel_V001(TEXT);
    note.style = style.copy();
    if (changeSet != null) {
      note.textChange = new TextChangeModel_V001(changeSet, TEXT_BEFORE);
    }
    ClassDiagramModel_V001 diagram = new ClassDiagramModel_V001(null);
    diagram.elements.add(note);
    return diagram;
  }
}

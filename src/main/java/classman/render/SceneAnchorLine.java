package classman.render;

import classman.model.v001.NoteModel_V001;

import java.util.List;

/** The dashed line from a note to the element it is attached to, the anchor of the note. It has no marks at its ends. */
public class SceneAnchorLine extends SceneConnectionLine {
    public final NoteModel_V001 note;

    public SceneAnchorLine(NoteModel_V001 note, ScenePolyline line) {
        super(line, List.of());
        this.note = note;
    }

    @Override
    public SceneAnchorLine withStrokeWidth(double width) {
        return new SceneAnchorLine(note, line.withStrokeWidth(width).withRoundCorners());
    }
}

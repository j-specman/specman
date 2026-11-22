package classman.render;

import classman.model.v001.AnchorLineModel_V001;
import classman.model.v001.NoteModel_V001;

import java.util.List;

/** The dashed line from a note to the element it is attached to, the anchor line. It has no marks at its ends. */
public class SceneAnchorLine extends SceneConnectionLine {
    public final AnchorLineModel_V001 anchorLine;
    /** The note the line starts at. */
    public final NoteModel_V001 note;

    public SceneAnchorLine(AnchorLineModel_V001 anchorLine, NoteModel_V001 note, ScenePolyline line) {
        super(line, List.of());
        this.anchorLine = anchorLine;
        this.note = note;
    }

    @Override
    public SceneAnchorLine withStrokeWidth(double width) {
        return new SceneAnchorLine(anchorLine, note, line.withStrokeWidth(width).withRoundCorners());
    }
}

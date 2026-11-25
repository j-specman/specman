package classman.render;

import classman.model.v001.AbstractBoxModel_V001;
import classman.model.v001.AttachLineModel_V001;

import java.util.List;

/** The dashed line from a note or a class to the element it is attached to: an anchor line or an association class
 * link. It has no marks at its ends. */
public class SceneAttachLine extends SceneConnectionLine {
    public final AttachLineModel_V001 attachLine;
    /** The box the line starts at. */
    public final AbstractBoxModel_V001 startBox;

    public SceneAttachLine(AttachLineModel_V001 attachLine, AbstractBoxModel_V001 startBox, ScenePolyline line) {
        super(line, List.of());
        this.attachLine = attachLine;
        this.startBox = startBox;
    }

    @Override
    public SceneAttachLine withStrokeWidth(double width) {
        return new SceneAttachLine(attachLine, startBox, line.withStrokeWidth(width).withRoundCorners());
    }
}

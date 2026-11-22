package classman.model.v001;

/** Base of the elements which are boxes with a place and a width: classes and notes. */
public abstract class AbstractBoxModel_V001 extends AbstractDiagramElementModel_V001 {
    /** Null if the source gives no position, the layout places the box then. */
    public PositionModel_V001 position;
    /** Null if the content decides about the width. The height is always computed. */
    public Integer width;
}

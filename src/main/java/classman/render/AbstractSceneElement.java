package classman.render;

/** Base of the primitives a scene consists of. All coordinates are in pixels of the diagram. */
public abstract class AbstractSceneElement {

    public abstract AbstractSceneElement translated(double dx, double dy);
}

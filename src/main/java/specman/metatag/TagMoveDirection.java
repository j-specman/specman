package specman.metatag;

/** The directions a tag can be moved in among the tags of its step. The first tag hangs directly
 * left of the step number, so moving left means moving away from the step number. */
public enum TagMoveDirection {
  LEFT(1), RIGHT(-1);

  private final int indexOffset;

  TagMoveDirection(int indexOffset) {
    this.indexOffset = indexOffset;
  }

  public int indexOffset() {
    return indexOffset;
  }

  public TagMoveDirection opposite() {
    return this == LEFT ? RIGHT : LEFT;
  }
}

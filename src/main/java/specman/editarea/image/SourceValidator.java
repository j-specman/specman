package specman.editarea.image;

/** The rules the source of a diagram must follow to be accepted by Specman. */
public interface SourceValidator {

  /** @return a message telling the user what is wrong with the source, or null if it is fine */
  String problemOf(String source);
}

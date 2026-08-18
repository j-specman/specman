package specman.editarea.document;

import javax.swing.text.Document;
import java.util.Objects;

public class WrappedPosition {
  final int position;
  final WrappedDocumentI document;

  /** This constructor is package-visible by intention as it is only allowed to
   * be used from {@link WrappedDocument} to make sure that the passed position
   * integer is always properly converted to a logical value. If it comes from
   * a model object like {@link specman.model.v002.Markup_V002}, the value is supposed
   * to be used as is, while integers from a {@link Document} might require adaption. */
  WrappedPosition(int position, WrappedDocumentI document) {
    this.document = document;
    this.position = position;
  }

  public WrappedPosition(int delta, WrappedPosition from) {
    this.document = from.document;
    this.position = from.position + delta;
  }

  public int unwrap() {
    return position + document.getVisibleTextStart();
  }

  public WrappedPosition dec(int length) {
    return new WrappedPosition(-length, this);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    WrappedPosition that = (WrappedPosition) o;
    return position == that.position;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(position);
  }

  public WrappedPosition inc(int length) {
    return new WrappedPosition(length, this);
  }

  @Override
  public String toString() {
    return "" + position;
  }

  public int toModel() { return position; }

  public boolean exists() {
    return position >= 0 && position < document.getLength();
  }

  public boolean isZero() { return position == 0; }
  public WrappedPosition dec() { return dec(1); }
  public WrappedPosition inc() { return inc(1); }
  public boolean greater(WrappedPosition than) { return position > than.position; }
  public boolean less(WrappedPosition than) { return position < than.position; };
  public int distance(WrappedPosition lhs) { return position - lhs.position; }
  public WrappedPosition max(WrappedPosition rhs) {
    return document.fromUI(Math.max(unwrap(), rhs.unwrap()));
  }
  public WrappedPosition min(WrappedPosition rhs) {
    return document.fromUI(Math.min(unwrap(), rhs.unwrap()));
  }
  public char charAt() {
    return document.getText(this, 1).charAt(0);
  }

  public boolean isLast() {
    return position == document.getLength()-1;
  }

  public WrappedPosition copy() { return new WrappedPosition(position, document); }
}

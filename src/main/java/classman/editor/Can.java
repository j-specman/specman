package classman.editor;

import javax.swing.AbstractButton;

/** The answer to the question whether the user may do something: yes, or no with the reason, which a menu shows as the
 * tooltip of an entry which can't be chosen. A no without a reason, {@link #NOT}, is for the cases where the reason
 * is obvious or not worth a text. */
final class Can {
  static final Can YES = new Can(true, "");
  static final Can NOT = new Can(false, "");

  private final boolean allowed;
  private final String reason;

  private Can(boolean allowed, String reason) {
    this.allowed = allowed;
    this.reason = reason;
  }

  /** @return no, with the reason */
  static Can not(String reason) {
    return new Can(false, reason);
  }

  /** @return yes if the condition holds, otherwise no without a reason */
  static Can IF(boolean condition) {
    return condition ? YES : NOT;
  }

  /** @return yes if the condition holds, otherwise no with the reason */
  static Can IF(boolean condition, String reason) {
    return condition ? YES : not(reason);
  }

  boolean can() {
    return allowed;
  }

  /** @return the reason for a no, empty for a yes and for a no without a reason */
  String whyNot() {
    return reason;
  }

  /** @return this if it is a no, otherwise the other answer: yes only if both are, and the first reason wins */
  Can and(Can other) {
    return allowed ? other : this;
  }

  /** Enables the button or menu entry if the answer is yes, otherwise disables it and shows the reason, if there is
   * one, as its tooltip. */
  void applyTo(AbstractButton button) {
    button.setEnabled(allowed);
    button.setToolTipText(allowed || reason.isEmpty() ? null : reason);
  }
}

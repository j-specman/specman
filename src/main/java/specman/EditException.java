package specman;

import static specman.i18n.Messages.i18n;

public class EditException extends Exception {
  public EditException(String messageKey, Object... args) {
    super(i18n(messageKey, args));
  }
}

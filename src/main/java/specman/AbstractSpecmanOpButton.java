package specman;

import javax.swing.*;

/**
 * Base class for toolbar buttons that encapsulate a {@link Specman} operation.
 * Parallel hierarchy to {@link AbstractSpecmanOp} for the same pattern — code blocks
 * extracted from the Specman monolith — but extending JButton instead, since Java
 * does not support multiple inheritance.
 * <p>
 * Subclasses implement {@link #execute()} with the operation logic. The button
 * wires itself as its own ActionListener. Icon and tooltip are set via
 * {@link Specman#toolbarButtonHinzufuegen} when the button is added to the toolbar
 * in {@code initComponents}.
 */
abstract class AbstractSpecmanOpButton extends JButton {

  protected final Specman specman;

  AbstractSpecmanOpButton(Specman specman) {
    this.specman = specman;
    registerActionListener();
  }

  void registerActionListener() {
    addActionListener(e -> {
      try {
        execute();
      }
      catch (EditException ex) {
        specman.showError(ex);
      }
    });
  }

  abstract void execute() throws EditException;

}

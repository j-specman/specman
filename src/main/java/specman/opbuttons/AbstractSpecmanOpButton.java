package specman.opbuttons;

import specman.*;

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
public abstract class AbstractSpecmanOpButton extends JButton implements SpecmanOpContextMixin {

  protected final SpecmanOpContext context;

  public AbstractSpecmanOpButton(Specman specman) {
    this.context = specman;
    registerActionListener();
  }

  void registerActionListener() {
    addActionListener(e -> {
      try {
        execute();
      }
      catch (EditException ex) {
        context.showError(ex);
      }
    });
  }

  @Override
  public SpecmanOpContext context() { return context; }

  abstract void execute() throws EditException;

}

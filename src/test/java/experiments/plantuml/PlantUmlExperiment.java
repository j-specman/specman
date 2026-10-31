package experiments.plantuml;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import specman.graphics.Styles;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.InputEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** PlantUML spike: renders PlantUML source as SVG with the ELK layout and shows it via the same
 * FlatSVGIcon that Specman uses for its icons. Edit the source on the left, zoom with Ctrl+mouse wheel. */
public class PlantUmlExperiment extends JFrame {
  private static final int RENDER_DELAY_MS = 400;
  private static final double ZOOM_STEP = 1.1;
  // Touching Styles registers Specman's shipped fonts with AWT, so PlantUML measures text with them and the
  // SVG renderer finds them by name. labelFont is Roboto, the shipped sans-serif font.
  private static final String PRELUDE = "\n!pragma layout elk"
      + "\nskinparam defaultFontName \"" + Styles.labelFont.getFamily() + "\""
      + "\nskinparam monospacedFontName \"Courier Prime\"";
  private static final String SAMPLE_SOURCE = """
      @startuml
      hide empty members

      class Order <<AggregateRoot>> {
        id
        status
        place()
        cancel()
      }
      class OrderLine {
        quantity
        price
      }
      class Customer <<AggregateRoot>> {
        id
        name
      }
      class Address <<ValueObject>> {
        street
        city
        zip
      }
      class Money <<ValueObject>> {
        amount
        currency
      }
      class Product <<AggregateRoot>> {
        sku
        name
      }
      class Invoice <<AggregateRoot>> {
        number
        issue()
      }
      class Shipment <<AggregateRoot>> {
        trackingNo
        dispatch()
      }

      Customer "1" --> "*" Order
      Customer *-- "1..*" Address
      Order *-- "1..*" OrderLine
      OrderLine --> Product
      OrderLine --> Money
      Order --> Invoice
      Order --> Shipment
      Shipment --> Address
      @enduml
      """;

  private final JTextArea sourceArea = new JTextArea(SAMPLE_SOURCE);
  private final JLabel diagramLabel = new JLabel();
  private final JLabel status = new JLabel(" ");
  private final Timer renderTimer = new Timer(RENDER_DELAY_MS, e -> render());
  private FlatSVGIcon diagramIcon;
  private double zoom = 1.0;

  public PlantUmlExperiment() {
    super("PlantUML experiment");
    renderTimer.setRepeats(false);

    sourceArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
    sourceArea.getDocument().addDocumentListener(new SimpleDocumentListener(renderTimer::restart));

    JScrollPane diagramScroll = new JScrollPane(diagramLabel);
    diagramScroll.addMouseWheelListener(e -> {
      if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
        zoomBy(e.getWheelRotation() < 0 ? ZOOM_STEP : 1 / ZOOM_STEP);
        e.consume();
      }
    });

    JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
    toolbar.add(status);

    JPanel right = new JPanel(new BorderLayout());
    right.add(toolbar, BorderLayout.NORTH);
    right.add(diagramScroll, BorderLayout.CENTER);

    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(sourceArea), right);
    split.setDividerLocation(380);
    add(split);
    setSize(1300, 900);
    setLocationRelativeTo(null);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
  }

  private void render() {
    long start = System.nanoTime();
    byte[] svg = renderSvg(sourceArea.getText());
    diagramIcon = loadIcon(svg);
    applyZoom();
    status.setText("Rendered in " + (System.nanoTime() - start) / 1_000_000 + " ms, SVG " + svg.length + " bytes");
  }

  /** Syntax errors do not throw - PlantUML renders them as an error graphic. */
  private byte[] renderSvg(String source) {
    String withPragma = source.replaceFirst("@startuml[^\\n]*", "$0" + PRELUDE);
    ByteArrayOutputStream svg = new ByteArrayOutputStream();
    try {
      new SourceStringReader(withPragma).outputImage(svg, new FileFormatOption(FileFormat.SVG));
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return svg.toByteArray();
  }

  private FlatSVGIcon loadIcon(byte[] svg) {
    try {
      return new FlatSVGIcon(new ByteArrayInputStream(svg));
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void zoomBy(double factor) {
    zoom *= factor;
    applyZoom();
  }

  private void applyZoom() {
    int width = (int) Math.round(diagramIcon.getIconWidth() * zoom);
    int height = (int) Math.round(diagramIcon.getIconHeight() * zoom);
    diagramLabel.setIcon(diagramIcon.derive(width, height));
  }

  private static class SimpleDocumentListener implements DocumentListener {
    private final Runnable onChange;

    SimpleDocumentListener(Runnable onChange) {
      this.onChange = onChange;
    }

    @Override public void insertUpdate(DocumentEvent e) { onChange.run(); }
    @Override public void removeUpdate(DocumentEvent e) { onChange.run(); }
    @Override public void changedUpdate(DocumentEvent e) { }
  }

  public static void main(String[] args) {
    FlatLightLaf.setup();
    SwingUtilities.invokeLater(() -> {
      PlantUmlExperiment experiment = new PlantUmlExperiment();
      experiment.setVisible(true);
      experiment.render();
    });
  }
}

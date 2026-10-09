package specman.metatag.generic.config;

import specman.metatag.MetaTagPanel;
import specman.metatag.generic.GenericMetaTag;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import static specman.view.AbstractSchrittView.LINIENBREITE;

/** Shows a meta tag hanging below a black line, as it would hang below the top edge of a step, at
 * the height it has in a step at the current zoom. The height is reserved from the start, so the
 * surrounding dialog keeps its size whether a tag is shown or not. */
class PreviewPanel extends JPanel {
  private static final int TAG_INDENT = 8;
  private static final int BOTTOM_PADDING = 8;

  private final JPanel tagHolder = new JPanel(new FlowLayout(FlowLayout.LEFT, TAG_INDENT, 0));

  PreviewPanel() {
    super(new BorderLayout());
    JPanel stepEdge = new JPanel();
    stepEdge.setBackground(Color.BLACK);
    stepEdge.setPreferredSize(new Dimension(1, LINIENBREITE));
    tagHolder.setBackground(Color.WHITE);
    tagHolder.setPreferredSize(new Dimension(0, MetaTagPanel.stepnumberHeight() + BOTTOM_PADDING));
    add(stepEdge, BorderLayout.NORTH);
    add(tagHolder, BorderLayout.CENTER);
  }

  /** @param config the configuration to show a tag for, null to show nothing */
  void showTag(MetaTagConfig config) {
    tagHolder.removeAll();
    if (config != null) {
      GenericMetaTag tag = new GenericMetaTag(config, null);
      tag.setFreetextEditable(false);
      tag.setPreferredSize(new Dimension(tag.getPreferredSize().width, MetaTagPanel.stepnumberHeight()));
      tagHolder.add(tag);
    }
    tagHolder.revalidate();
    tagHolder.repaint();
  }
}

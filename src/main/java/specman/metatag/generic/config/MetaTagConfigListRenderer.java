package specman.metatag.generic.config;

import specman.graphics.SvgIcon;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JList;
import java.awt.Component;
import java.io.Serial;
import java.util.HashMap;
import java.util.Map;

/** Shows a meta tag configuration in a list by its name and its icon, if it has one. */
class MetaTagConfigListRenderer extends DefaultListCellRenderer {
  @Serial
  private static final long serialVersionUID = 1L;

  private static final int ICON_SIZE = 16;

  private final Map<MetaTagConfig, Icon> icons = new HashMap<>();

  @Override
  public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    MetaTagConfig config = (MetaTagConfig) value;
    super.getListCellRendererComponent(list, config.getName(), index, isSelected, cellHasFocus);
    setIcon(config.hasIcon() ? icons.computeIfAbsent(config, c -> new SvgIcon(ICON_SIZE, c.getIconSvg())) : null);
    return this;
  }
}

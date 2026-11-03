package specman.settings;


import specman.graphics.IconReader;
import specman.graphics.SpecmanIcons;

import javax.swing.*;
import java.awt.Component;
import java.util.List;

import static specman.i18n.Messages.i18n;


public class SettingAutoSave extends AbstractSelectSetting<Integer> {

  public static final String AUTOSAVE_PREF = "autosave";

  static final List<SettingOption<Integer>> OPTIONS = List.of(
      new SettingOption<>(null, "settings.autosave.off"),
      new SettingOption<>(5, "settings.autosave.5.seconds"),
      new SettingOption<>(60, "settings.autosave.1.minute"),
      new SettingOption<>(900, "settings.autosave.15.minutes")
  );

  public SettingAutoSave() {
    super("settings.autosave", AUTOSAVE_PREF, OPTIONS);
  }

  /**
   * The combo box for the toolbar. Selecting an entry writes the preference immediately; the dialog's own
   * instance only reads it on creation, so the toolbar calls {@link #reloadFromPreferences()} after the dialog closes.
   * The closed combo box shows a clock icon in front of the selected interval instead of a label next to it,
   * the opened list shows the intervals only.
   */
  public JComponent toolbarComponent() {
    JComboBox<SettingOption<Integer>> comboBox = comboBox();
    comboBox.addActionListener(e -> savePreference());
    comboBox.setToolTipText(i18n("settings.autosave"));
    Icon icon = IconReader.readSvgIcon(SpecmanIcons.AUTO_SAVE, SpecmanIcons.SIZE_SMALL);
    ListCellRenderer<? super SettingOption<Integer>> defaultRenderer = comboBox.getRenderer();
    comboBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
      Component component = defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (component instanceof JLabel label) {
        // index -1 is the closed combo box; the renderer is shared with the list, so the icon is reset there
        label.setIcon(index == -1 ? icon : null);
      }
      return component;
    });
    return comboBox;
  }

  public static Integer getIntervalSeconds() {
    return loadValue(AUTOSAVE_PREF, OPTIONS, null);
  }

}

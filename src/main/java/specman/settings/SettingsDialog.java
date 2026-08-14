package specman.settings;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import specman.Specman;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class SettingsDialog extends JDialog {

  private final List<AbstractSetting> settings;

  public SettingsDialog(Frame owner) {
    super(owner, "Settings", true);
    settings = List.of(
        new SettingAutoSave(),
        new SettingAutoLoad()
    );
    initComponents();
  }

  private void initComponents() {
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(buildSettingsPanel(), BorderLayout.CENTER);
    JPanel south = new JPanel(new BorderLayout());
    south.add(new JSeparator(), BorderLayout.NORTH);
    south.add(buildButtonPanel(), BorderLayout.CENTER);
    getContentPane().add(south, BorderLayout.SOUTH);
    pack();
    setResizable(false);
    setLocationRelativeTo(getOwner());
  }

  private JPanel buildSettingsPanel() {
    String rowSpec = "10px, " + settings.stream()
        .map(s -> "pref")
        .collect(Collectors.joining(", 4px, ")) + ", 10px";
    JPanel panel = new JPanel(new FormLayout("10px, right:default, 8px, fill:default:grow, 10px", rowSpec));
    for (int i = 0; i < settings.size(); i++) {
      int row = 2 + i * 2;
      panel.add(settings.get(i).label, CC.xy(2, row));
      panel.add(settings.get(i).inputComponent, CC.xy(4, row));
    }
    return panel;
  }

  private JPanel buildButtonPanel() {
    JButton ok = new JButton("OK");
    ok.addActionListener(e -> { saveAllPreferences(); dispose(); });
    JButton cancel = new JButton("Abbrechen");
    cancel.addActionListener(e -> dispose());
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    panel.add(ok);
    panel.add(cancel);
    return panel;
  }

  private void saveAllPreferences() {
    settings.forEach(AbstractSetting::savePreference);
  }

}

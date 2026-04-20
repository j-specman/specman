package specman.suggest.github;

import specman.Specman;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;

public class CopilotToggleButton extends JToggleButton implements ActionListener {
  public static final String COPILOT_SELECTED_PREF = "copilot.selected";

  public CopilotToggleButton() {
    setIcon(Specman.readImageIcon("copilot"));
    setSelectedIcon(Specman.readImageIcon("copilot-selected"));
    setToolTipText("GitHub Copilot Completions");
    Preferences prefs = Preferences.userNodeForPackage(Specman.class);
    setSelected(prefs.getBoolean(COPILOT_SELECTED_PREF, false));
    addActionListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Preferences prefs = Preferences.userNodeForPackage(Specman.class);
    prefs.putBoolean(COPILOT_SELECTED_PREF, isSelected());
  }
}

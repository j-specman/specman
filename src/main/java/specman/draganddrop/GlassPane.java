package specman.draganddrop;

import com.formdev.flatlaf.FlatLaf;
import specman.Specman;
import static specman.Specman.editor;

import java.awt.Color;
import java.awt.Cursor;

import javax.swing.*;

public class GlassPane extends JPanel {
	private JPanel insertPosIndicator;
	private final int contentPaneY;
	private int windowsoffset;

	public GlassPane(int contentPaneY, int menueHeight) {
		setOpaque(false);
		setLayout(null);

    // This is baaaaad stuff: If we are using FlatLaf as LookAndFeel, the main menu is incorporated
    // in the application windows's title and the content pane's Y position is directly the offset
    // for displaying the input rectangle. In all other LookAndFeels, this is not the case and we
    // must reduce the offset by the heigt of the main menu. This hack should vanish when redesigning
    // the Drag & Drop functionality.
    this.contentPaneY = (UIManager.getLookAndFeel() instanceof FlatLaf)
      ? contentPaneY
      : contentPaneY - menueHeight;

		createInsertPosIndicator();
		add(insertPosIndicator);
		this.setVisible(true);
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")){
		    windowsoffset = 6;
		}
	  
	}

	private void createInsertPosIndicator() {
		insertPosIndicator = new JPanel();
		insertPosIndicator.setBackground(new Color(85,85,85, 95));
	}

	public void setInputRecBounds(int x, int y, int width, int height) {
		y -= contentPaneY;
		insertPosIndicator.setBounds(x - windowsoffset, y, width, height);
		editor().setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
	}
}

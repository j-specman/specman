package specman;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
/** Non-modal toast notification shown in the bottom-right corner of a parent frame.
 *  Auto-dismisses after {@link #DISPLAY_DURATION_MS} ms; a "Details" button opens the full modal dialog. */
public class ToastNotification {

    private static final int DISPLAY_DURATION_MS = 5000;
    private static final int WIDTH = 400;
    private static final int HEIGHT = 72;
    private static final Color BG = new Color(100, 100, 100);
    private static final float OPACITY = 0.7f;

    /** Toast with just the message, for plain feedback that has nothing more to tell. */
    public static void show(JFrame parent, String brief) {
        show(parent, brief, null);
    }

    /** @param detail text of the modal dialog behind a "Details" button, null for a toast without such button */
    public static void show(JFrame parent, String brief, String detail) {
        JWindow toast = new JWindow(parent);
        if (GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
            .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT)) {
            toast.setOpacity(OPACITY);
        }
        toast.setFocusableWindowState(false); // prevents focus steal → no title-bar flicker on parent
        toast.setLayout(new BorderLayout(0, 0));
        toast.getContentPane().setBackground(BG);

        JLabel label = new JLabel("<html>" + brief + "</html>");
        label.setForeground(Color.WHITE);
        label.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 8));
        toast.add(label, BorderLayout.CENTER);

        if (detail != null) {
            toast.add(createDetailsPanel(toast, parent, detail), BorderLayout.EAST);
        }

        toast.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toast.dispose();
            }
        });

        toast.setSize(WIDTH, HEIGHT);
        positionToast(toast, parent);
        toast.setVisible(true);

        Timer dismissTimer = new Timer(DISPLAY_DURATION_MS, e -> toast.dispose());
        dismissTimer.setRepeats(false);
        dismissTimer.start();
    }

    private static JPanel createDetailsPanel(JWindow toast, JFrame parent, String detail) {
        JButton detailsBtn = new JButton("Details");
        detailsBtn.setFocusPainted(false);
        detailsBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        detailsBtn.addActionListener(e -> {
            toast.dispose();
            JOptionPane.showMessageDialog(parent, detail);
        });
        JPanel btnPanel = new JPanel(new GridBagLayout());
        btnPanel.setBackground(BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 8, 0, 10);
        btnPanel.add(detailsBtn, gbc);
        return btnPanel;
    }

    private static void positionToast(JWindow toast, JFrame parent) {
        Rectangle bounds = parent.getBounds();
        int x = bounds.x + bounds.width - toast.getWidth() - 16;
        int y = bounds.y + bounds.height - toast.getHeight() - 48;
        toast.setLocation(x, y);
    }
}

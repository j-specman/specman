package specman.editarea;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import specman.ChangeInfo;

import javax.swing.*;
import java.awt.*;

public class ImageEditAreaGlassPane extends JPanel {
  static final Color FOCUS_AND_DELETED_GLASS_COLOR = new Color(100, 100, 100, 80);

  ImageEditAreaGlassPane(ChangeInfo changeInfo) {
    setLayout(new FormLayout(
      "fill:8px,pref:grow,fill:8px",
      "fill:8px,fill:pref:grow,fill:3px,fill:pref:grow,fill:8px"));
    setBackground(FOCUS_AND_DELETED_GLASS_COLOR);

    if (changeInfo.isDeleted()) {
      new ImageDeletedMarker();
    }
    else {
      new ImageFocusedMarker(1, 1);
      new ImageFocusedMarker(1, 5);
      new ImageFocusedMarker(3, 1);
      new ImageFocusedMarker(3, 5);
    }
  }

  public void toDeleted() {
    removeAll();
    new ImageDeletedMarker();
    revalidate();
  }

  /** The Markers are shown as small gray rectangles in each corner of a focussed {@link ImageEditArea},
   * making the focussing look familiar to the user. However, the markers have no function yet like
   * resizing or rotating the image. This may be an interesting feature someday. */
  private class ImageFocusedMarker extends JLabel {
    public ImageFocusedMarker(int col, int row) {
      setBackground(ImageEditArea.FOCUS_BORDER_COLOR);
      setOpaque(true);
      ImageEditAreaGlassPane.this.add(this, CC.xy(col, row));
    }
  }

  private class ImageDeletedMarker extends JLabel {
    public ImageDeletedMarker() {
      setBackground(Color.GRAY);
      setOpaque(true);
      ImageEditAreaGlassPane.this.add(this, CC.xywh(1, 3, 3, 1));
    }
  }

}

package specman.i18n;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

import static specman.i18n.Messages.i18n;

/** FileNameExtensionFilter is final, so this filter delegates to one instead of extending it. */
public class IFileNameExtensionFilter extends FileFilter {

  private final FileNameExtensionFilter delegate;

  public IFileNameExtensionFilter(String descriptionKey, String... extensions) {
    delegate = new FileNameExtensionFilter(i18n(descriptionKey), extensions);
  }

  @Override
  public boolean accept(File file) {
    return delegate.accept(file);
  }

  @Override
  public String getDescription() {
    return delegate.getDescription();
  }

}

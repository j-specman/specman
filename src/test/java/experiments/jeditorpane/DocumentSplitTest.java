package experiments.jeditorpane;

import com.jgoodies.forms.factories.CC;
import experiments.NoSuiteTest;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.text.Document;

@NoSuiteTest
public class DocumentSplitTest {

  @Test
  void split() throws Exception {
    JEditorPane field = new JEditorPane();
    field.setContentType("text/html");
    field.setText("eins <b>zwei drei</b> vier");
    Document doc = field.getDocument();
    System.out.println(doc.getLength());
    doc.remove(0, 7);
    System.out.println(doc.getLength());
    System.out.println(field.getText());
  }
}

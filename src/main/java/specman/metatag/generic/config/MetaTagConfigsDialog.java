package specman.metatag.generic.config;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import specman.graphics.SvgIcon;
import specman.undo.UndoableMetaTagConfigAdded;
import specman.undo.UndoableMetaTagConfigEdited;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import static specman.Specman.editor;

/** Lists the meta tag configurations of the diagram and lets the user create and edit them.
 * Every confirmed creation or edit is one undoable step. */
public class MetaTagConfigsDialog extends JDialog {
  private static final int LIST_ICON_SIZE = 16;
  private static final Dimension LIST_SIZE = new Dimension(280, 200);

  private final MetaTagConfigRegistry registry;
  private final DefaultListModel<MetaTagConfig> listModel = new DefaultListModel<>();
  private final JList<MetaTagConfig> list = new JList<>(listModel);
  private final JButton editButton = new JButton("Edit...");

  public MetaTagConfigsDialog(Frame owner, MetaTagConfigRegistry registry) {
    super(owner, "Meta Tags", true);
    this.registry = registry;
    initComponents();
    refreshList(null);
  }

  private void initComponents() {
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setCellRenderer(new ConfigRenderer());
    list.addListSelectionListener(e -> editButton.setEnabled(list.getSelectedValue() != null));
    list.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && list.getSelectedValue() != null) {
          editSelected();
        }
      }
    });
    JScrollPane scrollPane = new JScrollPane(list);
    scrollPane.setPreferredSize(LIST_SIZE);

    JPanel center = new JPanel(new FormLayout("10px, fill:default:grow, 10px", "10px, fill:default:grow, 10px"));
    center.add(scrollPane, CC.xy(2, 2));

    JPanel south = new JPanel(new BorderLayout());
    south.add(new JSeparator(), BorderLayout.NORTH);
    south.add(buildButtonPanel(), BorderLayout.CENTER);

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(center, BorderLayout.CENTER);
    getContentPane().add(south, BorderLayout.SOUTH);
    pack();
    setLocationRelativeTo(getOwner());
  }

  private JPanel buildButtonPanel() {
    JButton newButton = new JButton("New...");
    newButton.addActionListener(e -> createNew());
    editButton.addActionListener(e -> editSelected());
    editButton.setEnabled(false);
    JButton close = new JButton("Close");
    close.addActionListener(e -> dispose());
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    panel.add(newButton);
    panel.add(editButton);
    panel.add(close);
    return panel;
  }

  private void createNew() {
    MetaTagConfig created = new MetaTagConfigEditDialog((Frame) getOwner(), null, registry::contains).showAndGet();
    if (created != null) {
      registry.add(created);
      editor().addEdit(new UndoableMetaTagConfigAdded(registry, created));
      refreshList(created);
    }
  }

  private void editSelected() {
    MetaTagConfig old = list.getSelectedValue();
    MetaTagConfig edited = new MetaTagConfigEditDialog((Frame) getOwner(), old,
      name -> !name.equals(old.getName()) && registry.contains(name)).showAndGet();
    if (edited != null && !edited.equals(old)) {
      registry.replace(old.getName(), edited);
      editor().addEdit(new UndoableMetaTagConfigEdited(registry, old, edited));
      refreshList(edited);
    }
  }

  private void refreshList(MetaTagConfig toSelect) {
    listModel.clear();
    registry.all().forEach(listModel::addElement);
    list.setSelectedValue(toSelect, true);
  }

  private static class ConfigRenderer extends DefaultListCellRenderer {
    private final Map<MetaTagConfig, Icon> icons = new HashMap<>();

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      MetaTagConfig config = (MetaTagConfig) value;
      super.getListCellRendererComponent(list, config.getName(), index, isSelected, cellHasFocus);
      setIcon(config.hasIcon() ? icons.computeIfAbsent(config, c -> new SvgIcon(LIST_ICON_SIZE, c.getIconSvg())) : null);
      return this;
    }
  }
}

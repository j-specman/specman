package specman.metatag.generic.config;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import specman.graphics.SvgIcon;
import specman.undo.UndoableMetaTagConfigAdded;
import specman.undo.UndoableMetaTagConfigEdited;
import specman.undo.UndoableMetaTagConfigRemoved;
import specman.undo.manager.UndoRecording;
import specman.view.AbstractSchrittView;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static specman.Specman.editor;

/** Lists the meta tag configurations of the diagram and lets the user create and edit them.
 * Every confirmed creation or edit is one undoable step. */
public class MetaTagConfigsDialog extends JDialog {
  private static final int LIST_ICON_SIZE = 16;
  private static final Dimension LIST_SIZE = new Dimension(280, 200);

  private final MetaTagConfigRegistry registry;
  private final Set<String> namesInUse;
  private final DefaultListModel<MetaTagConfig> listModel = new DefaultListModel<>();
  private final JList<MetaTagConfig> list = new JList<>(listModel);
  private final JButton editButton = new JButton("Edit...");
  private final JButton deleteButton = new JButton("Delete");

  /** @param namesInUse names of the configurations which tags at steps refer to; these must not be renamed */
  public MetaTagConfigsDialog(Frame owner, MetaTagConfigRegistry registry, Set<String> namesInUse) {
    super(owner, "Meta Tags", true);
    this.registry = registry;
    this.namesInUse = new HashSet<>(namesInUse);
    initComponents();
    refreshList(null);
  }

  private void initComponents() {
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setCellRenderer(new ConfigRenderer());
    list.addListSelectionListener(e -> {
      boolean selected = list.getSelectedValue() != null;
      editButton.setEnabled(selected);
      deleteButton.setEnabled(selected);
    });
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
    deleteButton.addActionListener(e -> deleteSelected());
    deleteButton.setEnabled(false);
    JButton close = new JButton("Close");
    close.addActionListener(e -> dispose());
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    panel.add(newButton);
    panel.add(editButton);
    panel.add(deleteButton);
    panel.add(close);
    return panel;
  }

  private void createNew() {
    MetaTagConfig created = new MetaTagConfigEditDialog((Frame) getOwner(), null, true, registry::contains).showAndGet();
    if (created != null) {
      registry.add(created);
      editor().addEdit(new UndoableMetaTagConfigAdded(registry, created));
      refreshList(created);
    }
  }

  private void editSelected() {
    MetaTagConfig old = list.getSelectedValue();
    MetaTagConfig edited = new MetaTagConfigEditDialog((Frame) getOwner(), old, !namesInUse.contains(old.getName()),
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

  /** The tags at steps which refer to the configuration are removed together with it, in one undoable step. */
  private void deleteSelected() {
    MetaTagConfig config = list.getSelectedValue();
    if (namesInUse.contains(config.getName()) && !confirmDeletionWithTags(config)) {
      return;
    }
    try (UndoRecording ur = editor().composeUndo()) {
      removeTagsOf(config);
      int index = registry.remove(config.getName());
      editor().addEdit(new UndoableMetaTagConfigRemoved(registry, config, index));
    }
    namesInUse.remove(config.getName());
    refreshList(null);
  }

  private boolean confirmDeletionWithTags(MetaTagConfig config) {
    Object[] options = {"Delete", "Cancel"};
    int choice = JOptionPane.showOptionDialog(this,
      "Tags at steps refer to the meta tag '" + config.getName() + "'.\n"
        + "If you delete it, these tags are removed as well.",
      "Delete Meta Tag", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
      null, options, options[1]);
    return choice == 0;
  }

  private void removeTagsOf(MetaTagConfig config) {
    for (AbstractSchrittView step : editor().listAllSteps()) {
      step.removeTagsOfUDBL(config);
    }
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

package classman.editor;

import classman.model.v001.RelationType_V001;

/** One of the relations which a button of the palette can draw. */
class RelationVariant {
  final RelationType_V001 type;
  final String iconPath;

  RelationVariant(RelationType_V001 type, String iconPath) {
    this.type = type;
    this.iconPath = iconPath;
  }

  /** @return the part of the keys of the texts which is special for the variant: palette.label.key, palette.create.key */
  String key() {
    return type.name().toLowerCase();
  }
}

package org.apache.poi.poifs.property;

public final class RootProperty extends DirectoryProperty {
   private static final String NAME = "Root Entry";

   RootProperty() {
      super("Root Entry");
      this.setNodeColor((byte)1);
      this.setPropertyType((byte)5);
      this.setStartBlock(-2);
   }

   RootProperty(int index, byte[] array, int offset) {
      super(index, array, offset);
   }

   public void setSize(int size) {
      int BLOCK_SHIFT = 6;
      int _block_size = 64;
      super.setSize(size * 64);
   }

   public String getName() {
      return "Root Entry";
   }
}

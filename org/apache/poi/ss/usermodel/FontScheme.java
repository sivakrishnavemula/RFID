package org.apache.poi.ss.usermodel;

public enum FontScheme {
   NONE(1),
   MAJOR(2),
   MINOR(3);

   private int value;
   private static FontScheme[] _table = new FontScheme[4];

   FontScheme(int val) {
      this.value = val;
   }

   public int getValue() {
      return this.value;
   }

   public static FontScheme valueOf(int value) {
      return _table[value];
   }

   static {
      for (FontScheme c : values()) {
         _table[c.getValue()] = c;
      }
   }
}

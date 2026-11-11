package org.apache.poi.ss.usermodel;

public enum PrintOrientation {
   DEFAULT(1),
   PORTRAIT(2),
   LANDSCAPE(3);

   private int orientation;
   private static PrintOrientation[] _table = new PrintOrientation[4];

   PrintOrientation(int orientation) {
      this.orientation = orientation;
   }

   public int getValue() {
      return this.orientation;
   }

   public static PrintOrientation valueOf(int value) {
      return _table[value];
   }

   static {
      for (PrintOrientation c : values()) {
         _table[c.getValue()] = c;
      }
   }
}

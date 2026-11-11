package org.apache.poi.ss.usermodel;

public enum PrintCellComments {
   NONE(1),
   AS_DISPLAYED(2),
   AT_END(3);

   private int comments;
   private static PrintCellComments[] _table = new PrintCellComments[4];

   PrintCellComments(int comments) {
      this.comments = comments;
   }

   public int getValue() {
      return this.comments;
   }

   public static PrintCellComments valueOf(int value) {
      return _table[value];
   }

   static {
      for (PrintCellComments c : values()) {
         _table[c.getValue()] = c;
      }
   }
}

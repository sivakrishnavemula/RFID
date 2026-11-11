package org.apache.poi.ss.usermodel;

public enum PageOrder {
   DOWN_THEN_OVER(1),
   OVER_THEN_DOWN(2);

   private final int order;
   private static PageOrder[] _table = new PageOrder[3];

   PageOrder(int order) {
      this.order = order;
   }

   public int getValue() {
      return this.order;
   }

   public static PageOrder valueOf(int value) {
      return _table[value];
   }

   static {
      for (PageOrder c : values()) {
         _table[c.getValue()] = c;
      }
   }
}

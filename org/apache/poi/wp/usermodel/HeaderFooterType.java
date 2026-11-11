package org.apache.poi.wp.usermodel;

public enum HeaderFooterType {
   DEFAULT(2),
   EVEN(1),
   FIRST(3);

   private final int code;

   HeaderFooterType(int i) {
      this.code = i;
   }

   public int toInt() {
      return this.code;
   }

   public static HeaderFooterType forInt(int i) {
      for (HeaderFooterType type : values()) {
         if (type.code == i) {
            return type;
         }
      }

      throw new IllegalArgumentException("Invalid HeaderFooterType code: " + i);
   }
}

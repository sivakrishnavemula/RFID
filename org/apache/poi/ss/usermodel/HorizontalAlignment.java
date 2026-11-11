package org.apache.poi.ss.usermodel;

public enum HorizontalAlignment {
   GENERAL,
   LEFT,
   CENTER,
   RIGHT,
   FILL,
   JUSTIFY,
   CENTER_SELECTION,
   DISTRIBUTED;

   public short getCode() {
      return (short)this.ordinal();
   }

   public static HorizontalAlignment forInt(int code) {
      if (code >= 0 && code < values().length) {
         return values()[code];
      } else {
         throw new IllegalArgumentException("Invalid HorizontalAlignment code: " + code);
      }
   }
}

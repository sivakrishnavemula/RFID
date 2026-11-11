package org.apache.poi.ss.usermodel;

import org.apache.poi.util.Internal;

public enum CellType {
   @Internal(since = "POI 3.15 beta 3")
   _NONE(-1),
   NUMERIC(0),
   STRING(1),
   FORMULA(2),
   BLANK(3),
   BOOLEAN(4),
   ERROR(5);

   /** @deprecated */
   private final int code;

   /** @deprecated */
   CellType(int code) {
      this.code = code;
   }

   /** @deprecated */
   public static CellType forInt(int code) {
      for (CellType type : values()) {
         if (type.code == code) {
            return type;
         }
      }

      throw new IllegalArgumentException("Invalid CellType code: " + code);
   }

   /** @deprecated */
   public int getCode() {
      return this.code;
   }
}

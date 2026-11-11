package org.apache.poi.hssf.util;

public final class RKUtil {
   private RKUtil() {
   }

   public static double decodeNumber(int number) {
      long raw_number = number;
      raw_number >>= 2;
      double rvalue = 0.0;
      if ((number & 2) == 2) {
         rvalue = raw_number;
      } else {
         rvalue = Double.longBitsToDouble(raw_number << 34);
      }

      if ((number & 1) == 1) {
         rvalue /= 100.0;
      }

      return rvalue;
   }
}

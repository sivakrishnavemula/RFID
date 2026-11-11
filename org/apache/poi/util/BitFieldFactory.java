package org.apache.poi.util;

import java.util.HashMap;
import java.util.Map;

public class BitFieldFactory {
   private static Map<Integer, BitField> instances = new HashMap();

   public static BitField getInstance(int mask) {
      BitField f = (BitField)instances.get(mask);
      if (f == null) {
         f = new BitField(mask);
         instances.put(mask, f);
      }

      return f;
   }
}

package org.apache.poi.common.usermodel.fonts;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.Map.Entry;

public enum FontGroup {
   LATIN,
   EAST_ASIAN,
   SYMBOL,
   COMPLEX_SCRIPT;

   private static NavigableMap<Integer, FontGroup.Range> UCS_RANGES = new TreeMap();

   public static List<FontGroup.FontGroupRange> getFontGroupRanges(String runText) {
      List<FontGroup.FontGroupRange> ttrList = new ArrayList();
      if (runText != null && !runText.isEmpty()) {
         FontGroup.FontGroupRange ttrLast = null;
         int rlen = runText.length();
         int i = 0;

         while (i < rlen) {
            int cp = runText.codePointAt(i);
            int charCount = Character.charCount(cp);
            FontGroup tt;
            if (ttrLast != null && " \n\r".indexOf(cp) > -1) {
               tt = ttrLast.fontGroup;
            } else {
               tt = lookup(cp);
            }

            if (ttrLast == null || ttrLast.fontGroup != tt) {
               ttrLast = new FontGroup.FontGroupRange();
               ttrLast.fontGroup = tt;
               ttrList.add(ttrLast);
            }

            FontGroup.FontGroupRange var8 = ttrLast;
            var8.len = var8.len + charCount;
            i += charCount;
         }

         return ttrList;
      } else {
         return ttrList;
      }
   }

   public static FontGroup getFontGroupFirst(String runText) {
      return runText != null && !runText.isEmpty() ? lookup(runText.codePointAt(0)) : LATIN;
   }

   private static FontGroup lookup(int codepoint) {
      Entry<Integer, FontGroup.Range> entry = UCS_RANGES.floorEntry(codepoint);
      FontGroup.Range range = entry != null ? (FontGroup.Range)entry.getValue() : null;
      return range != null && codepoint <= range.upper ? range.fontGroup : EAST_ASIAN;
   }

   static {
      UCS_RANGES.put(0, new FontGroup.Range(127, LATIN));
      UCS_RANGES.put(128, new FontGroup.Range(166, LATIN));
      UCS_RANGES.put(169, new FontGroup.Range(175, LATIN));
      UCS_RANGES.put(178, new FontGroup.Range(179, LATIN));
      UCS_RANGES.put(181, new FontGroup.Range(214, LATIN));
      UCS_RANGES.put(216, new FontGroup.Range(246, LATIN));
      UCS_RANGES.put(248, new FontGroup.Range(1423, LATIN));
      UCS_RANGES.put(1424, new FontGroup.Range(1871, COMPLEX_SCRIPT));
      UCS_RANGES.put(1920, new FontGroup.Range(1983, COMPLEX_SCRIPT));
      UCS_RANGES.put(2304, new FontGroup.Range(4255, COMPLEX_SCRIPT));
      UCS_RANGES.put(4256, new FontGroup.Range(4351, LATIN));
      UCS_RANGES.put(4608, new FontGroup.Range(4991, LATIN));
      UCS_RANGES.put(5024, new FontGroup.Range(6015, LATIN));
      UCS_RANGES.put(7424, new FontGroup.Range(7551, LATIN));
      UCS_RANGES.put(7680, new FontGroup.Range(8191, LATIN));
      UCS_RANGES.put(6016, new FontGroup.Range(6319, COMPLEX_SCRIPT));
      UCS_RANGES.put(8192, new FontGroup.Range(8203, LATIN));
      UCS_RANGES.put(8204, new FontGroup.Range(8207, COMPLEX_SCRIPT));
      UCS_RANGES.put(8208, new FontGroup.Range(8233, LATIN));
      UCS_RANGES.put(8234, new FontGroup.Range(8239, COMPLEX_SCRIPT));
      UCS_RANGES.put(8240, new FontGroup.Range(8262, LATIN));
      UCS_RANGES.put(8266, new FontGroup.Range(9311, LATIN));
      UCS_RANGES.put(9840, new FontGroup.Range(9841, COMPLEX_SCRIPT));
      UCS_RANGES.put(10176, new FontGroup.Range(11263, LATIN));
      UCS_RANGES.put(12441, new FontGroup.Range(12442, EAST_ASIAN));
      UCS_RANGES.put(55349, new FontGroup.Range(55349, LATIN));
      UCS_RANGES.put(61440, new FontGroup.Range(61695, SYMBOL));
      UCS_RANGES.put(64256, new FontGroup.Range(64279, LATIN));
      UCS_RANGES.put(64285, new FontGroup.Range(64335, COMPLEX_SCRIPT));
      UCS_RANGES.put(65104, new FontGroup.Range(65135, LATIN));
   }

   public static class FontGroupRange {
      private int len;
      private FontGroup fontGroup;

      public int getLength() {
         return this.len;
      }

      public FontGroup getFontGroup() {
         return this.fontGroup;
      }
   }

   private static class Range {
      int upper;
      FontGroup fontGroup;

      Range(int upper, FontGroup fontGroup) {
         this.upper = upper;
         this.fontGroup = fontGroup;
      }
   }
}

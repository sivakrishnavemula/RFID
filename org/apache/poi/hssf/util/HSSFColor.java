package org.apache.poi.hssf.util;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.util.Removal;

public class HSSFColor implements Color {
   private static Map<Integer, HSSFColor> indexHash;
   private static Map<HSSFColor.HSSFColorPredefined, HSSFColor> enumList;
   private java.awt.Color color;
   private int index;
   private int index2;

   public HSSFColor() {
      this(64, -1, java.awt.Color.BLACK);
   }

   public HSSFColor(int index, int index2, java.awt.Color color) {
      this.index = index;
      this.index2 = index2;
      this.color = color;
   }

   public static synchronized Map<Integer, HSSFColor> getIndexHash() {
      if (indexHash == null) {
         indexHash = Collections.unmodifiableMap(createColorsByIndexMap());
      }

      return indexHash;
   }

   public static Map<Integer, HSSFColor> getMutableIndexHash() {
      return createColorsByIndexMap();
   }

   private static Map<Integer, HSSFColor> createColorsByIndexMap() {
      Map<HSSFColor.HSSFColorPredefined, HSSFColor> eList = mapEnumToColorClass();
      Map<Integer, HSSFColor> result = new HashMap(eList.size() * 3 / 2);

      for (Entry<HSSFColor.HSSFColorPredefined, HSSFColor> colorRef : eList.entrySet()) {
         Integer index1 = Integer.valueOf(((HSSFColor.HSSFColorPredefined)colorRef.getKey()).getIndex());
         if (!result.containsKey(index1)) {
            result.put(index1, colorRef.getValue());
         }

         Integer index2 = Integer.valueOf(((HSSFColor.HSSFColorPredefined)colorRef.getKey()).getIndex2());
         if (index2 != -1 && !result.containsKey(index2)) {
            result.put(index2, colorRef.getValue());
         }
      }

      return result;
   }

   public static Map<String, HSSFColor> getTripletHash() {
      return createColorsByHexStringMap();
   }

   private static Map<String, HSSFColor> createColorsByHexStringMap() {
      Map<HSSFColor.HSSFColorPredefined, HSSFColor> eList = mapEnumToColorClass();
      Map<String, HSSFColor> result = new HashMap(eList.size());

      for (Entry<HSSFColor.HSSFColorPredefined, HSSFColor> colorRef : eList.entrySet()) {
         String hexString = ((HSSFColor.HSSFColorPredefined)colorRef.getKey()).getHexString();
         if (!result.containsKey(hexString)) {
            result.put(hexString, colorRef.getValue());
         }
      }

      return result;
   }

   @Deprecated
   @Removal(version = "3.18")
   private static synchronized Map<HSSFColor.HSSFColorPredefined, HSSFColor> mapEnumToColorClass() {
      if (enumList == null) {
         enumList = new EnumMap(HSSFColor.HSSFColorPredefined.class);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.BLACK);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.BROWN);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.OLIVE_GREEN);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.DARK_GREEN);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.DARK_TEAL);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.DARK_BLUE);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.INDIGO);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.GREY_80_PERCENT);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.ORANGE);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.DARK_YELLOW);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.GREEN);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.TEAL);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.BLUE);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.BLUE_GREY);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.GREY_50_PERCENT);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.RED);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.LIGHT_ORANGE);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.LIME);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.SEA_GREEN);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.AQUA);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.LIGHT_BLUE);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.VIOLET);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.GREY_40_PERCENT);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.PINK);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.GOLD);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.YELLOW);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.BRIGHT_GREEN);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.TURQUOISE);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.DARK_RED);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.SKY_BLUE);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.PLUM);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.GREY_25_PERCENT);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.ROSE);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.LIGHT_YELLOW);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.LIGHT_GREEN);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.LIGHT_TURQUOISE);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.PALE_BLUE);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.LAVENDER);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.WHITE);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.CORNFLOWER_BLUE);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.LEMON_CHIFFON);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.MAROON);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.ORCHID);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.CORAL);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.ROYAL_BLUE);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.LIGHT_CORNFLOWER_BLUE);
         addHSSFColorPredefined(HSSFColor.HSSFColorPredefined.TAN);
      }

      return enumList;
   }

   private static void addHSSFColorPredefined(HSSFColor.HSSFColorPredefined color) {
      enumList.put(color, color.getColor());
   }

   public short getIndex() {
      return (short)this.index;
   }

   public short getIndex2() {
      return (short)this.index2;
   }

   public short[] getTriplet() {
      return new short[]{(short)this.color.getRed(), (short)this.color.getGreen(), (short)this.color.getBlue()};
   }

   public String getHexString() {
      return (Integer.toHexString(this.color.getRed() * 257)
            + ":"
            + Integer.toHexString(this.color.getGreen() * 257)
            + ":"
            + Integer.toHexString(this.color.getBlue() * 257))
         .toUpperCase(Locale.ROOT);
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (o != null && this.getClass() == o.getClass()) {
         HSSFColor hssfColor = (HSSFColor)o;
         if (this.index != hssfColor.index) {
            return false;
         } else if (this.index2 != hssfColor.index2) {
            return false;
         } else {
            return this.color != null ? this.color.equals(hssfColor.color) : hssfColor.color == null;
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = this.color != null ? this.color.hashCode() : 0;
      result = 31 * result + this.index;
      return 31 * result + this.index2;
   }

   public static HSSFColor toHSSFColor(Color color) {
      if (color != null && !(color instanceof HSSFColor)) {
         throw new IllegalArgumentException("Only HSSFColor objects are supported, but had " + color.getClass());
      } else {
         return (HSSFColor)color;
      }
   }

   public enum HSSFColorPredefined {
      BLACK(8, -1, 0),
      BROWN(60, -1, 10040064),
      OLIVE_GREEN(59, -1, 3355392),
      DARK_GREEN(58, -1, 13056),
      DARK_TEAL(56, -1, 13158),
      DARK_BLUE(18, 32, 128),
      INDIGO(62, -1, 3355545),
      GREY_80_PERCENT(63, -1, 3355443),
      ORANGE(53, -1, 16737792),
      DARK_YELLOW(19, -1, 8421376),
      GREEN(17, -1, 32768),
      TEAL(21, 38, 32896),
      BLUE(12, 39, 255),
      BLUE_GREY(54, -1, 6710937),
      GREY_50_PERCENT(23, -1, 8421504),
      RED(10, -1, 16711680),
      LIGHT_ORANGE(52, -1, 16750848),
      LIME(50, -1, 10079232),
      SEA_GREEN(57, -1, 3381606),
      AQUA(49, -1, 3394764),
      LIGHT_BLUE(48, -1, 3368703),
      VIOLET(20, 36, 8388736),
      GREY_40_PERCENT(55, -1, 9868950),
      PINK(14, 33, 16711935),
      GOLD(51, -1, 16763904),
      YELLOW(13, 34, 16776960),
      BRIGHT_GREEN(11, -1, 65280),
      TURQUOISE(15, 35, 65535),
      DARK_RED(16, 37, 8388608),
      SKY_BLUE(40, -1, 52479),
      PLUM(61, 25, 10040166),
      GREY_25_PERCENT(22, -1, 12632256),
      ROSE(45, -1, 16751052),
      LIGHT_YELLOW(43, -1, 16777113),
      LIGHT_GREEN(42, -1, 13434828),
      LIGHT_TURQUOISE(41, 27, 13434879),
      PALE_BLUE(44, -1, 10079487),
      LAVENDER(46, -1, 13408767),
      WHITE(9, -1, 16777215),
      CORNFLOWER_BLUE(24, -1, 10066431),
      LEMON_CHIFFON(26, -1, 16777164),
      MAROON(25, -1, 8323072),
      ORCHID(28, -1, 6684774),
      CORAL(29, -1, 16744576),
      ROYAL_BLUE(30, -1, 26316),
      LIGHT_CORNFLOWER_BLUE(31, -1, 13421823),
      TAN(47, -1, 16764057),
      AUTOMATIC(64, -1, 0);

      private HSSFColor color;

      HSSFColorPredefined(int index, int index2, int rgb) {
         this.color = new HSSFColor(index, index2, new java.awt.Color(rgb));
      }

      public short getIndex() {
         return this.color.getIndex();
      }

      public short getIndex2() {
         return this.color.getIndex2();
      }

      public short[] getTriplet() {
         return this.color.getTriplet();
      }

      public String getHexString() {
         return this.color.getHexString();
      }

      public HSSFColor getColor() {
         return new HSSFColor(this.getIndex(), this.getIndex2(), this.color.color);
      }
   }
}

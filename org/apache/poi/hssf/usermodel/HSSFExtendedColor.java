package org.apache.poi.hssf.usermodel;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.ExtendedColor;

public class HSSFExtendedColor extends ExtendedColor {
   private org.apache.poi.hssf.record.common.ExtendedColor color;

   public HSSFExtendedColor(org.apache.poi.hssf.record.common.ExtendedColor color) {
      this.color = color;
   }

   protected org.apache.poi.hssf.record.common.ExtendedColor getExtendedColor() {
      return this.color;
   }

   public boolean isAuto() {
      return this.color.getType() == 0;
   }

   public boolean isIndexed() {
      return this.color.getType() == 1;
   }

   public boolean isRGB() {
      return this.color.getType() == 2;
   }

   public boolean isThemed() {
      return this.color.getType() == 3;
   }

   public short getIndex() {
      return (short)this.color.getColorIndex();
   }

   public int getTheme() {
      return this.color.getThemeIndex();
   }

   public byte[] getRGB() {
      byte[] rgb = new byte[3];
      byte[] rgba = this.color.getRGBA();
      if (rgba == null) {
         return null;
      }

      System.arraycopy(rgba, 0, rgb, 0, 3);
      return rgb;
   }

   public byte[] getARGB() {
      byte[] argb = new byte[4];
      byte[] rgba = this.color.getRGBA();
      if (rgba == null) {
         return null;
      }

      System.arraycopy(rgba, 0, argb, 1, 3);
      argb[0] = rgba[3];
      return argb;
   }

   protected byte[] getStoredRBG() {
      return this.getARGB();
   }

   public void setRGB(byte[] rgb) {
      if (rgb.length == 3) {
         byte[] rgba = new byte[4];
         System.arraycopy(rgb, 0, rgba, 0, 3);
         rgba[3] = -1;
      } else {
         byte a = rgb[0];
         rgb[0] = rgb[1];
         rgb[1] = rgb[2];
         rgb[2] = rgb[3];
         rgb[3] = a;
         this.color.setRGBA(rgb);
      }

      this.color.setType(2);
   }

   public double getTint() {
      return this.color.getTint();
   }

   public void setTint(double tint) {
      this.color.setTint(tint);
   }

   protected byte[] getIndexedRGB() {
      if (this.isIndexed() && this.getIndex() > 0) {
         int indexNum = this.getIndex();
         HSSFColor indexed = (HSSFColor)HSSFColor.getIndexHash().get(indexNum);
         if (indexed != null) {
            return new byte[]{(byte)indexed.getTriplet()[0], (byte)indexed.getTriplet()[1], (byte)indexed.getTriplet()[2]};
         }
      }

      return null;
   }
}

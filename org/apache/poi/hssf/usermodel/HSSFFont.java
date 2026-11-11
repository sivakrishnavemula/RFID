package org.apache.poi.hssf.usermodel;

import org.apache.poi.hssf.record.FontRecord;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Font;

public final class HSSFFont implements Font {
   static final short BOLDWEIGHT_NORMAL = 400;
   static final short BOLDWEIGHT_BOLD = 700;
   public static final String FONT_ARIAL = "Arial";
   private FontRecord font;
   private int index;

   protected HSSFFont(int index, FontRecord rec) {
      this.font = rec;
      this.index = index;
   }

   public void setFontName(String name) {
      this.font.setFontName(name);
   }

   public String getFontName() {
      return this.font.getFontName();
   }

   public short getIndex() {
      return (short)this.index;
   }

   public int getIndexAsInt() {
      return this.index;
   }

   public void setFontHeight(short height) {
      this.font.setFontHeight(height);
   }

   public void setFontHeightInPoints(short height) {
      this.font.setFontHeight((short)(height * 20));
   }

   public short getFontHeight() {
      return this.font.getFontHeight();
   }

   public short getFontHeightInPoints() {
      return (short)(this.font.getFontHeight() / 20);
   }

   public void setItalic(boolean italic) {
      this.font.setItalic(italic);
   }

   public boolean getItalic() {
      return this.font.isItalic();
   }

   public void setStrikeout(boolean strikeout) {
      this.font.setStrikeout(strikeout);
   }

   public boolean getStrikeout() {
      return this.font.isStruckout();
   }

   public void setColor(short color) {
      this.font.setColorPaletteIndex(color);
   }

   public short getColor() {
      return this.font.getColorPaletteIndex();
   }

   public HSSFColor getHSSFColor(HSSFWorkbook wb) {
      HSSFPalette pallette = wb.getCustomPalette();
      return pallette.getColor(this.getColor());
   }

   public void setBold(boolean bold) {
      if (bold) {
         this.font.setBoldWeight((short)700);
      } else {
         this.font.setBoldWeight((short)400);
      }
   }

   public boolean getBold() {
      return this.font.getBoldWeight() == 700;
   }

   public void setTypeOffset(short offset) {
      this.font.setSuperSubScript(offset);
   }

   public short getTypeOffset() {
      return this.font.getSuperSubScript();
   }

   public void setUnderline(byte underline) {
      this.font.setUnderline(underline);
   }

   public byte getUnderline() {
      return this.font.getUnderline();
   }

   public int getCharSet() {
      byte charset = this.font.getCharset();
      return charset >= 0 ? charset : charset + 256;
   }

   public void setCharSet(int charset) {
      byte cs = (byte)charset;
      if (charset > 127) {
         cs = (byte)(charset - 256);
      }

      this.setCharSet(cs);
   }

   public void setCharSet(byte charset) {
      this.font.setCharset(charset);
   }

   public String toString() {
      return "org.apache.poi.hssf.usermodel.HSSFFont{" + this.font + "}";
   }

   public int hashCode() {
      int prime = 31;
      int result = 1;
      result = 31 * result + (this.font == null ? 0 : this.font.hashCode());
      return 31 * result + this.index;
   }

   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }

      if (obj == null) {
         return false;
      }

      if (obj instanceof HSSFFont) {
         HSSFFont other = (HSSFFont)obj;
         if (this.font == null) {
            if (other.font != null) {
               return false;
            }
         } else if (!this.font.equals(other.font)) {
            return false;
         }

         return this.index == other.index;
      } else {
         return false;
      }
   }
}

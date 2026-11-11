package org.apache.poi.sl.draw;

import org.apache.poi.common.usermodel.fonts.FontCharset;
import org.apache.poi.common.usermodel.fonts.FontFamily;
import org.apache.poi.common.usermodel.fonts.FontInfo;
import org.apache.poi.common.usermodel.fonts.FontPitch;
import org.apache.poi.util.Internal;

@Internal
class DrawFontInfo implements FontInfo {
   private final String typeface;

   DrawFontInfo(String typeface) {
      this.typeface = typeface;
   }

   public Integer getIndex() {
      return null;
   }

   public void setIndex(int index) {
      throw new UnsupportedOperationException("DrawFontManagers FontInfo can't be changed.");
   }

   public String getTypeface() {
      return this.typeface;
   }

   public void setTypeface(String typeface) {
      throw new UnsupportedOperationException("DrawFontManagers FontInfo can't be changed.");
   }

   public FontCharset getCharset() {
      return FontCharset.ANSI;
   }

   public void setCharset(FontCharset charset) {
      throw new UnsupportedOperationException("DrawFontManagers FontInfo can't be changed.");
   }

   public FontFamily getFamily() {
      return FontFamily.FF_SWISS;
   }

   public void setFamily(FontFamily family) {
      throw new UnsupportedOperationException("DrawFontManagers FontInfo can't be changed.");
   }

   public FontPitch getPitch() {
      return FontPitch.VARIABLE;
   }

   public void setPitch(FontPitch pitch) {
      throw new UnsupportedOperationException("DrawFontManagers FontInfo can't be changed.");
   }
}

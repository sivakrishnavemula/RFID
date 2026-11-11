package org.apache.poi.common.usermodel.fonts;

public interface FontInfo {
   Integer getIndex();

   void setIndex(int var1);

   String getTypeface();

   void setTypeface(String var1);

   FontCharset getCharset();

   void setCharset(FontCharset var1);

   FontFamily getFamily();

   void setFamily(FontFamily var1);

   FontPitch getPitch();

   void setPitch(FontPitch var1);
}

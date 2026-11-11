package org.apache.poi.ss.usermodel;

public interface FontFormatting {
   short SS_NONE = 0;
   short SS_SUPER = 1;
   short SS_SUB = 2;
   byte U_NONE = 0;
   byte U_SINGLE = 1;
   byte U_DOUBLE = 2;
   byte U_SINGLE_ACCOUNTING = 33;
   byte U_DOUBLE_ACCOUNTING = 34;

   short getEscapementType();

   void setEscapementType(short var1);

   short getFontColorIndex();

   void setFontColorIndex(short var1);

   Color getFontColor();

   void setFontColor(Color var1);

   int getFontHeight();

   void setFontHeight(int var1);

   short getUnderlineType();

   void setUnderlineType(short var1);

   boolean isBold();

   boolean isItalic();

   boolean isStruckout();

   void setFontStyle(boolean var1, boolean var2);

   void resetFontStyle();
}

package org.apache.poi.ss.usermodel;

import org.apache.poi.util.Removal;

public interface Font {
   short COLOR_NORMAL = 32767;
   short COLOR_RED = 10;
   short SS_NONE = 0;
   short SS_SUPER = 1;
   short SS_SUB = 2;
   byte U_NONE = 0;
   byte U_SINGLE = 1;
   byte U_DOUBLE = 2;
   byte U_SINGLE_ACCOUNTING = 33;
   byte U_DOUBLE_ACCOUNTING = 34;
   byte ANSI_CHARSET = 0;
   byte DEFAULT_CHARSET = 1;
   byte SYMBOL_CHARSET = 2;

   void setFontName(String var1);

   String getFontName();

   void setFontHeight(short var1);

   void setFontHeightInPoints(short var1);

   short getFontHeight();

   short getFontHeightInPoints();

   void setItalic(boolean var1);

   boolean getItalic();

   void setStrikeout(boolean var1);

   boolean getStrikeout();

   void setColor(short var1);

   short getColor();

   void setTypeOffset(short var1);

   short getTypeOffset();

   void setUnderline(byte var1);

   byte getUnderline();

   int getCharSet();

   void setCharSet(byte var1);

   void setCharSet(int var1);

   /** @deprecated */
   @Removal(version = "4.2")
   short getIndex();

   int getIndexAsInt();

   void setBold(boolean var1);

   boolean getBold();
}

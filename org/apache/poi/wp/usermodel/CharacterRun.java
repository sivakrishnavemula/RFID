package org.apache.poi.wp.usermodel;

public interface CharacterRun {
   boolean isBold();

   void setBold(boolean var1);

   boolean isItalic();

   void setItalic(boolean var1);

   boolean isSmallCaps();

   void setSmallCaps(boolean var1);

   boolean isCapitalized();

   void setCapitalized(boolean var1);

   boolean isStrikeThrough();

   void setStrikeThrough(boolean var1);

   boolean isDoubleStrikeThrough();

   void setDoubleStrikethrough(boolean var1);

   boolean isShadowed();

   void setShadow(boolean var1);

   boolean isEmbossed();

   void setEmbossed(boolean var1);

   boolean isImprinted();

   void setImprinted(boolean var1);

   int getFontSize();

   void setFontSize(int var1);

   int getCharacterSpacing();

   void setCharacterSpacing(int var1);

   int getKerning();

   void setKerning(int var1);

   boolean isHighlighted();

   String getFontName();

   String text();
}

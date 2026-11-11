package org.apache.poi.wp.usermodel;

public interface Paragraph {
   int getIndentFromRight();

   void setIndentFromRight(int var1);

   int getIndentFromLeft();

   void setIndentFromLeft(int var1);

   int getFirstLineIndent();

   void setFirstLineIndent(int var1);

   int getFontAlignment();

   void setFontAlignment(int var1);

   boolean isWordWrapped();

   void setWordWrapped(boolean var1);
}

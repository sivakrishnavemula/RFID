package org.apache.poi.ss.usermodel;

import org.apache.poi.util.Removal;

public interface BorderFormatting {
   BorderStyle getBorderBottom();

   BorderStyle getBorderDiagonal();

   BorderStyle getBorderLeft();

   BorderStyle getBorderRight();

   BorderStyle getBorderTop();

   BorderStyle getBorderVertical();

   BorderStyle getBorderHorizontal();

   @Removal(version = "4.2")
   @Deprecated
   BorderStyle getBorderBottomEnum();

   @Removal(version = "4.2")
   @Deprecated
   BorderStyle getBorderDiagonalEnum();

   @Removal(version = "4.2")
   @Deprecated
   BorderStyle getBorderLeftEnum();

   @Removal(version = "4.2")
   @Deprecated
   BorderStyle getBorderRightEnum();

   @Removal(version = "4.2")
   @Deprecated
   BorderStyle getBorderTopEnum();

   @Removal(version = "4.2")
   @Deprecated
   BorderStyle getBorderVerticalEnum();

   @Removal(version = "4.2")
   @Deprecated
   BorderStyle getBorderHorizontalEnum();

   short getBottomBorderColor();

   Color getBottomBorderColorColor();

   short getDiagonalBorderColor();

   Color getDiagonalBorderColorColor();

   short getLeftBorderColor();

   Color getLeftBorderColorColor();

   short getRightBorderColor();

   Color getRightBorderColorColor();

   short getTopBorderColor();

   Color getTopBorderColorColor();

   short getVerticalBorderColor();

   Color getVerticalBorderColorColor();

   short getHorizontalBorderColor();

   Color getHorizontalBorderColorColor();

   void setBorderBottom(BorderStyle var1);

   void setBorderDiagonal(BorderStyle var1);

   void setBorderLeft(BorderStyle var1);

   void setBorderRight(BorderStyle var1);

   void setBorderTop(BorderStyle var1);

   void setBorderHorizontal(BorderStyle var1);

   void setBorderVertical(BorderStyle var1);

   void setBottomBorderColor(short var1);

   void setBottomBorderColor(Color var1);

   void setDiagonalBorderColor(short var1);

   void setDiagonalBorderColor(Color var1);

   void setLeftBorderColor(short var1);

   void setLeftBorderColor(Color var1);

   void setRightBorderColor(short var1);

   void setRightBorderColor(Color var1);

   void setTopBorderColor(short var1);

   void setTopBorderColor(Color var1);

   void setHorizontalBorderColor(short var1);

   void setHorizontalBorderColor(Color var1);

   void setVerticalBorderColor(short var1);

   void setVerticalBorderColor(Color var1);
}

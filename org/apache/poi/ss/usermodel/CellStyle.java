package org.apache.poi.ss.usermodel;

import org.apache.poi.util.Removal;

public interface CellStyle {
   short getIndex();

   void setDataFormat(short var1);

   short getDataFormat();

   String getDataFormatString();

   void setFont(Font var1);

   /** @deprecated */
   @Removal(version = "4.2")
   short getFontIndex();

   int getFontIndexAsInt();

   void setHidden(boolean var1);

   boolean getHidden();

   void setLocked(boolean var1);

   boolean getLocked();

   void setQuotePrefixed(boolean var1);

   boolean getQuotePrefixed();

   void setAlignment(HorizontalAlignment var1);

   HorizontalAlignment getAlignment();

   @Removal(version = "4.2")
   @Deprecated
   HorizontalAlignment getAlignmentEnum();

   void setWrapText(boolean var1);

   boolean getWrapText();

   void setVerticalAlignment(VerticalAlignment var1);

   VerticalAlignment getVerticalAlignment();

   @Removal(version = "4.2")
   @Deprecated
   VerticalAlignment getVerticalAlignmentEnum();

   void setRotation(short var1);

   short getRotation();

   void setIndention(short var1);

   short getIndention();

   void setBorderLeft(BorderStyle var1);

   BorderStyle getBorderLeft();

   @Removal(version = "4.2")
   @Deprecated
   BorderStyle getBorderLeftEnum();

   void setBorderRight(BorderStyle var1);

   BorderStyle getBorderRight();

   @Removal(version = "4.2")
   @Deprecated
   BorderStyle getBorderRightEnum();

   void setBorderTop(BorderStyle var1);

   BorderStyle getBorderTop();

   @Removal(version = "4.2")
   @Deprecated
   BorderStyle getBorderTopEnum();

   void setBorderBottom(BorderStyle var1);

   BorderStyle getBorderBottom();

   @Removal(version = "4.2")
   @Deprecated
   BorderStyle getBorderBottomEnum();

   void setLeftBorderColor(short var1);

   short getLeftBorderColor();

   void setRightBorderColor(short var1);

   short getRightBorderColor();

   void setTopBorderColor(short var1);

   short getTopBorderColor();

   void setBottomBorderColor(short var1);

   short getBottomBorderColor();

   void setFillPattern(FillPatternType var1);

   FillPatternType getFillPattern();

   @Removal(version = "4.2")
   @Deprecated
   FillPatternType getFillPatternEnum();

   void setFillBackgroundColor(short var1);

   short getFillBackgroundColor();

   Color getFillBackgroundColorColor();

   void setFillForegroundColor(short var1);

   short getFillForegroundColor();

   Color getFillForegroundColorColor();

   void cloneStyleFrom(CellStyle var1);

   void setShrinkToFit(boolean var1);

   boolean getShrinkToFit();
}

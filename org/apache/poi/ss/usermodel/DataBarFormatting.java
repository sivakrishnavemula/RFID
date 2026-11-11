package org.apache.poi.ss.usermodel;

public interface DataBarFormatting {
   boolean isLeftToRight();

   void setLeftToRight(boolean var1);

   boolean isIconOnly();

   void setIconOnly(boolean var1);

   int getWidthMin();

   void setWidthMin(int var1);

   int getWidthMax();

   void setWidthMax(int var1);

   Color getColor();

   void setColor(Color var1);

   ConditionalFormattingThreshold getMinThreshold();

   ConditionalFormattingThreshold getMaxThreshold();
}

package org.apache.poi.ss.usermodel;

public interface DifferentialStyleProvider {
   BorderFormatting getBorderFormatting();

   FontFormatting getFontFormatting();

   ExcelNumberFormat getNumberFormat();

   PatternFormatting getPatternFormatting();

   int getStripeSize();
}

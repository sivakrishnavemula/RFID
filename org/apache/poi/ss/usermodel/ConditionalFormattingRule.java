package org.apache.poi.ss.usermodel;

public interface ConditionalFormattingRule extends DifferentialStyleProvider {
   BorderFormatting createBorderFormatting();

   BorderFormatting getBorderFormatting();

   FontFormatting createFontFormatting();

   FontFormatting getFontFormatting();

   PatternFormatting createPatternFormatting();

   PatternFormatting getPatternFormatting();

   DataBarFormatting getDataBarFormatting();

   IconMultiStateFormatting getMultiStateFormatting();

   ColorScaleFormatting getColorScaleFormatting();

   ExcelNumberFormat getNumberFormat();

   ConditionType getConditionType();

   ConditionFilterType getConditionFilterType();

   ConditionFilterData getFilterConfiguration();

   byte getComparisonOperation();

   String getFormula1();

   String getFormula2();

   String getText();

   int getPriority();

   boolean getStopIfTrue();
}

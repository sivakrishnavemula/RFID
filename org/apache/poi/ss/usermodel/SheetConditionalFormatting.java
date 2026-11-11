package org.apache.poi.ss.usermodel;

import org.apache.poi.ss.usermodel.IconMultiStateFormatting.IconSet;
import org.apache.poi.ss.util.CellRangeAddress;

public interface SheetConditionalFormatting {
   int addConditionalFormatting(CellRangeAddress[] var1, ConditionalFormattingRule var2);

   int addConditionalFormatting(CellRangeAddress[] var1, ConditionalFormattingRule var2, ConditionalFormattingRule var3);

   int addConditionalFormatting(CellRangeAddress[] var1, ConditionalFormattingRule[] var2);

   int addConditionalFormatting(ConditionalFormatting var1);

   ConditionalFormattingRule createConditionalFormattingRule(byte var1, String var2, String var3);

   ConditionalFormattingRule createConditionalFormattingRule(byte var1, String var2);

   ConditionalFormattingRule createConditionalFormattingRule(String var1);

   ConditionalFormattingRule createConditionalFormattingRule(ExtendedColor var1);

   ConditionalFormattingRule createConditionalFormattingRule(IconSet var1);

   ConditionalFormattingRule createConditionalFormattingColorScaleRule();

   ConditionalFormatting getConditionalFormattingAt(int var1);

   int getNumConditionalFormattings();

   void removeConditionalFormatting(int var1);
}

package org.apache.poi.ss.usermodel;

import org.apache.poi.ss.util.CellRangeAddress;

public interface ConditionalFormatting {
   CellRangeAddress[] getFormattingRanges();

   void setFormattingRanges(CellRangeAddress[] var1);

   void setRule(int var1, ConditionalFormattingRule var2);

   void addRule(ConditionalFormattingRule var1);

   ConditionalFormattingRule getRule(int var1);

   int getNumberOfRules();
}

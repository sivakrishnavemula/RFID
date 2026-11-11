package org.apache.poi.ss.usermodel;

import org.apache.poi.ss.util.CellRangeAddressList;

public interface DataValidationHelper {
   DataValidationConstraint createFormulaListConstraint(String var1);

   DataValidationConstraint createExplicitListConstraint(String[] var1);

   DataValidationConstraint createNumericConstraint(int var1, int var2, String var3, String var4);

   DataValidationConstraint createTextLengthConstraint(int var1, String var2, String var3);

   DataValidationConstraint createDecimalConstraint(int var1, String var2, String var3);

   DataValidationConstraint createIntegerConstraint(int var1, String var2, String var3);

   DataValidationConstraint createDateConstraint(int var1, String var2, String var3, String var4);

   DataValidationConstraint createTimeConstraint(int var1, String var2, String var3);

   DataValidationConstraint createCustomConstraint(String var1);

   DataValidation createValidation(DataValidationConstraint var1, CellRangeAddressList var2);
}

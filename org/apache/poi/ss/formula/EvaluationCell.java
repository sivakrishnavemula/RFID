package org.apache.poi.ss.formula;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Removal;

public interface EvaluationCell {
   Object getIdentityKey();

   EvaluationSheet getSheet();

   int getRowIndex();

   int getColumnIndex();

   CellType getCellType();

   @Deprecated
   @Removal(version = "4.2")
   CellType getCellTypeEnum();

   double getNumericCellValue();

   String getStringCellValue();

   boolean getBooleanCellValue();

   int getErrorCellValue();

   CellRangeAddress getArrayFormulaRange();

   boolean isPartOfArrayFormulaGroup();

   CellType getCachedFormulaResultType();

   @Deprecated
   @Removal(version = "4.2")
   CellType getCachedFormulaResultTypeEnum();
}

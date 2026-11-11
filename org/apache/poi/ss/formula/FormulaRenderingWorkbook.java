package org.apache.poi.ss.formula;

import org.apache.poi.ss.formula.EvaluationWorkbook.ExternalSheet;
import org.apache.poi.ss.formula.ptg.NamePtg;
import org.apache.poi.ss.formula.ptg.NameXPtg;

public interface FormulaRenderingWorkbook {
   ExternalSheet getExternalSheet(int var1);

   String getSheetFirstNameByExternSheet(int var1);

   String getSheetLastNameByExternSheet(int var1);

   String resolveNameXText(NameXPtg var1);

   String getNameText(NamePtg var1);
}

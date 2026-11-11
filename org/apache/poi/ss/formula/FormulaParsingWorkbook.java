package org.apache.poi.ss.formula;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Table;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;

public interface FormulaParsingWorkbook {
   EvaluationName getName(String var1, int var2);

   Name createName();

   Table getTable(String var1);

   Ptg getNameXPtg(String var1, SheetIdentifier var2);

   Ptg get3DReferencePtg(CellReference var1, SheetIdentifier var2);

   Ptg get3DReferencePtg(AreaReference var1, SheetIdentifier var2);

   int getExternalSheetIndex(String var1);

   int getExternalSheetIndex(String var1, String var2);

   SpreadsheetVersion getSpreadsheetVersion();
}

package org.apache.poi.ss.formula.eval;

import org.apache.poi.ss.formula.SheetRange;

public interface RefEval extends ValueEval, SheetRange {
   ValueEval getInnerValueEval(int var1);

   int getColumn();

   int getRow();

   int getFirstSheetIndex();

   int getLastSheetIndex();

   int getNumberOfSheets();

   AreaEval offset(int var1, int var2, int var3, int var4);
}

package org.apache.poi.ss.formula;

import org.apache.poi.util.Internal;

@Internal
public interface EvaluationSheet {
   EvaluationCell getCell(int var1, int var2);

   void clearAllCachedResultValues();

   int getLastRowNum();
}

package org.apache.poi.ss.usermodel;

import java.util.Map;
import org.apache.poi.util.Removal;

public interface FormulaEvaluator {
   void clearAllCachedResultValues();

   void notifySetFormula(Cell var1);

   void notifyDeleteCell(Cell var1);

   void notifyUpdateCell(Cell var1);

   void evaluateAll();

   CellValue evaluate(Cell var1);

   CellType evaluateFormulaCell(Cell var1);

   @Deprecated
   @Removal(version = "4.2")
   CellType evaluateFormulaCellEnum(Cell var1);

   Cell evaluateInCell(Cell var1);

   void setupReferencedWorkbooks(Map<String, FormulaEvaluator> var1);

   void setIgnoreMissingWorkbooks(boolean var1);

   void setDebugEvaluationOutputForNextEval(boolean var1);
}

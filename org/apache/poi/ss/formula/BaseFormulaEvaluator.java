package org.apache.poi.ss.formula;

import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.Removal;

public abstract class BaseFormulaEvaluator implements FormulaEvaluator, WorkbookEvaluatorProvider {
   protected final WorkbookEvaluator _bookEvaluator;

   protected BaseFormulaEvaluator(WorkbookEvaluator bookEvaluator) {
      this._bookEvaluator = bookEvaluator;
   }

   public static void setupEnvironment(String[] workbookNames, BaseFormulaEvaluator[] evaluators) {
      WorkbookEvaluator[] wbEvals = new WorkbookEvaluator[evaluators.length];

      for (int i = 0; i < wbEvals.length; i++) {
         wbEvals[i] = evaluators[i]._bookEvaluator;
      }

      CollaboratingWorkbooksEnvironment.setup(workbookNames, wbEvals);
   }

   public void setupReferencedWorkbooks(Map<String, FormulaEvaluator> evaluators) {
      CollaboratingWorkbooksEnvironment.setupFormulaEvaluator(evaluators);
   }

   public WorkbookEvaluator _getWorkbookEvaluator() {
      return this._bookEvaluator;
   }

   public void clearAllCachedResultValues() {
      this._bookEvaluator.clearAllCachedResultValues();
   }

   public CellValue evaluate(Cell cell) {
      if (cell == null) {
         return null;
      }

      switch (cell.getCellType()) {
         case BOOLEAN:
            return CellValue.valueOf(cell.getBooleanCellValue());
         case ERROR:
            return CellValue.getError(cell.getErrorCellValue());
         case FORMULA:
            return this.evaluateFormulaCellValue(cell);
         case NUMERIC:
            return new CellValue(cell.getNumericCellValue());
         case STRING:
            return new CellValue(cell.getRichStringCellValue().getString());
         case BLANK:
            return null;
         default:
            throw new IllegalStateException("Bad cell type (" + cell.getCellType() + ")");
      }
   }

   public Cell evaluateInCell(Cell cell) {
      if (cell == null) {
         return null;
      }

      if (cell.getCellType() == CellType.FORMULA) {
         CellValue cv = this.evaluateFormulaCellValue(cell);
         this.setCellValue(cell, cv);
         setCellType(cell, cv);
         this.setCellValue(cell, cv);
      }

      return cell;
   }

   protected abstract CellValue evaluateFormulaCellValue(Cell var1);

   public CellType evaluateFormulaCell(Cell cell) {
      if (cell != null && cell.getCellType() == CellType.FORMULA) {
         CellValue cv = this.evaluateFormulaCellValue(cell);
         this.setCellValue(cell, cv);
         return cv.getCellType();
      } else {
         return CellType._NONE;
      }
   }

   @Deprecated
   @Removal(version = "4.2")
   public CellType evaluateFormulaCellEnum(Cell cell) {
      return this.evaluateFormulaCell(cell);
   }

   protected static void setCellType(Cell cell, CellValue cv) {
      CellType cellType = cv.getCellType();
      switch (cellType) {
         case BOOLEAN:
         case ERROR:
         case NUMERIC:
         case STRING:
            cell.setCellType(cellType);
            return;
         case FORMULA:
            throw new IllegalArgumentException("This should never happen. Formulas should have already been evaluated.");
         case BLANK:
            throw new IllegalArgumentException("This should never happen. Blanks eventually get translated to zero.");
         default:
            throw new IllegalStateException("Unexpected cell value type (" + cellType + ")");
      }
   }

   protected abstract RichTextString createRichTextString(String var1);

   protected void setCellValue(Cell cell, CellValue cv) {
      CellType cellType = cv.getCellType();
      switch (cellType) {
         case BOOLEAN:
            cell.setCellValue(cv.getBooleanValue());
            break;
         case ERROR:
            cell.setCellErrorValue(cv.getErrorValue());
            break;
         case FORMULA:
         case BLANK:
         default:
            throw new IllegalStateException("Unexpected cell value type (" + cellType + ")");
         case NUMERIC:
            cell.setCellValue(cv.getNumberValue());
            break;
         case STRING:
            cell.setCellValue(this.createRichTextString(cv.getStringValue()));
      }
   }

   public static void evaluateAllFormulaCells(Workbook wb) {
      FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
      evaluateAllFormulaCells(wb, evaluator);
   }

   protected static void evaluateAllFormulaCells(Workbook wb, FormulaEvaluator evaluator) {
      for (int i = 0; i < wb.getNumberOfSheets(); i++) {
         for (Row r : wb.getSheetAt(i)) {
            for (Cell c : r) {
               if (c.getCellType() == CellType.FORMULA) {
                  evaluator.evaluateFormulaCell(c);
               }
            }
         }
      }
   }

   public void setIgnoreMissingWorkbooks(boolean ignore) {
      this._bookEvaluator.setIgnoreMissingWorkbooks(ignore);
   }

   public void setDebugEvaluationOutputForNextEval(boolean value) {
      this._bookEvaluator.setDebugEvaluationOutputForNextEval(value);
   }
}

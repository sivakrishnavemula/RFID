package org.apache.poi.ss.formula;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.poi.ss.formula.FormulaUsedBlankCellSet.BookSheetKey;
import org.apache.poi.ss.formula.eval.ValueEval;

final class FormulaCellCacheEntry extends CellCacheEntry {
   private CellCacheEntry[] _sensitiveInputCells;
   private FormulaUsedBlankCellSet _usedBlankCellGroup;

   public FormulaCellCacheEntry() {
   }

   public boolean isInputSensitive() {
      if (this._sensitiveInputCells != null && this._sensitiveInputCells.length > 0) {
         return true;
      } else {
         return this._usedBlankCellGroup == null ? false : !this._usedBlankCellGroup.isEmpty();
      }
   }

   public void setSensitiveInputCells(CellCacheEntry[] sensitiveInputCells) {
      if (sensitiveInputCells == null) {
         this._sensitiveInputCells = null;
         this.changeConsumingCells(CellCacheEntry.EMPTY_ARRAY);
      } else {
         this._sensitiveInputCells = (CellCacheEntry[])sensitiveInputCells.clone();
         this.changeConsumingCells(this._sensitiveInputCells);
      }
   }

   public void clearFormulaEntry() {
      CellCacheEntry[] usedCells = this._sensitiveInputCells;
      if (usedCells != null) {
         for (int i = usedCells.length - 1; i >= 0; i--) {
            usedCells[i].clearConsumingCell(this);
         }
      }

      this._sensitiveInputCells = null;
      this.clearValue();
   }

   private void changeConsumingCells(CellCacheEntry[] usedCells) {
      CellCacheEntry[] prevUsedCells = this._sensitiveInputCells;
      int nUsed = usedCells.length;

      for (int i = 0; i < nUsed; i++) {
         usedCells[i].addConsumingCell(this);
      }

      if (prevUsedCells != null) {
         int nPrevUsed = prevUsedCells.length;
         if (nPrevUsed >= 1) {
            Set<CellCacheEntry> usedSet;
            if (nUsed < 1) {
               usedSet = Collections.emptySet();
            } else {
               usedSet = new HashSet(nUsed * 3 / 2);

               for (int i = 0; i < nUsed; i++) {
                  usedSet.add(usedCells[i]);
               }
            }

            for (int i = 0; i < nPrevUsed; i++) {
               CellCacheEntry prevUsed = prevUsedCells[i];
               if (!usedSet.contains(prevUsed)) {
                  prevUsed.clearConsumingCell(this);
               }
            }
         }
      }
   }

   public void updateFormulaResult(ValueEval result, CellCacheEntry[] sensitiveInputCells, FormulaUsedBlankCellSet usedBlankAreas) {
      this.updateValue(result);
      this.setSensitiveInputCells(sensitiveInputCells);
      this._usedBlankCellGroup = usedBlankAreas;
   }

   public void notifyUpdatedBlankCell(BookSheetKey bsk, int rowIndex, int columnIndex, IEvaluationListener evaluationListener) {
      if (this._usedBlankCellGroup != null && this._usedBlankCellGroup.containsCell(bsk, rowIndex, columnIndex)) {
         this.clearFormulaEntry();
         this.recurseClearCachedFormulaResults(evaluationListener);
      }
   }
}

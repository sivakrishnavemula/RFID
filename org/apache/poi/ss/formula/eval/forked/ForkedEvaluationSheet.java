package org.apache.poi.ss.formula.eval.forked;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.formula.EvaluationSheet;
import org.apache.poi.ss.formula.EvaluationWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.Internal;

@Internal
final class ForkedEvaluationSheet implements EvaluationSheet {
   private final EvaluationSheet _masterSheet;
   private final Map<ForkedEvaluationSheet.RowColKey, ForkedEvaluationCell> _sharedCellsByRowCol;

   public ForkedEvaluationSheet(EvaluationSheet masterSheet) {
      this._masterSheet = masterSheet;
      this._sharedCellsByRowCol = new HashMap();
   }

   public int getLastRowNum() {
      return this._masterSheet.getLastRowNum();
   }

   public EvaluationCell getCell(int rowIndex, int columnIndex) {
      ForkedEvaluationSheet.RowColKey key = new ForkedEvaluationSheet.RowColKey(rowIndex, columnIndex);
      ForkedEvaluationCell result = (ForkedEvaluationCell)this._sharedCellsByRowCol.get(key);
      return (EvaluationCell)(result == null ? this._masterSheet.getCell(rowIndex, columnIndex) : result);
   }

   public ForkedEvaluationCell getOrCreateUpdatableCell(int rowIndex, int columnIndex) {
      ForkedEvaluationSheet.RowColKey key = new ForkedEvaluationSheet.RowColKey(rowIndex, columnIndex);
      ForkedEvaluationCell result = (ForkedEvaluationCell)this._sharedCellsByRowCol.get(key);
      if (result == null) {
         EvaluationCell mcell = this._masterSheet.getCell(rowIndex, columnIndex);
         if (mcell == null) {
            CellReference cr = new CellReference(rowIndex, columnIndex);
            throw new UnsupportedOperationException("Underlying cell '" + cr.formatAsString() + "' is missing in master sheet.");
         }

         result = new ForkedEvaluationCell(this, mcell);
         this._sharedCellsByRowCol.put(key, result);
      }

      return result;
   }

   public void copyUpdatedCells(Sheet sheet) {
      ForkedEvaluationSheet.RowColKey[] keys = new ForkedEvaluationSheet.RowColKey[this._sharedCellsByRowCol.size()];
      this._sharedCellsByRowCol.keySet().toArray(keys);
      Arrays.sort(keys);

      for (int i = 0; i < keys.length; i++) {
         ForkedEvaluationSheet.RowColKey key = keys[i];
         Row row = sheet.getRow(key.getRowIndex());
         if (row == null) {
            row = sheet.createRow(key.getRowIndex());
         }

         Cell destCell = row.getCell(key.getColumnIndex());
         if (destCell == null) {
            destCell = row.createCell(key.getColumnIndex());
         }

         ForkedEvaluationCell srcCell = (ForkedEvaluationCell)this._sharedCellsByRowCol.get(key);
         srcCell.copyValue(destCell);
      }
   }

   public int getSheetIndex(EvaluationWorkbook mewb) {
      return mewb.getSheetIndex(this._masterSheet);
   }

   public void clearAllCachedResultValues() {
      this._masterSheet.clearAllCachedResultValues();
   }

   private static final class RowColKey implements Comparable<ForkedEvaluationSheet.RowColKey> {
      private final int _rowIndex;
      private final int _columnIndex;

      public RowColKey(int rowIndex, int columnIndex) {
         this._rowIndex = rowIndex;
         this._columnIndex = columnIndex;
      }

      public boolean equals(Object obj) {
         if (!(obj instanceof ForkedEvaluationSheet.RowColKey)) {
            return false;
         }

         ForkedEvaluationSheet.RowColKey other = (ForkedEvaluationSheet.RowColKey)obj;
         return this._rowIndex == other._rowIndex && this._columnIndex == other._columnIndex;
      }

      public int hashCode() {
         return this._rowIndex ^ this._columnIndex;
      }

      public int compareTo(ForkedEvaluationSheet.RowColKey o) {
         int cmp = this._rowIndex - o._rowIndex;
         return cmp != 0 ? cmp : this._columnIndex - o._columnIndex;
      }

      public int getRowIndex() {
         return this._rowIndex;
      }

      public int getColumnIndex() {
         return this._columnIndex;
      }
   }
}

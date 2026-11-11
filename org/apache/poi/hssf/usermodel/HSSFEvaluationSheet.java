package org.apache.poi.hssf.usermodel;

import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.formula.EvaluationSheet;
import org.apache.poi.util.Internal;

@Internal
final class HSSFEvaluationSheet implements EvaluationSheet {
   private final HSSFSheet _hs;
   private int _lastDefinedRow = -1;

   public HSSFEvaluationSheet(HSSFSheet hs) {
      this._hs = hs;
      this._lastDefinedRow = this._hs.getLastRowNum();
   }

   public HSSFSheet getHSSFSheet() {
      return this._hs;
   }

   public int getLastRowNum() {
      return this._lastDefinedRow;
   }

   public EvaluationCell getCell(int rowIndex, int columnIndex) {
      HSSFRow row = this._hs.getRow(rowIndex);
      if (row == null) {
         return null;
      }

      HSSFCell cell = row.getCell(columnIndex);
      return cell == null ? null : new HSSFEvaluationCell(cell, this);
   }

   public void clearAllCachedResultValues() {
      this._lastDefinedRow = this._hs.getLastRowNum();
   }
}

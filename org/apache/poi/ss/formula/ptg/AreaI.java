package org.apache.poi.ss.formula.ptg;

public interface AreaI {
   int getFirstRow();

   int getLastRow();

   int getFirstColumn();

   int getLastColumn();

   class OffsetArea implements AreaI {
      private final int _firstColumn;
      private final int _firstRow;
      private final int _lastColumn;
      private final int _lastRow;

      public OffsetArea(int baseRow, int baseColumn, int relFirstRowIx, int relLastRowIx, int relFirstColIx, int relLastColIx) {
         this._firstRow = baseRow + Math.min(relFirstRowIx, relLastRowIx);
         this._lastRow = baseRow + Math.max(relFirstRowIx, relLastRowIx);
         this._firstColumn = baseColumn + Math.min(relFirstColIx, relLastColIx);
         this._lastColumn = baseColumn + Math.max(relFirstColIx, relLastColIx);
      }

      @Override
      public int getFirstColumn() {
         return this._firstColumn;
      }

      @Override
      public int getFirstRow() {
         return this._firstRow;
      }

      @Override
      public int getLastColumn() {
         return this._lastColumn;
      }

      @Override
      public int getLastRow() {
         return this._lastRow;
      }
   }
}

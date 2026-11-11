package com.microsoft.sqlserver.jdbc;

import java.util.logging.Level;

final class ScrollWindow {
   private TDSReaderMark[] rowMark;
   private boolean[] updatedRow;
   private boolean[] deletedRow;
   private RowType[] rowType;
   private int size = 0;
   private int maxRows = 0;
   private int currentRow;

   final int getMaxRows() {
      return this.maxRows;
   }

   final int getRow() {
      return this.currentRow;
   }

   ScrollWindow(int var1) {
      this.setSize(var1);
      this.reset();
   }

   private final void setSize(int var1) {
      assert this.size != var1;
      this.size = var1;
      this.maxRows = var1;
      this.rowMark = new TDSReaderMark[var1];
      this.updatedRow = new boolean[var1];
      this.deletedRow = new boolean[var1];
      this.rowType = new RowType[var1];

      for (int var2 = 0; var2 < var1; var2++) {
         this.rowType[var2] = RowType.UNKNOWN;
      }
   }

   final void clear() {
      for (int var1 = 0; var1 < this.rowMark.length; var1++) {
         this.rowMark[var1] = null;
         this.updatedRow[var1] = false;
         this.deletedRow[var1] = false;
         this.rowType[var1] = RowType.UNKNOWN;
      }

      assert this.size > 0;
      this.maxRows = this.size;
      this.reset();
   }

   final void reset() {
      this.currentRow = 0;
   }

   final void resize(int var1) {
      assert var1 > 0;
      if (var1 != this.size) {
         this.setSize(var1);
      }
   }

   final String logCursorState() {
      return " currentRow:" + this.currentRow + " maxRows:" + this.maxRows;
   }

   final boolean next(SQLServerResultSet var1) throws SQLServerException {
      if (SQLServerResultSet.logger.isLoggable(Level.FINER)) {
         SQLServerResultSet.logger.finer(var1.toString() + this.logCursorState());
      }

      assert 0 <= this.currentRow && this.currentRow <= this.maxRows + 1;
      if (this.maxRows + 1 == this.currentRow) {
         return false;
      }

      if (this.currentRow >= 1) {
         this.updatedRow[this.currentRow - 1] = var1.getUpdatedCurrentRow();
         this.deletedRow[this.currentRow - 1] = var1.getDeletedCurrentRow();
         this.rowType[this.currentRow - 1] = var1.getCurrentRowType();
      }

      this.currentRow++;
      if (this.maxRows + 1 == this.currentRow) {
         var1.fetchBufferNext();
         return false;
      }

      if (null != this.rowMark[this.currentRow - 1]) {
         var1.fetchBufferReset(this.rowMark[this.currentRow - 1]);
         var1.setCurrentRowType(this.rowType[this.currentRow - 1]);
         var1.setUpdatedCurrentRow(this.updatedRow[this.currentRow - 1]);
         var1.setDeletedCurrentRow(this.deletedRow[this.currentRow - 1]);
         return true;
      }

      if (var1.fetchBufferNext()) {
         this.rowMark[this.currentRow - 1] = var1.fetchBufferMark();
         this.rowType[this.currentRow - 1] = var1.getCurrentRowType();
         if (SQLServerResultSet.logger.isLoggable(Level.FINEST)) {
            SQLServerResultSet.logger
               .finest(
                  var1.toString()
                     + " Set mark "
                     + this.rowMark[this.currentRow - 1]
                     + " for row "
                     + this.currentRow
                     + " of type "
                     + this.rowType[this.currentRow - 1]
               );
         }

         return true;
      } else {
         this.maxRows = this.currentRow - 1;
         return false;
      }
   }

   final void previous(SQLServerResultSet var1) throws SQLServerException {
      if (SQLServerResultSet.logger.isLoggable(Level.FINER)) {
         SQLServerResultSet.logger.finer(var1.toString() + this.logCursorState());
      }

      assert 0 <= this.currentRow && this.currentRow <= this.maxRows + 1;
      if (0 != this.currentRow) {
         if (this.currentRow <= this.maxRows) {
            assert this.currentRow >= 1;
            this.updatedRow[this.currentRow - 1] = var1.getUpdatedCurrentRow();
            this.deletedRow[this.currentRow - 1] = var1.getDeletedCurrentRow();
            this.rowType[this.currentRow - 1] = var1.getCurrentRowType();
         }

         this.currentRow--;
         if (0 != this.currentRow) {
            assert null != this.rowMark[this.currentRow - 1];
            var1.fetchBufferReset(this.rowMark[this.currentRow - 1]);
            var1.setCurrentRowType(this.rowType[this.currentRow - 1]);
            var1.setUpdatedCurrentRow(this.updatedRow[this.currentRow - 1]);
            var1.setDeletedCurrentRow(this.deletedRow[this.currentRow - 1]);
         }
      }
   }
}

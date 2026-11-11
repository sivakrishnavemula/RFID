package org.apache.poi.ss.util.cellwalk;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

public class CellWalk {
   private Sheet sheet;
   private CellRangeAddress range;
   private boolean traverseEmptyCells;

   public CellWalk(Sheet sheet, CellRangeAddress range) {
      this.sheet = sheet;
      this.range = range;
      this.traverseEmptyCells = false;
   }

   public boolean isTraverseEmptyCells() {
      return this.traverseEmptyCells;
   }

   public void setTraverseEmptyCells(boolean traverseEmptyCells) {
      this.traverseEmptyCells = traverseEmptyCells;
   }

   public void traverse(CellHandler handler) {
      int firstRow = this.range.getFirstRow();
      int lastRow = this.range.getLastRow();
      int firstColumn = this.range.getFirstColumn();
      int lastColumn = this.range.getLastColumn();
      int width = lastColumn - firstColumn + 1;
      CellWalk.SimpleCellWalkContext ctx = new CellWalk.SimpleCellWalkContext();
      Row currentRow = null;
      Cell currentCell = null;

      for (ctx.rowNumber = firstRow; ctx.rowNumber <= lastRow; ctx.rowNumber++) {
         currentRow = this.sheet.getRow(ctx.rowNumber);
         if (currentRow != null) {
            for (ctx.colNumber = firstColumn; ctx.colNumber <= lastColumn; ctx.colNumber++) {
               currentCell = currentRow.getCell(ctx.colNumber);
               if (currentCell != null && (!this.isEmpty(currentCell) || this.traverseEmptyCells)) {
                  ctx.ordinalNumber = (ctx.rowNumber - firstRow) * width + ctx.colNumber - firstColumn + 1;
                  handler.onCell(currentCell, ctx);
               }
            }
         }
      }
   }

   private boolean isEmpty(Cell cell) {
      return cell.getCellType() == CellType.BLANK;
   }

   private static class SimpleCellWalkContext implements CellWalkContext {
      public long ordinalNumber;
      public int rowNumber;
      public int colNumber;

      private SimpleCellWalkContext() {
      }

      public long getOrdinalNumber() {
         return this.ordinalNumber;
      }

      public int getRowNumber() {
         return this.rowNumber;
      }

      public int getColumnNumber() {
         return this.colNumber;
      }
   }
}

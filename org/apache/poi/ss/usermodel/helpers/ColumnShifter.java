package org.apache.poi.ss.usermodel.helpers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

public abstract class ColumnShifter extends BaseRowColShifter {
   protected final Sheet sheet;

   public ColumnShifter(Sheet sh) {
      this.sheet = sh;
   }

   public List<CellRangeAddress> shiftMergedRegions(int startColumn, int endColumn, int n) {
      List<CellRangeAddress> shiftedRegions = new ArrayList();
      Set<Integer> removedIndices = new HashSet();
      int size = this.sheet.getNumMergedRegions();

      for (int i = 0; i < size; i++) {
         CellRangeAddress merged = this.sheet.getMergedRegion(i);
         if (this.removalNeeded(merged, startColumn, endColumn, n)) {
            removedIndices.add(i);
         } else {
            boolean inStart = merged.getFirstColumn() >= startColumn || merged.getLastColumn() >= startColumn;
            boolean inEnd = merged.getFirstColumn() <= endColumn || merged.getLastColumn() <= endColumn;
            if (inStart && inEnd && !merged.containsColumn(startColumn - 1) && !merged.containsColumn(endColumn + 1)) {
               merged.setFirstColumn(merged.getFirstColumn() + n);
               merged.setLastColumn(merged.getLastColumn() + n);
               shiftedRegions.add(merged);
               removedIndices.add(i);
            }
         }
      }

      if (!removedIndices.isEmpty()) {
         this.sheet.removeMergedRegions(removedIndices);
      }

      for (CellRangeAddress region : shiftedRegions) {
         this.sheet.addMergedRegion(region);
      }

      return shiftedRegions;
   }

   private boolean removalNeeded(CellRangeAddress merged, int startColumn, int endColumn, int n) {
      int movedColumns = endColumn - startColumn + 1;
      CellRangeAddress overwrite;
      if (n > 0) {
         int firstCol = Math.max(endColumn + 1, endColumn + n - movedColumns);
         int lastCol = endColumn + n;
         overwrite = new CellRangeAddress(0, 0, firstCol, lastCol);
      } else {
         int firstCol = startColumn + n;
         int lastCol = Math.min(startColumn - 1, startColumn + n + movedColumns);
         overwrite = new CellRangeAddress(0, 0, firstCol, lastCol);
      }

      return merged.intersects(overwrite);
   }

   public void shiftColumns(int firstShiftColumnIndex, int lastShiftColumnIndex, int step) {
      if (step > 0) {
         for (Row row : this.sheet) {
            if (row != null) {
               row.shiftCellsRight(firstShiftColumnIndex, lastShiftColumnIndex, step);
            }
         }
      } else if (step < 0) {
         for (Row row : this.sheet) {
            if (row != null) {
               row.shiftCellsLeft(firstShiftColumnIndex, lastShiftColumnIndex, -step);
            }
         }
      }
   }
}

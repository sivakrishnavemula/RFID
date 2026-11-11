package org.apache.poi.ss.usermodel.helpers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

public abstract class RowShifter extends BaseRowColShifter {
   protected final Sheet sheet;

   public RowShifter(Sheet sh) {
      this.sheet = sh;
   }

   public List<CellRangeAddress> shiftMergedRegions(int startRow, int endRow, int n) {
      List<CellRangeAddress> shiftedRegions = new ArrayList();
      Set<Integer> removedIndices = new HashSet();
      int size = this.sheet.getNumMergedRegions();

      for (int i = 0; i < size; i++) {
         CellRangeAddress merged = this.sheet.getMergedRegion(i);
         if (this.removalNeeded(merged, startRow, endRow, n)) {
            removedIndices.add(i);
         } else {
            boolean inStart = merged.getFirstRow() >= startRow || merged.getLastRow() >= startRow;
            boolean inEnd = merged.getFirstRow() <= endRow || merged.getLastRow() <= endRow;
            if (inStart && inEnd && !merged.containsRow(startRow - 1) && !merged.containsRow(endRow + 1)) {
               merged.setFirstRow(merged.getFirstRow() + n);
               merged.setLastRow(merged.getLastRow() + n);
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

   private boolean removalNeeded(CellRangeAddress merged, int startRow, int endRow, int n) {
      int movedRows = endRow - startRow + 1;
      CellRangeAddress overwrite;
      if (n > 0) {
         int firstRow = Math.max(endRow + 1, endRow + n - movedRows);
         int lastRow = endRow + n;
         overwrite = new CellRangeAddress(firstRow, lastRow, 0, 0);
      } else {
         int firstRow = startRow + n;
         int lastRow = Math.min(startRow - 1, startRow + n + movedRows);
         overwrite = new CellRangeAddress(firstRow, lastRow, 0, 0);
      }

      return merged.intersects(overwrite);
   }
}

package org.apache.poi.ss.usermodel;

import java.util.regex.Pattern;
import org.apache.poi.ss.util.CellReference;

public interface Table {
   Pattern isStructuredReference = Pattern.compile("[a-zA-Z_\\\\][a-zA-Z0-9._]*\\[.*\\]");

   int getStartColIndex();

   int getStartRowIndex();

   int getEndColIndex();

   int getEndRowIndex();

   String getName();

   String getStyleName();

   int findColumnIndex(String var1);

   String getSheetName();

   boolean isHasTotalsRow();

   int getTotalsRowCount();

   int getHeaderRowCount();

   TableStyleInfo getStyle();

   default boolean contains(Cell cell) {
      return cell == null ? false : this.contains(new CellReference(cell.getSheet().getSheetName(), cell.getRowIndex(), cell.getColumnIndex(), true, true));
   }

   boolean contains(CellReference var1);
}

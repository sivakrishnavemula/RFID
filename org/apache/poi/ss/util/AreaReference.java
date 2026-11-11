package org.apache.poi.ss.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.poi.ss.SpreadsheetVersion;

public class AreaReference {
   private static final char SHEET_NAME_DELIMITER = '!';
   private static final char CELL_DELIMITER = ':';
   private static final char SPECIAL_NAME_DELIMITER = '\'';
   private static final SpreadsheetVersion DEFAULT_SPREADSHEET_VERSION = SpreadsheetVersion.EXCEL97;
   private final CellReference _firstCell;
   private final CellReference _lastCell;
   private final boolean _isSingleCell;
   private final SpreadsheetVersion _version;

   public AreaReference(String reference, SpreadsheetVersion version) {
      this._version = null != version ? version : DEFAULT_SPREADSHEET_VERSION;
      if (!isContiguous(reference)) {
         throw new IllegalArgumentException(
            "References passed to the AreaReference must be contiguous, use generateContiguous(ref) if you have non-contiguous references"
         );
      }

      String[] parts = separateAreaRefs(reference);
      String part0 = parts[0];
      if (parts.length == 1) {
         this._firstCell = new CellReference(part0);
         this._lastCell = this._firstCell;
         this._isSingleCell = true;
      } else {
         if (parts.length != 2) {
            throw new IllegalArgumentException("Bad area ref '" + reference + "'");
         }

         String part1 = parts[1];
         if (isPlainColumn(part0)) {
            if (!isPlainColumn(part1)) {
               throw new RuntimeException("Bad area ref '" + reference + "'");
            }

            boolean firstIsAbs = CellReference.isPartAbsolute(part0);
            boolean lastIsAbs = CellReference.isPartAbsolute(part1);
            int col0 = CellReference.convertColStringToIndex(part0);
            int col1 = CellReference.convertColStringToIndex(part1);
            this._firstCell = new CellReference(0, col0, true, firstIsAbs);
            this._lastCell = new CellReference(65535, col1, true, lastIsAbs);
            this._isSingleCell = false;
         } else {
            this._firstCell = new CellReference(part0);
            this._lastCell = new CellReference(part1);
            this._isSingleCell = part0.equals(part1);
         }
      }
   }

   private static boolean isPlainColumn(String refPart) {
      for (int i = refPart.length() - 1; i >= 0; i--) {
         int ch = refPart.charAt(i);
         if ((ch != 36 || i != 0) && (ch < 65 || ch > 90)) {
            return false;
         }
      }

      return true;
   }

   public AreaReference(CellReference topLeft, CellReference botRight, SpreadsheetVersion version) {
      this._version = null != version ? version : DEFAULT_SPREADSHEET_VERSION;
      boolean swapRows = topLeft.getRow() > botRight.getRow();
      boolean swapCols = topLeft.getCol() > botRight.getCol();
      if (!swapRows && !swapCols) {
         this._firstCell = topLeft;
         this._lastCell = botRight;
      } else {
         int firstRow;
         int lastRow;
         boolean firstRowAbs;
         boolean lastRowAbs;
         if (swapRows) {
            firstRow = botRight.getRow();
            firstRowAbs = botRight.isRowAbsolute();
            lastRow = topLeft.getRow();
            lastRowAbs = topLeft.isRowAbsolute();
         } else {
            firstRow = topLeft.getRow();
            firstRowAbs = topLeft.isRowAbsolute();
            lastRow = botRight.getRow();
            lastRowAbs = botRight.isRowAbsolute();
         }

         int firstColumn;
         int lastColumn;
         boolean firstColAbs;
         boolean lastColAbs;
         if (swapCols) {
            firstColumn = botRight.getCol();
            firstColAbs = botRight.isColAbsolute();
            lastColumn = topLeft.getCol();
            lastColAbs = topLeft.isColAbsolute();
         } else {
            firstColumn = topLeft.getCol();
            firstColAbs = topLeft.isColAbsolute();
            lastColumn = botRight.getCol();
            lastColAbs = botRight.isColAbsolute();
         }

         this._firstCell = new CellReference(firstRow, firstColumn, firstRowAbs, firstColAbs);
         this._lastCell = new CellReference(lastRow, lastColumn, lastRowAbs, lastColAbs);
      }

      this._isSingleCell = false;
   }

   public static boolean isContiguous(String reference) {
      int sheetRefEnd = reference.indexOf(33);
      if (sheetRefEnd != -1) {
         reference = reference.substring(sheetRefEnd);
      }

      return !reference.contains(",");
   }

   public static AreaReference getWholeRow(SpreadsheetVersion version, String start, String end) {
      if (null == version) {
         version = DEFAULT_SPREADSHEET_VERSION;
      }

      return new AreaReference("$A" + start + ":$" + version.getLastColumnName() + end, version);
   }

   public static AreaReference getWholeColumn(SpreadsheetVersion version, String start, String end) {
      if (null == version) {
         version = DEFAULT_SPREADSHEET_VERSION;
      }

      return new AreaReference(start + "$1:" + end + "$" + version.getMaxRows(), version);
   }

   public static boolean isWholeColumnReference(SpreadsheetVersion version, CellReference topLeft, CellReference botRight) {
      if (null == version) {
         version = DEFAULT_SPREADSHEET_VERSION;
      }

      return topLeft.getRow() == 0 && topLeft.isRowAbsolute() && botRight.getRow() == version.getLastRowIndex() && botRight.isRowAbsolute();
   }

   public boolean isWholeColumnReference() {
      return isWholeColumnReference(this._version, this._firstCell, this._lastCell);
   }

   public static AreaReference[] generateContiguous(SpreadsheetVersion version, String reference) {
      if (null == version) {
         version = DEFAULT_SPREADSHEET_VERSION;
      }

      List<AreaReference> refs = new ArrayList();
      StringTokenizer st = new StringTokenizer(reference, ",");

      while (st.hasMoreTokens()) {
         refs.add(new AreaReference(st.nextToken(), version));
      }

      return (AreaReference[])refs.toArray(new AreaReference[refs.size()]);
   }

   public boolean isSingleCell() {
      return this._isSingleCell;
   }

   public CellReference getFirstCell() {
      return this._firstCell;
   }

   public CellReference getLastCell() {
      return this._lastCell;
   }

   public CellReference[] getAllReferencedCells() {
      if (this._isSingleCell) {
         return new CellReference[]{this._firstCell};
      }

      int minRow = Math.min(this._firstCell.getRow(), this._lastCell.getRow());
      int maxRow = Math.max(this._firstCell.getRow(), this._lastCell.getRow());
      int minCol = Math.min(this._firstCell.getCol(), this._lastCell.getCol());
      int maxCol = Math.max(this._firstCell.getCol(), this._lastCell.getCol());
      String sheetName = this._firstCell.getSheetName();
      List<CellReference> refs = new ArrayList();

      for (int row = minRow; row <= maxRow; row++) {
         for (int col = minCol; col <= maxCol; col++) {
            CellReference ref = new CellReference(sheetName, row, col, this._firstCell.isRowAbsolute(), this._firstCell.isColAbsolute());
            refs.add(ref);
         }
      }

      return (CellReference[])refs.toArray(new CellReference[refs.size()]);
   }

   public String formatAsString() {
      if (this.isWholeColumnReference()) {
         return CellReference.convertNumToColString(this._firstCell.getCol()) + ":" + CellReference.convertNumToColString(this._lastCell.getCol());
      }

      StringBuilder sb = new StringBuilder(32);
      sb.append(this._firstCell.formatAsString());
      if (!this._isSingleCell) {
         sb.append(':');
         if (this._lastCell.getSheetName() == null) {
            sb.append(this._lastCell.formatAsString());
         } else {
            this._lastCell.appendCellReference(sb);
         }
      }

      return sb.toString();
   }

   public String toString() {
      StringBuilder sb = new StringBuilder(64);
      sb.append(this.getClass().getName()).append(" [");

      try {
         sb.append(this.formatAsString());
      } catch (Exception e) {
         sb.append(e.toString());
      }

      sb.append(']');
      return sb.toString();
   }

   private static String[] separateAreaRefs(String reference) {
      int len = reference.length();
      int delimiterPos = -1;
      boolean insideDelimitedName = false;

      for (int i = 0; i < len; i++) {
         switch (reference.charAt(i)) {
            case '\'':
               if (!insideDelimitedName) {
                  insideDelimitedName = true;
               } else {
                  if (i >= len - 1) {
                     throw new IllegalArgumentException("Area reference '" + reference + "' ends with special name delimiter '" + '\'' + "'");
                  }

                  if (reference.charAt(i + 1) == '\'') {
                     i++;
                  } else {
                     insideDelimitedName = false;
                  }
               }
               break;
            case ':':
               if (!insideDelimitedName) {
                  if (delimiterPos >= 0) {
                     throw new IllegalArgumentException("More than one cell delimiter ':' appears in area reference '" + reference + "'");
                  }

                  delimiterPos = i;
               }
         }
      }

      if (delimiterPos < 0) {
         return new String[]{reference};
      }

      String partA = reference.substring(0, delimiterPos);
      String partB = reference.substring(delimiterPos + 1);
      if (partB.indexOf(33) >= 0) {
         throw new RuntimeException("Unexpected ! in second cell reference of '" + reference + "'");
      }

      int plingPos = partA.lastIndexOf(33);
      if (plingPos < 0) {
         return new String[]{partA, partB};
      }

      String sheetName = partA.substring(0, plingPos + 1);
      return new String[]{partA, sheetName + partB};
   }
}

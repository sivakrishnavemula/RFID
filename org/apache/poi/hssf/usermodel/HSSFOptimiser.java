package org.apache.poi.hssf.usermodel;

import java.util.HashSet;
import org.apache.poi.hssf.record.ExtendedFormatRecord;
import org.apache.poi.hssf.record.FontRecord;
import org.apache.poi.hssf.record.StyleRecord;
import org.apache.poi.hssf.record.common.UnicodeString;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

public class HSSFOptimiser {
   public static void optimiseFonts(HSSFWorkbook workbook) {
      short[] newPos = new short[workbook.getWorkbook().getNumberOfFontRecords() + 1];
      boolean[] zapRecords = new boolean[newPos.length];

      for (int i = 0; i < newPos.length; i++) {
         newPos[i] = (short)i;
         zapRecords[i] = false;
      }

      FontRecord[] frecs = new FontRecord[newPos.length];

      for (int i = 0; i < newPos.length; i++) {
         if (i != 4) {
            frecs[i] = workbook.getWorkbook().getFontRecordAt(i);
         }
      }

      for (int i = 5; i < newPos.length; i++) {
         int earlierDuplicate = -1;

         for (int j = 0; j < i && earlierDuplicate == -1; j++) {
            if (j != 4) {
               FontRecord frCheck = workbook.getWorkbook().getFontRecordAt(j);
               if (frCheck.sameProperties(frecs[i])) {
                  earlierDuplicate = j;
               }
            }
         }

         if (earlierDuplicate != -1) {
            newPos[i] = (short)earlierDuplicate;
            zapRecords[i] = true;
         }
      }

      for (int i = 5; i < newPos.length; i++) {
         short preDeletePos = newPos[i];
         short newPosition = preDeletePos;

         for (int j = 0; j < preDeletePos; j++) {
            if (zapRecords[j]) {
               newPosition--;
            }
         }

         newPos[i] = newPosition;
      }

      for (int i = 5; i < newPos.length; i++) {
         if (zapRecords[i]) {
            workbook.getWorkbook().removeFontRecord(frecs[i]);
         }
      }

      workbook.resetFontCache();

      for (int i = 0; i < workbook.getWorkbook().getNumExFormats(); i++) {
         ExtendedFormatRecord xfr = workbook.getWorkbook().getExFormatAt(i);
         xfr.setFontIndex(newPos[xfr.getFontIndex()]);
      }

      HashSet<UnicodeString> doneUnicodeStrings = new HashSet();

      for (int sheetNum = 0; sheetNum < workbook.getNumberOfSheets(); sheetNum++) {
         for (Row row : workbook.getSheetAt(sheetNum)) {
            for (Cell cell : row) {
               if (cell.getCellType() == CellType.STRING) {
                  HSSFRichTextString rtr = (HSSFRichTextString)cell.getRichStringCellValue();
                  UnicodeString u = rtr.getRawUnicodeString();
                  if (!doneUnicodeStrings.contains(u)) {
                     for (short i = 5; i < newPos.length; i++) {
                        if (i != newPos[i]) {
                           u.swapFontUse(i, newPos[i]);
                        }
                     }

                     doneUnicodeStrings.add(u);
                  }
               }
            }
         }
      }
   }

   public static void optimiseCellStyles(HSSFWorkbook workbook) {
      short[] newPos = new short[workbook.getWorkbook().getNumExFormats()];
      boolean[] isUsed = new boolean[newPos.length];
      boolean[] zapRecords = new boolean[newPos.length];

      for (int i = 0; i < newPos.length; i++) {
         isUsed[i] = false;
         newPos[i] = (short)i;
         zapRecords[i] = false;
      }

      ExtendedFormatRecord[] xfrs = new ExtendedFormatRecord[newPos.length];

      for (int i = 0; i < newPos.length; i++) {
         xfrs[i] = workbook.getWorkbook().getExFormatAt(i);
      }

      for (int i = 21; i < newPos.length; i++) {
         int earlierDuplicate = -1;

         for (int j = 0; j < i && earlierDuplicate == -1; j++) {
            ExtendedFormatRecord xfCheck = workbook.getWorkbook().getExFormatAt(j);
            if (xfCheck.equals(xfrs[i]) && !isUserDefined(workbook, j)) {
               earlierDuplicate = j;
            }
         }

         if (earlierDuplicate != -1) {
            newPos[i] = (short)earlierDuplicate;
            zapRecords[i] = true;
         }
      }

      for (int sheetNum = 0; sheetNum < workbook.getNumberOfSheets(); sheetNum++) {
         HSSFSheet s = workbook.getSheetAt(sheetNum);

         for (Row row : s) {
            for (Cell cellI : row) {
               HSSFCell cell = (HSSFCell)cellI;
               short oldXf = cell.getCellValueRecord().getXFIndex();
               if (oldXf < newPos.length) {
                  isUsed[oldXf] = true;
               }
            }

            short oldXf = ((HSSFRow)row).getRowRecord().getXFIndex();
            if (oldXf < newPos.length) {
               isUsed[oldXf] = true;
            }
         }

         for (int col = s.getSheet().getMinColumnIndex(); col <= s.getSheet().getMaxColumnIndex(); col++) {
            short oldXf = s.getSheet().getXFIndexForColAt((short)col);
            if (oldXf < newPos.length) {
               isUsed[oldXf] = true;
            }
         }
      }

      for (int i = 21; i < isUsed.length; i++) {
         if (isUserDefined(workbook, i)) {
            isUsed[i] = true;
         }

         if (newPos[i] != i && isUsed[i]) {
            isUsed[newPos[i]] = true;
         }
      }

      for (int i = 21; i < isUsed.length; i++) {
         if (!isUsed[i]) {
            zapRecords[i] = true;
            newPos[i] = 0;
         }
      }

      for (int i = 21; i < newPos.length; i++) {
         short preDeletePos = newPos[i];
         short newPosition = preDeletePos;

         for (int j = 0; j < preDeletePos; j++) {
            if (zapRecords[j]) {
               newPosition--;
            }
         }

         newPos[i] = newPosition;
         if (i != newPosition && newPosition != 0) {
            workbook.getWorkbook().updateStyleRecord(i, newPosition);
            ExtendedFormatRecord exFormat = workbook.getWorkbook().getExFormatAt(i);
            short oldParent = exFormat.getParentIndex();
            if (oldParent < newPos.length) {
               short newParent = newPos[oldParent];
               exFormat.setParentIndex(newParent);
            }
         }
      }

      int max = newPos.length;
      int removed = 0;

      for (int i = 21; i < max; i++) {
         if (zapRecords[i + removed]) {
            workbook.getWorkbook().removeExFormatRecord(i);
            i--;
            max--;
            removed++;
         }
      }

      for (int sheetNum = 0; sheetNum < workbook.getNumberOfSheets(); sheetNum++) {
         HSSFSheet s = workbook.getSheetAt(sheetNum);

         for (Row row : s) {
            for (Cell cell : row) {
               short oldXf = ((HSSFCell)cell).getCellValueRecord().getXFIndex();
               if (oldXf < newPos.length) {
                  HSSFCellStyle newStyle = workbook.getCellStyleAt(newPos[oldXf]);
                  cell.setCellStyle(newStyle);
               }
            }

            short oldXf = ((HSSFRow)row).getRowRecord().getXFIndex();
            if (oldXf < newPos.length) {
               HSSFCellStyle newStyle = workbook.getCellStyleAt(newPos[oldXf]);
               row.setRowStyle(newStyle);
            }
         }

         for (int col = s.getSheet().getMinColumnIndex(); col <= s.getSheet().getMaxColumnIndex(); col++) {
            short oldXf = s.getSheet().getXFIndexForColAt((short)col);
            if (oldXf < newPos.length) {
               HSSFCellStyle newStyle = workbook.getCellStyleAt(newPos[oldXf]);
               s.setDefaultColumnStyle(col, newStyle);
            }
         }
      }
   }

   private static boolean isUserDefined(HSSFWorkbook workbook, int index) {
      StyleRecord styleRecord = workbook.getWorkbook().getStyleRecord(index);
      return styleRecord != null && !styleRecord.isBuiltin() && styleRecord.getName() != null;
   }
}

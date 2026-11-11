package org.apache.poi.ss.usermodel;

import java.util.Calendar;
import java.util.Date;
import org.apache.poi.ss.formula.FormulaParseException;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Removal;

public interface Cell {
   int getColumnIndex();

   int getRowIndex();

   Sheet getSheet();

   Row getRow();

   void setCellType(CellType var1);

   CellType getCellType();

   @Deprecated
   @Removal(version = "4.2")
   CellType getCellTypeEnum();

   CellType getCachedFormulaResultType();

   @Deprecated
   @Removal(version = "4.2")
   CellType getCachedFormulaResultTypeEnum();

   void setCellValue(double var1);

   void setCellValue(Date var1);

   void setCellValue(Calendar var1);

   void setCellValue(RichTextString var1);

   void setCellValue(String var1);

   void setCellFormula(String var1) throws FormulaParseException;

   String getCellFormula();

   double getNumericCellValue();

   Date getDateCellValue();

   RichTextString getRichStringCellValue();

   String getStringCellValue();

   void setCellValue(boolean var1);

   void setCellErrorValue(byte var1);

   boolean getBooleanCellValue();

   byte getErrorCellValue();

   void setCellStyle(CellStyle var1);

   CellStyle getCellStyle();

   void setAsActiveCell();

   CellAddress getAddress();

   void setCellComment(Comment var1);

   Comment getCellComment();

   void removeCellComment();

   Hyperlink getHyperlink();

   void setHyperlink(Hyperlink var1);

   void removeHyperlink();

   CellRangeAddress getArrayFormulaRange();

   boolean isPartOfArrayFormulaGroup();
}

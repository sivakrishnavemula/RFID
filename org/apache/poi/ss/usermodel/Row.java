package org.apache.poi.ss.usermodel;

import java.util.Iterator;

public interface Row extends Iterable<Cell> {
   Cell createCell(int var1);

   Cell createCell(int var1, CellType var2);

   void removeCell(Cell var1);

   void setRowNum(int var1);

   int getRowNum();

   Cell getCell(int var1);

   Cell getCell(int var1, Row.MissingCellPolicy var2);

   short getFirstCellNum();

   short getLastCellNum();

   int getPhysicalNumberOfCells();

   void setHeight(short var1);

   void setZeroHeight(boolean var1);

   boolean getZeroHeight();

   void setHeightInPoints(float var1);

   short getHeight();

   float getHeightInPoints();

   boolean isFormatted();

   CellStyle getRowStyle();

   void setRowStyle(CellStyle var1);

   Iterator<Cell> cellIterator();

   Sheet getSheet();

   int getOutlineLevel();

   void shiftCellsRight(int var1, int var2, int var3);

   void shiftCellsLeft(int var1, int var2, int var3);

   enum MissingCellPolicy {
      RETURN_NULL_AND_BLANK,
      RETURN_BLANK_AS_NULL,
      CREATE_NULL_AS_BLANK;
   }
}

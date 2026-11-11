package org.apache.poi.hssf.record;

public interface CellValueRecordInterface {
   int getRow();

   short getColumn();

   void setRow(int var1);

   void setColumn(short var1);

   void setXFIndex(short var1);

   short getXFIndex();
}

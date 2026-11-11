package org.apache.poi.ss.usermodel;

public interface Hyperlink extends org.apache.poi.common.usermodel.Hyperlink {
   int getFirstRow();

   void setFirstRow(int var1);

   int getLastRow();

   void setLastRow(int var1);

   int getFirstColumn();

   void setFirstColumn(int var1);

   int getLastColumn();

   void setLastColumn(int var1);
}

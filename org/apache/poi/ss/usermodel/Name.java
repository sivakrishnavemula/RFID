package org.apache.poi.ss.usermodel;

public interface Name {
   String getSheetName();

   String getNameName();

   void setNameName(String var1);

   String getRefersToFormula();

   void setRefersToFormula(String var1);

   boolean isFunctionName();

   boolean isDeleted();

   void setSheetIndex(int var1);

   int getSheetIndex();

   String getComment();

   void setComment(String var1);

   void setFunction(boolean var1);
}

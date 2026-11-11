package org.apache.poi.ss.usermodel;

import org.apache.poi.ss.util.CellAddress;

public interface Comment {
   void setVisible(boolean var1);

   boolean isVisible();

   CellAddress getAddress();

   void setAddress(CellAddress var1);

   void setAddress(int var1, int var2);

   int getRow();

   void setRow(int var1);

   int getColumn();

   void setColumn(int var1);

   String getAuthor();

   void setAuthor(String var1);

   RichTextString getString();

   void setString(RichTextString var1);

   ClientAnchor getClientAnchor();
}

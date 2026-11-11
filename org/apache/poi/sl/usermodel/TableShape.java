package org.apache.poi.sl.usermodel;

public interface TableShape<S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> extends Shape<S, P>, PlaceableShape<S, P> {
   int getNumberOfColumns();

   int getNumberOfRows();

   TableCell<S, P> getCell(int var1, int var2);

   double getColumnWidth(int var1);

   void setColumnWidth(int var1, double var2);

   double getRowHeight(int var1);

   void setRowHeight(int var1, double var2);
}

package org.apache.poi.ss.usermodel;

import java.util.Iterator;

public interface CellRange<C extends Cell> extends Iterable<C> {
   int getWidth();

   int getHeight();

   int size();

   String getReferenceText();

   C getTopLeftCell();

   C getCell(int var1, int var2);

   C[] getFlattenedCells();

   C[][] getCells();

   Iterator<C> iterator();
}

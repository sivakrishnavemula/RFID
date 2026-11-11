package org.apache.poi.ss.util;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellRange;
import org.apache.poi.util.Internal;

@Internal
public final class SSCellRange<K extends Cell> implements CellRange<K> {
   private final int _height;
   private final int _width;
   private final K[] _flattenedArray;
   private final int _firstRow;
   private final int _firstColumn;

   private SSCellRange(int firstRow, int firstColumn, int height, int width, K[] flattenedArray) {
      this._firstRow = firstRow;
      this._firstColumn = firstColumn;
      this._height = height;
      this._width = width;
      this._flattenedArray = (K[])((Cell[])flattenedArray.clone());
   }

   public static <B extends Cell> SSCellRange<B> create(int firstRow, int firstColumn, int height, int width, List<B> flattenedList, Class<B> cellClass) {
      int nItems = flattenedList.size();
      if (height * width != nItems) {
         throw new IllegalArgumentException("Array size mismatch.");
      }

      B[] flattenedArray = (B[])Array.newInstance(cellClass, nItems);
      flattenedList.toArray(flattenedArray);
      return new SSCellRange<>(firstRow, firstColumn, height, width, flattenedArray);
   }

   public int getHeight() {
      return this._height;
   }

   public int getWidth() {
      return this._width;
   }

   public int size() {
      return this._height * this._width;
   }

   public String getReferenceText() {
      CellRangeAddress cra = new CellRangeAddress(this._firstRow, this._firstRow + this._height - 1, this._firstColumn, this._firstColumn + this._width - 1);
      return cra.formatAsString();
   }

   public K getTopLeftCell() {
      return this._flattenedArray[0];
   }

   public K getCell(int relativeRowIndex, int relativeColumnIndex) {
      if (relativeRowIndex < 0 || relativeRowIndex >= this._height) {
         throw new ArrayIndexOutOfBoundsException("Specified row " + relativeRowIndex + " is outside the allowable range (0.." + (this._height - 1) + ").");
      } else if (relativeColumnIndex >= 0 && relativeColumnIndex < this._width) {
         int flatIndex = this._width * relativeRowIndex + relativeColumnIndex;
         return this._flattenedArray[flatIndex];
      } else {
         throw new ArrayIndexOutOfBoundsException(
            "Specified colummn " + relativeColumnIndex + " is outside the allowable range (0.." + (this._width - 1) + ")."
         );
      }
   }

   public K[] getFlattenedCells() {
      return (K[])((Cell[])this._flattenedArray.clone());
   }

   public K[][] getCells() {
      Class<?> itemCls = this._flattenedArray.getClass();
      K[][] result = (K[][])((Cell[][])Array.newInstance(itemCls, this._height));
      itemCls = itemCls.getComponentType();

      for (int r = this._height - 1; r >= 0; r--) {
         K[] row = (K[])((Cell[])Array.newInstance(itemCls, this._width));
         int flatIndex = this._width * r;
         System.arraycopy(this._flattenedArray, flatIndex, row, 0, this._width);
      }

      return result;
   }

   public Iterator<K> iterator() {
      return new SSCellRange.ArrayIterator<>(this._flattenedArray);
   }

   private static final class ArrayIterator<D> implements Iterator<D> {
      private final D[] _array;
      private int _index;

      public ArrayIterator(D[] array) {
         this._array = (D[])((Object[])array.clone());
         this._index = 0;
      }

      public boolean hasNext() {
         return this._index < this._array.length;
      }

      public D next() {
         if (this._index >= this._array.length) {
            throw new NoSuchElementException(String.valueOf(this._index));
         } else {
            return this._array[this._index++];
         }
      }

      public void remove() {
         throw new UnsupportedOperationException("Cannot remove cells from this CellRange.");
      }
   }
}

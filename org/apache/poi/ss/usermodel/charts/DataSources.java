package org.apache.poi.ss.usermodel.charts;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Removal;

@Deprecated
@Removal(version = "4.2")
public class DataSources {
   private DataSources() {
   }

   public static <T> ChartDataSource<T> fromArray(T[] elements) {
      return new DataSources.ArrayDataSource<>(elements);
   }

   public static ChartDataSource<Number> fromNumericCellRange(Sheet sheet, CellRangeAddress cellRangeAddress) {
      return new DataSources.AbstractCellRangeDataSource<Number>(sheet, cellRangeAddress) {
         public Number getPointAt(int index) {
            CellValue cellValue = this.getCellValueAt(index);
            return cellValue != null && cellValue.getCellType() == CellType.NUMERIC ? cellValue.getNumberValue() : null;
         }

         public boolean isNumeric() {
            return true;
         }
      };
   }

   public static ChartDataSource<String> fromStringCellRange(Sheet sheet, CellRangeAddress cellRangeAddress) {
      return new DataSources.AbstractCellRangeDataSource<String>(sheet, cellRangeAddress) {
         public String getPointAt(int index) {
            CellValue cellValue = this.getCellValueAt(index);
            return cellValue != null && cellValue.getCellType() == CellType.STRING ? cellValue.getStringValue() : null;
         }

         public boolean isNumeric() {
            return false;
         }
      };
   }

   private abstract static class AbstractCellRangeDataSource<T> implements ChartDataSource<T> {
      private final Sheet sheet;
      private final CellRangeAddress cellRangeAddress;
      private final int numOfCells;
      private FormulaEvaluator evaluator;

      protected AbstractCellRangeDataSource(Sheet sheet, CellRangeAddress cellRangeAddress) {
         this.sheet = sheet;
         this.cellRangeAddress = cellRangeAddress.copy();
         this.numOfCells = this.cellRangeAddress.getNumberOfCells();
         this.evaluator = sheet.getWorkbook().getCreationHelper().createFormulaEvaluator();
      }

      public int getPointCount() {
         return this.numOfCells;
      }

      public boolean isReference() {
         return true;
      }

      public String getFormulaString() {
         return this.cellRangeAddress.formatAsString(this.sheet.getSheetName(), true);
      }

      protected CellValue getCellValueAt(int index) {
         if (index >= 0 && index < this.numOfCells) {
            int firstRow = this.cellRangeAddress.getFirstRow();
            int firstCol = this.cellRangeAddress.getFirstColumn();
            int lastCol = this.cellRangeAddress.getLastColumn();
            int width = lastCol - firstCol + 1;
            int rowIndex = firstRow + index / width;
            int cellIndex = firstCol + index % width;
            Row row = this.sheet.getRow(rowIndex);
            return row == null ? null : this.evaluator.evaluate(row.getCell(cellIndex));
         } else {
            throw new IndexOutOfBoundsException("Index must be between 0 and " + (this.numOfCells - 1) + " (inclusive), given: " + index);
         }
      }
   }

   private static class ArrayDataSource<T> implements ChartDataSource<T> {
      private final T[] elements;

      public ArrayDataSource(T[] elements) {
         this.elements = (T[])((Object[])elements.clone());
      }

      public int getPointCount() {
         return this.elements.length;
      }

      public T getPointAt(int index) {
         return this.elements[index];
      }

      public boolean isReference() {
         return false;
      }

      public boolean isNumeric() {
         Class<?> arrayComponentType = this.elements.getClass().getComponentType();
         return Number.class.isAssignableFrom(arrayComponentType);
      }

      public String getFormulaString() {
         throw new UnsupportedOperationException("Literal data source can not be expressed by reference.");
      }
   }
}

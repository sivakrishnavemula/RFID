package org.apache.poi.hssf.usermodel;

import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.formula.EvaluationSheet;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Removal;

final class HSSFEvaluationCell implements EvaluationCell {
   private final EvaluationSheet _evalSheet;
   private final HSSFCell _cell;

   public HSSFEvaluationCell(HSSFCell cell, EvaluationSheet evalSheet) {
      this._cell = cell;
      this._evalSheet = evalSheet;
   }

   public HSSFEvaluationCell(HSSFCell cell) {
      this(cell, new HSSFEvaluationSheet(cell.getSheet()));
   }

   public Object getIdentityKey() {
      return this._cell;
   }

   public HSSFCell getHSSFCell() {
      return this._cell;
   }

   public boolean getBooleanCellValue() {
      return this._cell.getBooleanCellValue();
   }

   public CellType getCellType() {
      return this._cell.getCellType();
   }

   @Deprecated
   @Removal(version = "4.2")
   public CellType getCellTypeEnum() {
      return this.getCellType();
   }

   public int getColumnIndex() {
      return this._cell.getColumnIndex();
   }

   public int getErrorCellValue() {
      return this._cell.getErrorCellValue();
   }

   public double getNumericCellValue() {
      return this._cell.getNumericCellValue();
   }

   public int getRowIndex() {
      return this._cell.getRowIndex();
   }

   public EvaluationSheet getSheet() {
      return this._evalSheet;
   }

   public String getStringCellValue() {
      return this._cell.getRichStringCellValue().getString();
   }

   public CellRangeAddress getArrayFormulaRange() {
      return this._cell.getArrayFormulaRange();
   }

   public boolean isPartOfArrayFormulaGroup() {
      return this._cell.isPartOfArrayFormulaGroup();
   }

   public CellType getCachedFormulaResultType() {
      return this._cell.getCachedFormulaResultType();
   }

   @Deprecated
   @Removal(version = "4.2")
   public CellType getCachedFormulaResultTypeEnum() {
      return this.getCachedFormulaResultType();
   }
}

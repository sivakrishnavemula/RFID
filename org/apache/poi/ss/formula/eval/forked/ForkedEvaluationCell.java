package org.apache.poi.ss.formula.eval.forked;

import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.formula.EvaluationSheet;
import org.apache.poi.ss.formula.eval.BlankEval;
import org.apache.poi.ss.formula.eval.BoolEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.StringEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Removal;

final class ForkedEvaluationCell implements EvaluationCell {
   private final EvaluationSheet _sheet;
   private final EvaluationCell _masterCell;
   private boolean _booleanValue;
   private CellType _cellType;
   private int _errorValue;
   private double _numberValue;
   private String _stringValue;

   public ForkedEvaluationCell(ForkedEvaluationSheet sheet, EvaluationCell masterCell) {
      this._sheet = sheet;
      this._masterCell = masterCell;
      this.setValue(BlankEval.instance);
   }

   public Object getIdentityKey() {
      return this._masterCell.getIdentityKey();
   }

   public void setValue(ValueEval value) {
      Class<? extends ValueEval> cls = value.getClass();
      if (cls == NumberEval.class) {
         this._cellType = CellType.NUMERIC;
         this._numberValue = ((NumberEval)value).getNumberValue();
      } else if (cls == StringEval.class) {
         this._cellType = CellType.STRING;
         this._stringValue = ((StringEval)value).getStringValue();
      } else if (cls == BoolEval.class) {
         this._cellType = CellType.BOOLEAN;
         this._booleanValue = ((BoolEval)value).getBooleanValue();
      } else if (cls == ErrorEval.class) {
         this._cellType = CellType.ERROR;
         this._errorValue = ((ErrorEval)value).getErrorCode();
      } else if (cls == BlankEval.class) {
         this._cellType = CellType.BLANK;
      } else {
         throw new IllegalArgumentException("Unexpected value class (" + cls.getName() + ")");
      }
   }

   public void copyValue(Cell destCell) {
      switch (this._cellType) {
         case BLANK:
            destCell.setCellType(CellType.BLANK);
            return;
         case NUMERIC:
            destCell.setCellValue(this._numberValue);
            return;
         case BOOLEAN:
            destCell.setCellValue(this._booleanValue);
            return;
         case STRING:
            destCell.setCellValue(this._stringValue);
            return;
         case ERROR:
            destCell.setCellErrorValue((byte)this._errorValue);
            return;
         default:
            throw new IllegalStateException("Unexpected data type (" + this._cellType + ")");
      }
   }

   private void checkCellType(CellType expectedCellType) {
      if (this._cellType != expectedCellType) {
         throw new RuntimeException("Wrong data type (" + this._cellType + ")");
      }
   }

   public CellType getCellType() {
      return this._cellType;
   }

   @Deprecated
   @Removal(version = "4.2")
   public CellType getCellTypeEnum() {
      return this.getCellType();
   }

   public boolean getBooleanCellValue() {
      this.checkCellType(CellType.BOOLEAN);
      return this._booleanValue;
   }

   public int getErrorCellValue() {
      this.checkCellType(CellType.ERROR);
      return this._errorValue;
   }

   public double getNumericCellValue() {
      this.checkCellType(CellType.NUMERIC);
      return this._numberValue;
   }

   public String getStringCellValue() {
      this.checkCellType(CellType.STRING);
      return this._stringValue;
   }

   public EvaluationSheet getSheet() {
      return this._sheet;
   }

   public int getRowIndex() {
      return this._masterCell.getRowIndex();
   }

   public int getColumnIndex() {
      return this._masterCell.getColumnIndex();
   }

   public CellRangeAddress getArrayFormulaRange() {
      return this._masterCell.getArrayFormulaRange();
   }

   public boolean isPartOfArrayFormulaGroup() {
      return this._masterCell.isPartOfArrayFormulaGroup();
   }

   public CellType getCachedFormulaResultType() {
      return this._masterCell.getCachedFormulaResultType();
   }

   @Deprecated
   @Removal(version = "4.2")
   public CellType getCachedFormulaResultTypeEnum() {
      return this.getCachedFormulaResultType();
   }
}

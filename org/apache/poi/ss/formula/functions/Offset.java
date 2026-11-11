package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.eval.AreaEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.MissingArgEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.RefEval;
import org.apache.poi.ss.formula.eval.ValueEval;

public final class Offset implements Function {
   private static final int LAST_VALID_ROW_INDEX = 65535;
   private static final int LAST_VALID_COLUMN_INDEX = 255;

   public ValueEval evaluate(ValueEval[] args, int srcCellRow, int srcCellCol) {
      if (args.length >= 1 && args.length <= 5) {
         try {
            Offset.BaseRef baseRef = evaluateBaseRef(args[0]);
            int rowOffset = args[1] instanceof MissingArgEval ? 0 : evaluateIntArg(args[1], srcCellRow, srcCellCol);
            int columnOffset = args[2] instanceof MissingArgEval ? 0 : evaluateIntArg(args[2], srcCellRow, srcCellCol);
            int height = baseRef.getHeight();
            int width = baseRef.getWidth();
            switch (args.length) {
               case 5:
                  if (!(args[4] instanceof MissingArgEval)) {
                     width = evaluateIntArg(args[4], srcCellRow, srcCellCol);
                  }
               case 4:
                  if (!(args[3] instanceof MissingArgEval)) {
                     height = evaluateIntArg(args[3], srcCellRow, srcCellCol);
                  }
               default:
                  if (height != 0 && width != 0) {
                     Offset.LinearOffsetRange rowOffsetRange = new Offset.LinearOffsetRange(rowOffset, height);
                     Offset.LinearOffsetRange colOffsetRange = new Offset.LinearOffsetRange(columnOffset, width);
                     return createOffset(baseRef, rowOffsetRange, colOffsetRange);
                  } else {
                     return ErrorEval.REF_INVALID;
                  }
            }
         } catch (EvaluationException e) {
            return e.getErrorEval();
         }
      } else {
         return ErrorEval.VALUE_INVALID;
      }
   }

   private static AreaEval createOffset(Offset.BaseRef baseRef, Offset.LinearOffsetRange orRow, Offset.LinearOffsetRange orCol) throws EvaluationException {
      Offset.LinearOffsetRange absRows = orRow.normaliseAndTranslate(baseRef.getFirstRowIndex());
      Offset.LinearOffsetRange absCols = orCol.normaliseAndTranslate(baseRef.getFirstColumnIndex());
      if (absRows.isOutOfBounds(0, 65535)) {
         throw new EvaluationException(ErrorEval.REF_INVALID);
      } else if (absCols.isOutOfBounds(0, 255)) {
         throw new EvaluationException(ErrorEval.REF_INVALID);
      } else {
         return baseRef.offset(orRow.getFirstIndex(), orRow.getLastIndex(), orCol.getFirstIndex(), orCol.getLastIndex());
      }
   }

   private static Offset.BaseRef evaluateBaseRef(ValueEval eval) throws EvaluationException {
      if (eval instanceof RefEval) {
         return new Offset.BaseRef((RefEval)eval);
      } else if (eval instanceof AreaEval) {
         return new Offset.BaseRef((AreaEval)eval);
      } else if (eval instanceof ErrorEval) {
         throw new EvaluationException((ErrorEval)eval);
      } else {
         throw new EvaluationException(ErrorEval.VALUE_INVALID);
      }
   }

   static int evaluateIntArg(ValueEval eval, int srcCellRow, int srcCellCol) throws EvaluationException {
      ValueEval ve = OperandResolver.getSingleValue(eval, srcCellRow, srcCellCol);
      return OperandResolver.coerceValueToInt(ve);
   }

   private static final class BaseRef {
      private final int _firstRowIndex;
      private final int _firstColumnIndex;
      private final int _width;
      private final int _height;
      private final RefEval _refEval;
      private final AreaEval _areaEval;

      public BaseRef(RefEval re) {
         this._refEval = re;
         this._areaEval = null;
         this._firstRowIndex = re.getRow();
         this._firstColumnIndex = re.getColumn();
         this._height = 1;
         this._width = 1;
      }

      public BaseRef(AreaEval ae) {
         this._refEval = null;
         this._areaEval = ae;
         this._firstRowIndex = ae.getFirstRow();
         this._firstColumnIndex = ae.getFirstColumn();
         this._height = ae.getLastRow() - ae.getFirstRow() + 1;
         this._width = ae.getLastColumn() - ae.getFirstColumn() + 1;
      }

      public int getWidth() {
         return this._width;
      }

      public int getHeight() {
         return this._height;
      }

      public int getFirstRowIndex() {
         return this._firstRowIndex;
      }

      public int getFirstColumnIndex() {
         return this._firstColumnIndex;
      }

      public AreaEval offset(int relFirstRowIx, int relLastRowIx, int relFirstColIx, int relLastColIx) {
         return this._refEval == null
            ? this._areaEval.offset(relFirstRowIx, relLastRowIx, relFirstColIx, relLastColIx)
            : this._refEval.offset(relFirstRowIx, relLastRowIx, relFirstColIx, relLastColIx);
      }
   }

   static final class LinearOffsetRange {
      private final int _offset;
      private final int _length;

      public LinearOffsetRange(int offset, int length) {
         if (length == 0) {
            throw new RuntimeException("length may not be zero");
         }

         this._offset = offset;
         this._length = length;
      }

      public short getFirstIndex() {
         return (short)this._offset;
      }

      public short getLastIndex() {
         return (short)(this._offset + this._length - 1);
      }

      public Offset.LinearOffsetRange normaliseAndTranslate(int translationAmount) {
         if (this._length > 0) {
            return translationAmount == 0 ? this : new Offset.LinearOffsetRange(translationAmount + this._offset, this._length);
         } else {
            return new Offset.LinearOffsetRange(translationAmount + this._offset + this._length + 1, -this._length);
         }
      }

      public boolean isOutOfBounds(int lowValidIx, int highValidIx) {
         return this._offset < lowValidIx ? true : this.getLastIndex() > highValidIx;
      }

      public String toString() {
         StringBuilder sb = new StringBuilder(64);
         sb.append(this.getClass().getName()).append(" [");
         sb.append(this._offset).append("...").append(this.getLastIndex());
         sb.append("]");
         return sb.toString();
      }
   }
}

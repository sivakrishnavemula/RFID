package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.ThreeDEval;
import org.apache.poi.ss.formula.TwoDEval;
import org.apache.poi.ss.formula.eval.BlankEval;
import org.apache.poi.ss.formula.eval.BoolEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.NumericValueEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.RefEval;
import org.apache.poi.ss.formula.eval.StringValueEval;
import org.apache.poi.ss.formula.eval.ValueEval;

public abstract class MultiOperandNumericFunction implements Function {
   private final boolean _isReferenceBoolCounted;
   private final boolean _isBlankCounted;
   static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
   private static final int DEFAULT_MAX_NUM_OPERANDS = SpreadsheetVersion.EXCEL2007.getMaxFunctionArgs();

   protected MultiOperandNumericFunction(boolean isReferenceBoolCounted, boolean isBlankCounted) {
      this._isReferenceBoolCounted = isReferenceBoolCounted;
      this._isBlankCounted = isBlankCounted;
   }

   public final ValueEval evaluate(ValueEval[] args, int srcCellRow, int srcCellCol) {
      double d;
      try {
         double[] values = this.getNumberArray(args);
         d = this.evaluate(values);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }

      return (ValueEval)(!Double.isNaN(d) && !Double.isInfinite(d) ? new NumberEval(d) : ErrorEval.NUM_ERROR);
   }

   protected abstract double evaluate(double[] var1) throws EvaluationException;

   protected int getMaxNumOperands() {
      return DEFAULT_MAX_NUM_OPERANDS;
   }

   protected final double[] getNumberArray(ValueEval[] operands) throws EvaluationException {
      if (operands.length > this.getMaxNumOperands()) {
         throw EvaluationException.invalidValue();
      }

      MultiOperandNumericFunction.DoubleList retval = new MultiOperandNumericFunction.DoubleList();
      int i = 0;

      for (int iSize = operands.length; i < iSize; i++) {
         this.collectValues(operands[i], retval);
      }

      return retval.toArray();
   }

   public boolean isSubtotalCounted() {
      return true;
   }

   private void collectValues(ValueEval operand, MultiOperandNumericFunction.DoubleList temp) throws EvaluationException {
      if (operand instanceof ThreeDEval) {
         ThreeDEval ae = (ThreeDEval)operand;

         for (int sIx = ae.getFirstSheetIndex(); sIx <= ae.getLastSheetIndex(); sIx++) {
            int width = ae.getWidth();
            int height = ae.getHeight();

            for (int rrIx = 0; rrIx < height; rrIx++) {
               for (int rcIx = 0; rcIx < width; rcIx++) {
                  ValueEval ve = ae.getValue(sIx, rrIx, rcIx);
                  if (this.isSubtotalCounted() || !ae.isSubTotal(rrIx, rcIx)) {
                     this.collectValue(ve, true, temp);
                  }
               }
            }
         }
      } else if (operand instanceof TwoDEval) {
         TwoDEval ae = (TwoDEval)operand;
         int width = ae.getWidth();
         int height = ae.getHeight();

         for (int rrIx = 0; rrIx < height; rrIx++) {
            for (int rcIx = 0; rcIx < width; rcIx++) {
               ValueEval ve = ae.getValue(rrIx, rcIx);
               if (this.isSubtotalCounted() || !ae.isSubTotal(rrIx, rcIx)) {
                  this.collectValue(ve, true, temp);
               }
            }
         }
      } else if (!(operand instanceof RefEval)) {
         this.collectValue(operand, false, temp);
      } else {
         RefEval re = (RefEval)operand;

         for (int sIx = re.getFirstSheetIndex(); sIx <= re.getLastSheetIndex(); sIx++) {
            this.collectValue(re.getInnerValueEval(sIx), true, temp);
         }
      }
   }

   private void collectValue(ValueEval ve, boolean isViaReference, MultiOperandNumericFunction.DoubleList temp) throws EvaluationException {
      if (ve == null) {
         throw new IllegalArgumentException("ve must not be null");
      }

      if (!(ve instanceof BoolEval)) {
         if (ve instanceof NumericValueEval) {
            NumericValueEval ne = (NumericValueEval)ve;
            temp.add(ne.getNumberValue());
         } else if (ve instanceof StringValueEval) {
            if (!isViaReference) {
               String s = ((StringValueEval)ve).getStringValue();
               Double d = OperandResolver.parseDouble(s);
               if (d == null) {
                  throw new EvaluationException(ErrorEval.VALUE_INVALID);
               }

               temp.add(d);
            }
         } else {
            if (ve instanceof ErrorEval) {
               throw new EvaluationException((ErrorEval)ve);
            }

            if (ve == BlankEval.instance) {
               if (this._isBlankCounted) {
                  temp.add(0.0);
               }
            } else {
               throw new RuntimeException("Invalid ValueEval type passed for conversion: (" + ve.getClass() + ")");
            }
         }
      } else {
         if (!isViaReference || this._isReferenceBoolCounted) {
            BoolEval boolEval = (BoolEval)ve;
            temp.add(boolEval.getNumberValue());
         }
      }
   }

   private static class DoubleList {
      private double[] _array = new double[8];
      private int _count = 0;

      public DoubleList() {
      }

      public double[] toArray() {
         if (this._count < 1) {
            return MultiOperandNumericFunction.EMPTY_DOUBLE_ARRAY;
         }

         double[] result = new double[this._count];
         System.arraycopy(this._array, 0, result, 0, this._count);
         return result;
      }

      private void ensureCapacity(int reqSize) {
         if (reqSize > this._array.length) {
            int newSize = reqSize * 3 / 2;
            double[] newArr = new double[newSize];
            System.arraycopy(this._array, 0, newArr, 0, this._count);
            this._array = newArr;
         }
      }

      public void add(double value) {
         this.ensureCapacity(this._count + 1);
         this._array[this._count] = value;
         this._count++;
      }
   }
}

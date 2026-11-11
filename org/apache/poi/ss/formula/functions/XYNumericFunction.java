package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.TwoDEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.RefEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.LookupUtils.ValueVector;

public abstract class XYNumericFunction extends Fixed2ArgFunction {
   protected abstract XYNumericFunction.Accumulator createAccumulator();

   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
      double result;
      try {
         ValueVector vvX = createValueVector(arg0);
         ValueVector vvY = createValueVector(arg1);
         int size = vvX.getSize();
         if (size == 0 || vvY.getSize() != size) {
            return ErrorEval.NA;
         }

         result = this.evaluateInternal(vvX, vvY, size);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }

      return (ValueEval)(!Double.isNaN(result) && !Double.isInfinite(result) ? new NumberEval(result) : ErrorEval.NUM_ERROR);
   }

   private double evaluateInternal(ValueVector x, ValueVector y, int size) throws EvaluationException {
      XYNumericFunction.Accumulator acc = this.createAccumulator();
      ErrorEval firstXerr = null;
      ErrorEval firstYerr = null;
      boolean accumlatedSome = false;
      double result = 0.0;

      for (int i = 0; i < size; i++) {
         ValueEval vx = x.getItem(i);
         ValueEval vy = y.getItem(i);
         if (vx instanceof ErrorEval && firstXerr == null) {
            firstXerr = (ErrorEval)vx;
         } else if (vy instanceof ErrorEval && firstYerr == null) {
            firstYerr = (ErrorEval)vy;
         } else if (vx instanceof NumberEval && vy instanceof NumberEval) {
            accumlatedSome = true;
            NumberEval nx = (NumberEval)vx;
            NumberEval ny = (NumberEval)vy;
            result += acc.accumulate(nx.getNumberValue(), ny.getNumberValue());
         }
      }

      if (firstXerr != null) {
         throw new EvaluationException(firstXerr);
      } else if (firstYerr != null) {
         throw new EvaluationException(firstYerr);
      } else if (!accumlatedSome) {
         throw new EvaluationException(ErrorEval.DIV_ZERO);
      } else {
         return result;
      }
   }

   private static ValueVector createValueVector(ValueEval arg) throws EvaluationException {
      if (arg instanceof ErrorEval) {
         throw new EvaluationException((ErrorEval)arg);
      } else if (arg instanceof TwoDEval) {
         return new XYNumericFunction.AreaValueArray((TwoDEval)arg);
      } else {
         return arg instanceof RefEval ? new XYNumericFunction.RefValueArray((RefEval)arg) : new XYNumericFunction.SingleCellValueArray(arg);
      }
   }

   protected interface Accumulator {
      double accumulate(double var1, double var3);
   }

   private static final class AreaValueArray extends XYNumericFunction.ValueArray {
      private final TwoDEval _ae;
      private final int _width;

      public AreaValueArray(TwoDEval ae) {
         super(ae.getWidth() * ae.getHeight());
         this._ae = ae;
         this._width = ae.getWidth();
      }

      @Override
      protected ValueEval getItemInternal(int index) {
         int rowIx = index / this._width;
         int colIx = index % this._width;
         return this._ae.getValue(rowIx, colIx);
      }
   }

   private static final class RefValueArray extends XYNumericFunction.ValueArray {
      private final RefEval _ref;
      private final int _width;

      public RefValueArray(RefEval ref) {
         super(ref.getNumberOfSheets());
         this._ref = ref;
         this._width = ref.getNumberOfSheets();
      }

      @Override
      protected ValueEval getItemInternal(int index) {
         int sIx = index % this._width + this._ref.getFirstSheetIndex();
         return this._ref.getInnerValueEval(sIx);
      }
   }

   private static final class SingleCellValueArray extends XYNumericFunction.ValueArray {
      private final ValueEval _value;

      public SingleCellValueArray(ValueEval value) {
         super(1);
         this._value = value;
      }

      @Override
      protected ValueEval getItemInternal(int index) {
         return this._value;
      }
   }

   private abstract static class ValueArray implements ValueVector {
      private final int _size;

      protected ValueArray(int size) {
         this._size = size;
      }

      public ValueEval getItem(int index) {
         if (index >= 0 && index <= this._size) {
            return this.getItemInternal(index);
         } else {
            throw new IllegalArgumentException("Specified index " + index + " is outside range (0.." + (this._size - 1) + ")");
         }
      }

      protected abstract ValueEval getItemInternal(int var1);

      public final int getSize() {
         return this._size;
      }
   }
}

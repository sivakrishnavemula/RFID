package org.apache.poi.ss.formula.functions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.formula.TwoDEval;
import org.apache.poi.ss.formula.eval.BlankEval;
import org.apache.poi.ss.formula.eval.BoolEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.NumericValueEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.RefEval;
import org.apache.poi.ss.formula.eval.StringEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.Countif.StringMatcher;

final class LookupUtils {
   public static LookupUtils.ValueVector createRowVector(TwoDEval tableArray, int relativeRowIndex) {
      return new LookupUtils.RowVector(tableArray, relativeRowIndex);
   }

   public static LookupUtils.ValueVector createColumnVector(TwoDEval tableArray, int relativeColumnIndex) {
      return new LookupUtils.ColumnVector(tableArray, relativeColumnIndex);
   }

   public static LookupUtils.ValueVector createVector(TwoDEval ae) {
      if (ae.isColumn()) {
         return createColumnVector(ae, 0);
      } else {
         return ae.isRow() ? createRowVector(ae, 0) : null;
      }
   }

   public static LookupUtils.ValueVector createVector(RefEval re) {
      return new LookupUtils.SheetVector(re);
   }

   public static int resolveRowOrColIndexArg(ValueEval rowColIndexArg, int srcCellRow, int srcCellCol) throws EvaluationException {
      if (rowColIndexArg == null) {
         throw new IllegalArgumentException("argument must not be null");
      }

      ValueEval veRowColIndexArg;
      try {
         veRowColIndexArg = OperandResolver.getSingleValue(rowColIndexArg, srcCellRow, (short)srcCellCol);
      } catch (EvaluationException e) {
         throw EvaluationException.invalidRef();
      }

      if (veRowColIndexArg instanceof StringEval) {
         StringEval se = (StringEval)veRowColIndexArg;
         String strVal = se.getStringValue();
         Double dVal = OperandResolver.parseDouble(strVal);
         if (dVal == null) {
            throw EvaluationException.invalidRef();
         }
      }

      int oneBasedIndex = OperandResolver.coerceValueToInt(veRowColIndexArg);
      if (oneBasedIndex < 1) {
         throw EvaluationException.invalidValue();
      } else {
         return oneBasedIndex - 1;
      }
   }

   public static TwoDEval resolveTableArrayArg(ValueEval eval) throws EvaluationException {
      if (eval instanceof TwoDEval) {
         return (TwoDEval)eval;
      } else if (eval instanceof RefEval) {
         RefEval refEval = (RefEval)eval;
         return refEval.offset(0, 0, 0, 0);
      } else {
         throw EvaluationException.invalidValue();
      }
   }

   public static boolean resolveRangeLookupArg(ValueEval rangeLookupArg, int srcCellRow, int srcCellCol) throws EvaluationException {
      ValueEval valEval = OperandResolver.getSingleValue(rangeLookupArg, srcCellRow, srcCellCol);
      if (valEval instanceof BlankEval) {
         return false;
      }

      if (valEval instanceof BoolEval) {
         BoolEval boolEval = (BoolEval)valEval;
         return boolEval.getBooleanValue();
      }

      if (valEval instanceof StringEval) {
         String stringValue = ((StringEval)valEval).getStringValue();
         if (stringValue.length() < 1) {
            throw EvaluationException.invalidValue();
         } else {
            Boolean b = Countif.parseBoolean(stringValue);
            if (b != null) {
               return b;
            } else {
               throw EvaluationException.invalidValue();
            }
         }
      } else if (valEval instanceof NumericValueEval) {
         NumericValueEval nve = (NumericValueEval)valEval;
         return 0.0 != nve.getNumberValue();
      } else {
         throw new RuntimeException("Unexpected eval type (" + valEval + ")");
      }
   }

   public static int lookupIndexOfValue(ValueEval lookupValue, LookupUtils.ValueVector vector, boolean isRangeLookup) throws EvaluationException {
      LookupUtils.LookupValueComparer lookupComparer = createLookupComparer(lookupValue, isRangeLookup, false);
      int result;
      if (isRangeLookup) {
         result = performBinarySearch(vector, lookupComparer);
      } else {
         result = lookupIndexOfExactValue(lookupComparer, vector);
      }

      if (result < 0) {
         throw new EvaluationException(ErrorEval.NA);
      } else {
         return result;
      }
   }

   private static int lookupIndexOfExactValue(LookupUtils.LookupValueComparer lookupComparer, LookupUtils.ValueVector vector) {
      int size = vector.getSize();

      for (int i = 0; i < size; i++) {
         if (lookupComparer.compareTo(vector.getItem(i)).isEqual()) {
            return i;
         }
      }

      return -1;
   }

   private static int performBinarySearch(LookupUtils.ValueVector vector, LookupUtils.LookupValueComparer lookupComparer) {
      LookupUtils.BinarySearchIndexes bsi = new LookupUtils.BinarySearchIndexes(vector.getSize());

      while (true) {
         int midIx = bsi.getMidIx();
         if (midIx < 0) {
            return bsi.getLowIx();
         }

         LookupUtils.CompareResult cr = lookupComparer.compareTo(vector.getItem(midIx));
         if (cr.isTypeMismatch()) {
            int newMidIx = handleMidValueTypeMismatch(lookupComparer, vector, bsi, midIx);
            if (newMidIx < 0) {
               continue;
            }

            midIx = newMidIx;
            cr = lookupComparer.compareTo(vector.getItem(midIx));
         }

         if (cr.isEqual()) {
            return findLastIndexInRunOfEqualValues(lookupComparer, vector, midIx, bsi.getHighIx());
         }

         bsi.narrowSearch(midIx, cr.isLessThan());
      }
   }

   private static int handleMidValueTypeMismatch(
      LookupUtils.LookupValueComparer lookupComparer, LookupUtils.ValueVector vector, LookupUtils.BinarySearchIndexes bsi, int midIx
   ) {
      int newMid = midIx;
      int highIx = bsi.getHighIx();

      while (++newMid != highIx) {
         LookupUtils.CompareResult cr = lookupComparer.compareTo(vector.getItem(newMid));
         if (cr.isLessThan() && newMid == highIx - 1) {
            bsi.narrowSearch(midIx, true);
            return -1;
         }

         if (!cr.isTypeMismatch()) {
            if (cr.isEqual()) {
               return newMid;
            }

            bsi.narrowSearch(newMid, cr.isLessThan());
            return -1;
         }
      }

      bsi.narrowSearch(midIx, true);
      return -1;
   }

   private static int findLastIndexInRunOfEqualValues(
      LookupUtils.LookupValueComparer lookupComparer, LookupUtils.ValueVector vector, int firstFoundIndex, int maxIx
   ) {
      for (int i = firstFoundIndex + 1; i < maxIx; i++) {
         if (!lookupComparer.compareTo(vector.getItem(i)).isEqual()) {
            return i - 1;
         }
      }

      return maxIx - 1;
   }

   public static LookupUtils.LookupValueComparer createLookupComparer(ValueEval lookupValue, boolean matchExact, boolean isMatchFunction) {
      if (lookupValue == BlankEval.instance) {
         return new LookupUtils.NumberLookupComparer(NumberEval.ZERO);
      } else if (lookupValue instanceof StringEval) {
         return new LookupUtils.StringLookupComparer((StringEval)lookupValue, matchExact, isMatchFunction);
      } else if (lookupValue instanceof NumberEval) {
         return new LookupUtils.NumberLookupComparer((NumberEval)lookupValue);
      } else if (lookupValue instanceof BoolEval) {
         return new LookupUtils.BooleanLookupComparer((BoolEval)lookupValue);
      } else {
         throw new IllegalArgumentException("Bad lookup value type (" + lookupValue.getClass().getName() + ")");
      }
   }

   private static final class BinarySearchIndexes {
      private int _lowIx = -1;
      private int _highIx;

      public BinarySearchIndexes(int highIx) {
         this._highIx = highIx;
      }

      public int getMidIx() {
         int ixDiff = this._highIx - this._lowIx;
         return ixDiff < 2 ? -1 : this._lowIx + ixDiff / 2;
      }

      public int getLowIx() {
         return this._lowIx;
      }

      public int getHighIx() {
         return this._highIx;
      }

      public void narrowSearch(int midIx, boolean isLessThan) {
         if (isLessThan) {
            this._highIx = midIx;
         } else {
            this._lowIx = midIx;
         }
      }
   }

   private static final class BooleanLookupComparer extends LookupUtils.LookupValueComparerBase {
      private boolean _value;

      protected BooleanLookupComparer(BoolEval be) {
         super(be);
         this._value = be.getBooleanValue();
      }

      @Override
      protected LookupUtils.CompareResult compareSameType(ValueEval other) {
         BoolEval be = (BoolEval)other;
         boolean otherVal = be.getBooleanValue();
         if (this._value == otherVal) {
            return LookupUtils.CompareResult.EQUAL;
         } else {
            return this._value ? LookupUtils.CompareResult.GREATER_THAN : LookupUtils.CompareResult.LESS_THAN;
         }
      }

      @Override
      protected String getValueAsString() {
         return String.valueOf(this._value);
      }
   }

   private static final class ColumnVector implements LookupUtils.ValueVector {
      private final TwoDEval _tableArray;
      private final int _size;
      private final int _columnIndex;

      public ColumnVector(TwoDEval tableArray, int columnIndex) {
         this._columnIndex = columnIndex;
         int lastColIx = tableArray.getWidth() - 1;
         if (columnIndex >= 0 && columnIndex <= lastColIx) {
            this._tableArray = tableArray;
            this._size = this._tableArray.getHeight();
         } else {
            throw new IllegalArgumentException("Specified column index (" + columnIndex + ") is outside the allowed range (0.." + lastColIx + ")");
         }
      }

      @Override
      public ValueEval getItem(int index) {
         if (index > this._size) {
            throw new ArrayIndexOutOfBoundsException("Specified index (" + index + ") is outside the allowed range (0.." + (this._size - 1) + ")");
         } else {
            return this._tableArray.getValue(index, this._columnIndex);
         }
      }

      @Override
      public int getSize() {
         return this._size;
      }
   }

   public static final class CompareResult {
      private final boolean _isTypeMismatch;
      private final boolean _isLessThan;
      private final boolean _isEqual;
      private final boolean _isGreaterThan;
      public static final LookupUtils.CompareResult TYPE_MISMATCH = new LookupUtils.CompareResult(true, 0);
      public static final LookupUtils.CompareResult LESS_THAN = new LookupUtils.CompareResult(false, -1);
      public static final LookupUtils.CompareResult EQUAL = new LookupUtils.CompareResult(false, 0);
      public static final LookupUtils.CompareResult GREATER_THAN = new LookupUtils.CompareResult(false, 1);

      private CompareResult(boolean isTypeMismatch, int simpleCompareResult) {
         if (isTypeMismatch) {
            this._isTypeMismatch = true;
            this._isLessThan = false;
            this._isEqual = false;
            this._isGreaterThan = false;
         } else {
            this._isTypeMismatch = false;
            this._isLessThan = simpleCompareResult < 0;
            this._isEqual = simpleCompareResult == 0;
            this._isGreaterThan = simpleCompareResult > 0;
         }
      }

      public static LookupUtils.CompareResult valueOf(int simpleCompareResult) {
         if (simpleCompareResult < 0) {
            return LESS_THAN;
         } else {
            return simpleCompareResult > 0 ? GREATER_THAN : EQUAL;
         }
      }

      public static LookupUtils.CompareResult valueOf(boolean matches) {
         return matches ? EQUAL : LESS_THAN;
      }

      public boolean isTypeMismatch() {
         return this._isTypeMismatch;
      }

      public boolean isLessThan() {
         return this._isLessThan;
      }

      public boolean isEqual() {
         return this._isEqual;
      }

      public boolean isGreaterThan() {
         return this._isGreaterThan;
      }

      public String toString() {
         return this.getClass().getName() + " [" + this.formatAsString() + "]";
      }

      private String formatAsString() {
         if (this._isTypeMismatch) {
            return "TYPE_MISMATCH";
         } else if (this._isLessThan) {
            return "LESS_THAN";
         } else if (this._isEqual) {
            return "EQUAL";
         } else {
            return this._isGreaterThan ? "GREATER_THAN" : "??error??";
         }
      }
   }

   public interface LookupValueComparer {
      LookupUtils.CompareResult compareTo(ValueEval var1);
   }

   private abstract static class LookupValueComparerBase implements LookupUtils.LookupValueComparer {
      private final Class<? extends ValueEval> _targetClass;

      protected LookupValueComparerBase(ValueEval targetValue) {
         if (targetValue == null) {
            throw new RuntimeException("targetValue cannot be null");
         }

         this._targetClass = targetValue.getClass();
      }

      @Override
      public final LookupUtils.CompareResult compareTo(ValueEval other) {
         if (other == null) {
            throw new RuntimeException("compare to value cannot be null");
         } else {
            return this._targetClass != other.getClass() ? LookupUtils.CompareResult.TYPE_MISMATCH : this.compareSameType(other);
         }
      }

      public String toString() {
         return this.getClass().getName() + " [" + this.getValueAsString() + "]";
      }

      protected abstract LookupUtils.CompareResult compareSameType(ValueEval var1);

      protected abstract String getValueAsString();
   }

   private static final class NumberLookupComparer extends LookupUtils.LookupValueComparerBase {
      private double _value;

      protected NumberLookupComparer(NumberEval ne) {
         super(ne);
         this._value = ne.getNumberValue();
      }

      @Override
      protected LookupUtils.CompareResult compareSameType(ValueEval other) {
         NumberEval ne = (NumberEval)other;
         return LookupUtils.CompareResult.valueOf(Double.compare(this._value, ne.getNumberValue()));
      }

      @Override
      protected String getValueAsString() {
         return String.valueOf(this._value);
      }
   }

   private static final class RowVector implements LookupUtils.ValueVector {
      private final TwoDEval _tableArray;
      private final int _size;
      private final int _rowIndex;

      public RowVector(TwoDEval tableArray, int rowIndex) {
         this._rowIndex = rowIndex;
         int lastRowIx = tableArray.getHeight() - 1;
         if (rowIndex >= 0 && rowIndex <= lastRowIx) {
            this._tableArray = tableArray;
            this._size = tableArray.getWidth();
         } else {
            throw new IllegalArgumentException("Specified row index (" + rowIndex + ") is outside the allowed range (0.." + lastRowIx + ")");
         }
      }

      @Override
      public ValueEval getItem(int index) {
         if (index > this._size) {
            throw new ArrayIndexOutOfBoundsException("Specified index (" + index + ") is outside the allowed range (0.." + (this._size - 1) + ")");
         } else {
            return this._tableArray.getValue(this._rowIndex, index);
         }
      }

      @Override
      public int getSize() {
         return this._size;
      }
   }

   private static final class SheetVector implements LookupUtils.ValueVector {
      private final RefEval _re;
      private final int _size;

      public SheetVector(RefEval re) {
         this._size = re.getNumberOfSheets();
         this._re = re;
      }

      @Override
      public ValueEval getItem(int index) {
         if (index >= this._size) {
            throw new ArrayIndexOutOfBoundsException("Specified index (" + index + ") is outside the allowed range (0.." + (this._size - 1) + ")");
         }

         int sheetIndex = this._re.getFirstSheetIndex() + index;
         return this._re.getInnerValueEval(sheetIndex);
      }

      @Override
      public int getSize() {
         return this._size;
      }
   }

   private static final class StringLookupComparer extends LookupUtils.LookupValueComparerBase {
      private String _value;
      private final Pattern _wildCardPattern;
      private boolean _matchExact;
      private boolean _isMatchFunction;

      protected StringLookupComparer(StringEval se, boolean matchExact, boolean isMatchFunction) {
         super(se);
         this._value = se.getStringValue();
         this._wildCardPattern = StringMatcher.getWildCardPattern(this._value);
         this._matchExact = matchExact;
         this._isMatchFunction = isMatchFunction;
      }

      @Override
      protected LookupUtils.CompareResult compareSameType(ValueEval other) {
         StringEval se = (StringEval)other;
         String stringValue = se.getStringValue();
         if (this._wildCardPattern != null) {
            Matcher matcher = this._wildCardPattern.matcher(stringValue);
            boolean matches = matcher.matches();
            if (this._isMatchFunction || !this._matchExact) {
               return LookupUtils.CompareResult.valueOf(matches);
            }
         }

         return LookupUtils.CompareResult.valueOf(this._value.compareToIgnoreCase(stringValue));
      }

      @Override
      protected String getValueAsString() {
         return this._value;
      }
   }

   public interface ValueVector {
      ValueEval getItem(int var1);

      int getSize();
   }
}

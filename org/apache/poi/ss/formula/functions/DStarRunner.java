package org.apache.poi.ss.formula.functions;

import java.util.function.Supplier;
import org.apache.poi.ss.formula.eval.AreaEval;
import org.apache.poi.ss.formula.eval.BlankEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.formula.eval.NumericValueEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.StringEval;
import org.apache.poi.ss.formula.eval.StringValueEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.util.NumberComparer;

public final class DStarRunner implements Function3Arg {
   private final DStarRunner.DStarAlgorithmEnum algoType;

   public DStarRunner(DStarRunner.DStarAlgorithmEnum algorithm) {
      this.algoType = algorithm;
   }

   public final ValueEval evaluate(ValueEval[] args, int srcRowIndex, int srcColumnIndex) {
      return (ValueEval)(args.length == 3 ? this.evaluate(srcRowIndex, srcColumnIndex, args[0], args[1], args[2]) : ErrorEval.VALUE_INVALID);
   }

   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval database, ValueEval filterColumn, ValueEval conditionDatabase) {
      if (database instanceof AreaEval && conditionDatabase instanceof AreaEval) {
         AreaEval db = (AreaEval)database;
         AreaEval cdb = (AreaEval)conditionDatabase;

         try {
            filterColumn = OperandResolver.getSingleValue(filterColumn, srcRowIndex, srcColumnIndex);
         } catch (EvaluationException e) {
            return e.getErrorEval();
         }

         int fc;
         try {
            fc = getColumnForName(filterColumn, db);
         } catch (EvaluationException e) {
            return ErrorEval.VALUE_INVALID;
         }

         if (fc == -1) {
            return ErrorEval.VALUE_INVALID;
         }

         IDStarAlgorithm algorithm = this.algoType.newInstance();
         int height = db.getHeight();

         for (int row = 1; row < height; row++) {
            boolean matches;
            try {
               matches = fullfillsConditions(db, row, cdb);
            } catch (EvaluationException e) {
               return ErrorEval.VALUE_INVALID;
            }

            if (matches) {
               ValueEval currentValueEval = resolveReference(db, row, fc);
               boolean shouldContinue = algorithm.processMatch(currentValueEval);
               if (!shouldContinue) {
                  break;
               }
            }
         }

         return algorithm.getResult();
      } else {
         return ErrorEval.VALUE_INVALID;
      }
   }

   private static int getColumnForName(ValueEval nameValueEval, AreaEval db) throws EvaluationException {
      String name = OperandResolver.coerceValueToString(nameValueEval);
      return getColumnForString(db, name);
   }

   private static int getColumnForString(AreaEval db, String name) throws EvaluationException {
      int resultColumn = -1;
      int width = db.getWidth();

      for (int column = 0; column < width; column++) {
         ValueEval columnNameValueEval = resolveReference(db, 0, column);
         if (!(columnNameValueEval instanceof BlankEval) && !(columnNameValueEval instanceof ErrorEval)) {
            String columnName = OperandResolver.coerceValueToString(columnNameValueEval);
            if (name.equals(columnName)) {
               resultColumn = column;
               break;
            }
         }
      }

      return resultColumn;
   }

   private static boolean fullfillsConditions(AreaEval db, int row, AreaEval cdb) throws EvaluationException {
      int height = cdb.getHeight();

      for (int conditionRow = 1; conditionRow < height; conditionRow++) {
         boolean matches = true;
         int width = cdb.getWidth();

         for (int column = 0; column < width; column++) {
            boolean columnCondition = true;
            ValueEval condition = resolveReference(cdb, conditionRow, column);
            if (!(condition instanceof BlankEval)) {
               ValueEval targetHeader = resolveReference(cdb, 0, column);
               if (!(targetHeader instanceof StringValueEval)) {
                  throw new EvaluationException(ErrorEval.VALUE_INVALID);
               }

               if (getColumnForName(targetHeader, db) == -1) {
                  columnCondition = false;
               }

               if (!columnCondition) {
                  if (OperandResolver.coerceValueToString(condition).isEmpty()) {
                     throw new EvaluationException(ErrorEval.VALUE_INVALID);
                  }

                  throw new NotImplementedException("D* function with formula conditions");
               }

               ValueEval value = resolveReference(db, row, getColumnForName(targetHeader, db));
               if (!testNormalCondition(value, condition)) {
                  matches = false;
                  break;
               }
            }
         }

         if (matches) {
            return true;
         }
      }

      return false;
   }

   private static boolean testNormalCondition(ValueEval value, ValueEval condition) throws EvaluationException {
      if (condition instanceof StringEval) {
         String conditionString = ((StringEval)condition).getStringValue();
         if (conditionString.startsWith("<")) {
            String number = conditionString.substring(1);
            if (number.startsWith("=")) {
               number = number.substring(1);
               return testNumericCondition(value, DStarRunner.operator.smallerEqualThan, number);
            } else {
               return testNumericCondition(value, DStarRunner.operator.smallerThan, number);
            }
         } else if (conditionString.startsWith(">")) {
            String number = conditionString.substring(1);
            if (number.startsWith("=")) {
               number = number.substring(1);
               return testNumericCondition(value, DStarRunner.operator.largerEqualThan, number);
            } else {
               return testNumericCondition(value, DStarRunner.operator.largerThan, number);
            }
         } else if (conditionString.startsWith("=")) {
            String stringOrNumber = conditionString.substring(1);
            if (stringOrNumber.isEmpty()) {
               return value instanceof BlankEval;
            }

            boolean itsANumber;
            try {
               Integer.parseInt(stringOrNumber);
               itsANumber = true;
            } catch (NumberFormatException e) {
               try {
                  Double.parseDouble(stringOrNumber);
                  itsANumber = true;
               } catch (NumberFormatException e2) {
                  itsANumber = false;
               }
            }

            if (itsANumber) {
               return testNumericCondition(value, DStarRunner.operator.equal, stringOrNumber);
            }

            String valueString = value instanceof BlankEval ? "" : OperandResolver.coerceValueToString(value);
            return stringOrNumber.equals(valueString);
         } else {
            if (conditionString.isEmpty()) {
               return value instanceof StringEval;
            }

            String valueString = value instanceof BlankEval ? "" : OperandResolver.coerceValueToString(value);
            return valueString.startsWith(conditionString);
         }
      } else if (!(condition instanceof NumericValueEval)) {
         if (condition instanceof ErrorEval) {
            return value instanceof ErrorEval ? ((ErrorEval)condition).getErrorCode() == ((ErrorEval)value).getErrorCode() : false;
         } else {
            return false;
         }
      } else {
         double conditionNumber = ((NumericValueEval)condition).getNumberValue();
         Double valueNumber = getNumberFromValueEval(value);
         return valueNumber != null && conditionNumber == valueNumber;
      }
   }

   private static boolean testNumericCondition(ValueEval valueEval, DStarRunner.operator op, String condition) throws EvaluationException {
      if (!(valueEval instanceof NumericValueEval)) {
         return false;
      }

      double value = ((NumericValueEval)valueEval).getNumberValue();

      double conditionValue;
      try {
         conditionValue = Integer.parseInt(condition);
      } catch (NumberFormatException e) {
         try {
            conditionValue = Double.parseDouble(condition);
         } catch (NumberFormatException e2) {
            throw new EvaluationException(ErrorEval.VALUE_INVALID);
         }
      }

      int result = NumberComparer.compare(value, conditionValue);
      switch (op) {
         case largerThan:
            return result > 0;
         case largerEqualThan:
            return result >= 0;
         case smallerThan:
            return result < 0;
         case smallerEqualThan:
            return result <= 0;
         case equal:
            return result == 0;
         default:
            return false;
      }
   }

   private static Double getNumberFromValueEval(ValueEval value) {
      if (value instanceof NumericValueEval) {
         return ((NumericValueEval)value).getNumberValue();
      }

      if (value instanceof StringValueEval) {
         String stringValue = ((StringValueEval)value).getStringValue();

         try {
            return Double.parseDouble(stringValue);
         } catch (NumberFormatException e2) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static ValueEval resolveReference(AreaEval db, int dbRow, int dbCol) {
      try {
         return OperandResolver.getSingleValue(db.getValue(dbRow, dbCol), db.getFirstRow() + dbRow, db.getFirstColumn() + dbCol);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }
   }

   public enum DStarAlgorithmEnum {
      DGET(DGet::new),
      DMIN(DMin::new),
      DMAX(DMax::new),
      DSUM(DSum::new);

      private final Supplier<IDStarAlgorithm> implSupplier;

      DStarAlgorithmEnum(Supplier<IDStarAlgorithm> implSupplier) {
         this.implSupplier = implSupplier;
      }

      public IDStarAlgorithm newInstance() {
         return (IDStarAlgorithm)this.implSupplier.get();
      }
   }

   private enum operator {
      largerThan,
      largerEqualThan,
      smallerThan,
      smallerEqualThan,
      equal;
   }
}

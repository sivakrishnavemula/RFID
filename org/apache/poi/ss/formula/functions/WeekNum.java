package org.apache.poi.ss.formula.functions;

import java.util.Calendar;
import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.util.LocaleUtil;

public class WeekNum extends Fixed2ArgFunction implements FreeRefFunction {
   public static final FreeRefFunction instance = new WeekNum();

   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval serialNumVE, ValueEval returnTypeVE) {
      double serialNum;
      try {
         serialNum = NumericFunction.singleOperandEvaluate(serialNumVE, srcRowIndex, srcColumnIndex);
      } catch (EvaluationException e) {
         return ErrorEval.VALUE_INVALID;
      }

      Calendar serialNumCalendar = LocaleUtil.getLocaleCalendar();
      serialNumCalendar.setTime(DateUtil.getJavaDate(serialNum, false));

      int returnType;
      try {
         ValueEval ve = OperandResolver.getSingleValue(returnTypeVE, srcRowIndex, srcColumnIndex);
         returnType = OperandResolver.coerceValueToInt(ve);
      } catch (EvaluationException e) {
         return ErrorEval.NUM_ERROR;
      }

      return (ValueEval)(returnType != 1 && returnType != 2 ? ErrorEval.NUM_ERROR : new NumberEval(this.getWeekNo(serialNumCalendar, returnType)));
   }

   public int getWeekNo(Calendar cal, int weekStartOn) {
      if (weekStartOn == 1) {
         cal.setFirstDayOfWeek(1);
      } else {
         cal.setFirstDayOfWeek(2);
      }

      return cal.get(3);
   }

   public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
      return (ValueEval)(args.length == 2 ? this.evaluate(ec.getRowIndex(), ec.getColumnIndex(), args[0], args[1]) : ErrorEval.VALUE_INVALID);
   }
}

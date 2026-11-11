package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.StringEval;
import org.apache.poi.ss.formula.eval.ValueEval;

public final class Substitute extends Var3or4ArgFunction {
   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1, ValueEval arg2) {
      String result;
      try {
         String oldStr = TextFunction.evaluateStringArg(arg0, srcRowIndex, srcColumnIndex);
         String searchStr = TextFunction.evaluateStringArg(arg1, srcRowIndex, srcColumnIndex);
         String newStr = TextFunction.evaluateStringArg(arg2, srcRowIndex, srcColumnIndex);
         result = replaceAllOccurrences(oldStr, searchStr, newStr);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }

      return new StringEval(result);
   }

   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1, ValueEval arg2, ValueEval arg3) {
      String result;
      try {
         String oldStr = TextFunction.evaluateStringArg(arg0, srcRowIndex, srcColumnIndex);
         String searchStr = TextFunction.evaluateStringArg(arg1, srcRowIndex, srcColumnIndex);
         String newStr = TextFunction.evaluateStringArg(arg2, srcRowIndex, srcColumnIndex);
         int instanceNumber = TextFunction.evaluateIntArg(arg3, srcRowIndex, srcColumnIndex);
         if (instanceNumber < 1) {
            return ErrorEval.VALUE_INVALID;
         }

         result = replaceOneOccurrence(oldStr, searchStr, newStr, instanceNumber);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }

      return new StringEval(result);
   }

   private static String replaceAllOccurrences(String oldStr, String searchStr, String newStr) {
      StringBuffer sb = new StringBuffer();
      int startIndex = 0;
      int nextMatch = -1;

      while (true) {
         nextMatch = oldStr.indexOf(searchStr, startIndex);
         if (nextMatch < 0) {
            sb.append(oldStr.substring(startIndex));
            return sb.toString();
         }

         sb.append(oldStr.substring(startIndex, nextMatch));
         sb.append(newStr);
         startIndex = nextMatch + searchStr.length();
      }
   }

   private static String replaceOneOccurrence(String oldStr, String searchStr, String newStr, int instanceNumber) {
      if (searchStr.length() < 1) {
         return oldStr;
      }

      int startIndex = 0;
      int nextMatch = -1;
      int count = 0;

      while (true) {
         nextMatch = oldStr.indexOf(searchStr, startIndex);
         if (nextMatch < 0) {
            return oldStr;
         }

         if (++count == instanceNumber) {
            StringBuffer sb = new StringBuffer(oldStr.length() + newStr.length());
            sb.append(oldStr.substring(0, nextMatch));
            sb.append(newStr);
            sb.append(oldStr.substring(nextMatch + searchStr.length()));
            return sb.toString();
         }

         startIndex = nextMatch + searchStr.length();
      }
   }
}

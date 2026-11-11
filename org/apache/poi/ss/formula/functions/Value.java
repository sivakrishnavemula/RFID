package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.ValueEval;

public final class Value extends Fixed1ArgFunction {
   private static final int MIN_DISTANCE_BETWEEN_THOUSANDS_SEPARATOR = 4;
   private static final Double ZERO = 0.0;

   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0) {
      ValueEval veText;
      try {
         veText = OperandResolver.getSingleValue(arg0, srcRowIndex, srcColumnIndex);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }

      String strText = OperandResolver.coerceValueToString(veText);
      Double result = convertTextToNumber(strText);
      return (ValueEval)(result == null ? ErrorEval.VALUE_INVALID : new NumberEval(result));
   }

   private static Double convertTextToNumber(String strText) {
      boolean foundCurrency = false;
      boolean foundUnaryPlus = false;
      boolean foundUnaryMinus = false;
      boolean foundPercentage = false;
      int len = strText.length();

      int i;
      for (i = 0; i < len; i++) {
         char ch = strText.charAt(i);
         if (!Character.isDigit(ch) && ch != '.') {
            switch (ch) {
               case ' ':
                  continue;
               case '$':
                  if (foundCurrency) {
                     return null;
                  }

                  foundCurrency = true;
                  continue;
               case '+':
                  if (foundUnaryMinus || foundUnaryPlus) {
                     return null;
                  }

                  foundUnaryPlus = true;
                  continue;
               case '-':
                  if (foundUnaryMinus || foundUnaryPlus) {
                     return null;
                  }

                  foundUnaryMinus = true;
                  continue;
               default:
                  return null;
            }
         }
         break;
      }

      if (i < len) {
         boolean foundDecimalPoint = false;
         int lastThousandsSeparatorIndex = -32768;
         StringBuilder sb = new StringBuilder(len);

         while (i < len) {
            char ch = strText.charAt(i);
            if (Character.isDigit(ch)) {
               sb.append(ch);
            } else {
               switch (ch) {
                  case ' ':
                     String remainingTextTrimmed = strText.substring(i).trim();
                     if (remainingTextTrimmed.equals("%")) {
                        foundPercentage = true;
                     } else if (remainingTextTrimmed.length() > 0) {
                        return null;
                     }
                     break;
                  case '%':
                     foundPercentage = true;
                     break;
                  case ',':
                     if (foundDecimalPoint) {
                        return null;
                     }

                     int distanceBetweenThousandsSeparators = i - lastThousandsSeparatorIndex;
                     if (distanceBetweenThousandsSeparators < 4) {
                        return null;
                     }

                     lastThousandsSeparatorIndex = i;
                     break;
                  case '.':
                     if (foundDecimalPoint) {
                        return null;
                     }

                     if (i - lastThousandsSeparatorIndex < 4) {
                        return null;
                     }

                     foundDecimalPoint = true;
                     sb.append('.');
                     break;
                  case 'E':
                  case 'e':
                     if (i - lastThousandsSeparatorIndex < 4) {
                        return null;
                     }

                     sb.append(strText.substring(i));
                     i = len;
                     break;
                  default:
                     return null;
               }
            }

            i++;
         }

         if (!foundDecimalPoint && i - lastThousandsSeparatorIndex < 4) {
            return null;
         }

         double d;
         try {
            d = Double.parseDouble(sb.toString());
         } catch (NumberFormatException e) {
            return null;
         }

         double result = foundUnaryMinus ? -d : d;
         return foundPercentage ? result / 100.0 : result;
      } else {
         return !foundCurrency && !foundUnaryMinus && !foundUnaryPlus ? ZERO : null;
      }
   }
}

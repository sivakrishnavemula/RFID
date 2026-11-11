package org.apache.poi.ss.formula.functions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.StringEval;
import org.apache.poi.ss.formula.eval.ValueEval;

public class Imaginary extends Fixed1ArgFunction implements FreeRefFunction {
   public static final FreeRefFunction instance = new Imaginary();
   public static final String GROUP1_REAL_SIGN_REGEX = "([+-]?)";
   public static final String GROUP2_REAL_INTEGER_OR_DOUBLE_REGEX = "([0-9]+\\.[0-9]+|[0-9]*)";
   public static final String GROUP3_IMAGINARY_SIGN_REGEX = "([+-]?)";
   public static final String GROUP4_IMAGINARY_INTEGER_OR_DOUBLE_REGEX = "([0-9]+\\.[0-9]+|[0-9]*)";
   public static final String GROUP5_IMAGINARY_GROUP_REGEX = "([ij]?)";
   public static final Pattern COMPLEX_NUMBER_PATTERN = Pattern.compile("([+-]?)([0-9]+\\.[0-9]+|[0-9]*)([+-]?)([0-9]+\\.[0-9]+|[0-9]*)([ij]?)");
   public static final int GROUP1_REAL_SIGN = 1;
   public static final int GROUP2_IMAGINARY_INTEGER_OR_DOUBLE = 2;
   public static final int GROUP3_IMAGINARY_SIGN = 3;
   public static final int GROUP4_IMAGINARY_INTEGER_OR_DOUBLE = 4;

   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval inumberVE) {
      ValueEval veText1;
      try {
         veText1 = OperandResolver.getSingleValue(inumberVE, srcRowIndex, srcColumnIndex);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }

      String iNumber = OperandResolver.coerceValueToString(veText1);
      Matcher m = COMPLEX_NUMBER_PATTERN.matcher(iNumber);
      boolean result = m.matches();
      String imaginary = "";
      if (!result) {
         return ErrorEval.NUM_ERROR;
      }

      String imaginaryGroup = m.group(5);
      boolean hasImaginaryPart = imaginaryGroup.equals("i") || imaginaryGroup.equals("j");
      if (imaginaryGroup.length() == 0) {
         return new StringEval(String.valueOf(0));
      }

      if (hasImaginaryPart) {
         String sign = "";
         String imaginarySign = m.group(3);
         if (imaginarySign.length() != 0 && !imaginarySign.equals("+")) {
            sign = imaginarySign;
         }

         String groupImaginaryNumber = m.group(4);
         if (groupImaginaryNumber.length() != 0) {
            imaginary = sign + groupImaginaryNumber;
         } else {
            imaginary = sign + "1";
         }
      }

      return new StringEval(imaginary);
   }

   public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
      return (ValueEval)(args.length != 1 ? ErrorEval.VALUE_INVALID : this.evaluate(ec.getRowIndex(), ec.getColumnIndex(), args[0]));
   }
}

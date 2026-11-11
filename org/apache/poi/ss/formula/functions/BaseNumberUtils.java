package org.apache.poi.ss.formula.functions;

public class BaseNumberUtils {
   public static double convertToDecimal(String value, int base, int maxNumberOfPlaces) throws IllegalArgumentException {
      if (value != null && value.length() != 0) {
         long stringLength = value.length();
         if (stringLength > maxNumberOfPlaces) {
            throw new IllegalArgumentException();
         }

         double decimalValue = 0.0;
         long signedDigit = 0L;
         boolean hasSignedDigit = true;
         char[] characters = value.toCharArray();

         for (char character : characters) {
            long digit;
            if ('0' <= character && character <= '9') {
               digit = character - '0';
            } else if ('A' <= character && character <= 'Z') {
               digit = 10 + (character - 'A');
            } else if ('a' <= character && character <= 'z') {
               digit = 10 + (character - 'a');
            } else {
               digit = base;
            }

            if (digit >= base) {
               throw new IllegalArgumentException("character not allowed");
            }

            if (hasSignedDigit) {
               hasSignedDigit = false;
               signedDigit = digit;
            }

            decimalValue = decimalValue * base + digit;
         }

         boolean isNegative = !hasSignedDigit && stringLength == maxNumberOfPlaces && signedDigit >= base / 2;
         if (isNegative) {
            decimalValue = getTwoComplement(base, maxNumberOfPlaces, decimalValue);
            decimalValue *= -1.0;
         }

         return decimalValue;
      } else {
         return 0.0;
      }
   }

   private static double getTwoComplement(double base, double maxNumberOfPlaces, double decimalValue) {
      return Math.pow(base, maxNumberOfPlaces) - decimalValue;
   }
}

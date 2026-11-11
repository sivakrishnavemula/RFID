package org.apache.poi.ss.format;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import org.apache.poi.ss.format.CellFormatPart.PartHandler;
import org.apache.poi.ss.format.CellNumberFormatter.Special;
import org.apache.poi.util.Internal;

@Internal
public class CellNumberPartHandler implements PartHandler {
   private char insertSignForExponent;
   private double scale = 1.0;
   private Special decimalPoint;
   private Special slash;
   private Special exponent;
   private Special numerator;
   private final List<Special> specials = new LinkedList();
   private boolean improperFraction;

   public String handlePart(Matcher m, String part, CellFormatType type, StringBuffer descBuf) {
      int pos = descBuf.length();
      char firstCh = part.charAt(0);
      switch (firstCh) {
         case '#':
         case '0':
         case '?':
            if (this.insertSignForExponent != 0) {
               this.specials.add(new Special(this.insertSignForExponent, pos));
               descBuf.append(this.insertSignForExponent);
               this.insertSignForExponent = 0;
               pos++;
            }

            for (int i = 0; i < part.length(); i++) {
               char ch = part.charAt(i);
               this.specials.add(new Special(ch, pos + i));
            }
            break;
         case '%':
            this.scale *= 100.0;
            break;
         case '.':
            if (this.decimalPoint == null && this.specials.size() > 0) {
               this.decimalPoint = new Special('.', pos);
               this.specials.add(this.decimalPoint);
            }
            break;
         case '/':
            if (this.slash == null && this.specials.size() > 0) {
               this.numerator = this.previousNumber();
               this.improperFraction = this.improperFraction | this.numerator == firstDigit(this.specials);
               this.slash = new Special('.', pos);
               this.specials.add(this.slash);
            }
            break;
         case 'E':
         case 'e':
            if (this.exponent == null && this.specials.size() > 0) {
               this.exponent = new Special('.', pos);
               this.specials.add(this.exponent);
               this.insertSignForExponent = part.charAt(1);
               return part.substring(0, 1);
            }
            break;
         default:
            return null;
      }

      return part;
   }

   public double getScale() {
      return this.scale;
   }

   public Special getDecimalPoint() {
      return this.decimalPoint;
   }

   public Special getSlash() {
      return this.slash;
   }

   public Special getExponent() {
      return this.exponent;
   }

   public Special getNumerator() {
      return this.numerator;
   }

   public List<Special> getSpecials() {
      return this.specials;
   }

   public boolean isImproperFraction() {
      return this.improperFraction;
   }

   private Special previousNumber() {
      ListIterator<Special> it = this.specials.listIterator(this.specials.size());

      while (it.hasPrevious()) {
         Special s = (Special)it.previous();
         if (isDigitFmt(s)) {
            Special last = s;

            while (it.hasPrevious()) {
               s = (Special)it.previous();
               if (last.pos - s.pos > 1 || !isDigitFmt(s)) {
                  break;
               }

               last = s;
            }

            return last;
         }
      }

      return null;
   }

   private static boolean isDigitFmt(Special s) {
      return s.ch == '0' || s.ch == '?' || s.ch == '#';
   }

   private static Special firstDigit(List<Special> specials) {
      for (Special s : specials) {
         if (isDigitFmt(s)) {
            return s;
         }
      }

      return null;
   }
}

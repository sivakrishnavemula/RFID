package org.apache.poi.hssf.usermodel;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

public class FontDetails {
   private String _fontName;
   private int _height;
   private final Map<Character, Integer> charWidths = new HashMap();

   public FontDetails(String fontName, int height) {
      this._fontName = fontName;
      this._height = height;
   }

   public String getFontName() {
      return this._fontName;
   }

   public int getHeight() {
      return this._height;
   }

   public void addChar(char c, int width) {
      this.charWidths.put(c, width);
   }

   public int getCharWidth(char c) {
      Integer widthInteger = (Integer)this.charWidths.get(c);
      if (widthInteger == null) {
         return 87 == c ? 0 : this.getCharWidth((char)87);
      } else {
         return widthInteger;
      }
   }

   public void addChars(char[] characters, int[] widths) {
      for (int i = 0; i < characters.length; i++) {
         this.charWidths.put(characters[i], widths[i]);
      }
   }

   protected static String buildFontHeightProperty(String fontName) {
      return "font." + fontName + ".height";
   }

   protected static String buildFontWidthsProperty(String fontName) {
      return "font." + fontName + ".widths";
   }

   protected static String buildFontCharactersProperty(String fontName) {
      return "font." + fontName + ".characters";
   }

   public static FontDetails create(String fontName, Properties fontMetricsProps) {
      String heightStr = fontMetricsProps.getProperty(buildFontHeightProperty(fontName));
      String widthsStr = fontMetricsProps.getProperty(buildFontWidthsProperty(fontName));
      String charactersStr = fontMetricsProps.getProperty(buildFontCharactersProperty(fontName));
      if (heightStr != null && widthsStr != null && charactersStr != null) {
         int height = Integer.parseInt(heightStr);
         FontDetails d = new FontDetails(fontName, height);
         String[] charactersStrArray = split(charactersStr, ",", -1);
         String[] widthsStrArray = split(widthsStr, ",", -1);
         if (charactersStrArray.length != widthsStrArray.length) {
            throw new RuntimeException("Number of characters does not number of widths for font " + fontName);
         }

         for (int i = 0; i < widthsStrArray.length; i++) {
            if (charactersStrArray[i].length() != 0) {
               d.addChar(charactersStrArray[i].charAt(0), Integer.parseInt(widthsStrArray[i]));
            }
         }

         return d;
      } else {
         throw new IllegalArgumentException(
            "The supplied FontMetrics doesn't know about the font '"
               + fontName
               + "', so we can't use it. Please add it to your font metrics file (see StaticFontMetrics.getFontDetails"
         );
      }
   }

   public int getStringWidth(String str) {
      int width = 0;

      for (int i = 0; i < str.length(); i++) {
         width += this.getCharWidth(str.charAt(i));
      }

      return width;
   }

   private static String[] split(String text, String separator, int max) {
      StringTokenizer tok = new StringTokenizer(text, separator);
      int listSize = tok.countTokens();
      if (max != -1 && listSize > max) {
         listSize = max;
      }

      String[] list = new String[listSize];

      for (int i = 0; tok.hasMoreTokens(); i++) {
         if (max != -1 && i == listSize - 1) {
            StringBuffer buf = new StringBuffer(text.length() * (listSize - i) / listSize);

            while (tok.hasMoreTokens()) {
               buf.append(tok.nextToken());
               if (tok.hasMoreTokens()) {
                  buf.append(separator);
               }
            }

            list[i] = buf.toString().trim();
            break;
         }

         list[i] = tok.nextToken().trim();
      }

      return list;
   }
}

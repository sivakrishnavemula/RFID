package org.apache.poi.sl.draw;

import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Map;
import org.apache.poi.common.usermodel.fonts.FontInfo;
import org.apache.poi.sl.draw.Drawable.DrawableHint;

public class DrawFontManagerDefault implements DrawFontManager {
   public FontInfo getMappedFont(Graphics2D graphics, FontInfo fontInfo) {
      return this.getFontWithFallback(graphics, Drawable.FONT_MAP, fontInfo);
   }

   public FontInfo getFallbackFont(Graphics2D graphics, FontInfo fontInfo) {
      FontInfo fi = this.getFontWithFallback(graphics, Drawable.FONT_FALLBACK, fontInfo);
      if (fi == null) {
         fi = new DrawFontInfo("SansSerif");
      }

      return fi;
   }

   public String mapFontCharset(Graphics2D graphics, FontInfo fontInfo, String text) {
      String attStr = text;
      if (fontInfo != null && "Wingdings".equalsIgnoreCase(fontInfo.getTypeface())) {
         boolean changed = false;
         char[] chrs = attStr.toCharArray();

         for (int i = 0; i < chrs.length; i++) {
            if (' ' <= chrs[i] && chrs[i] <= 127 || 160 <= chrs[i] && chrs[i] <= 255) {
               chrs[i] = (char)(chrs[i] | 61440);
               changed = true;
            }
         }

         if (changed) {
            attStr = new String(chrs);
         }
      }

      return attStr;
   }

   public Font createAWTFont(Graphics2D graphics, FontInfo fontInfo, double fontSize, boolean bold, boolean italic) {
      int style = (bold ? 1 : 0) | (italic ? 2 : 0);
      Font font = new Font(fontInfo.getTypeface(), style, 12);
      if ("Dialog".equals(font.getFamily())) {
         font = new Font("SansSerif", style, 12);
      }

      return font.deriveFont((float)fontSize);
   }

   private FontInfo getFontWithFallback(Graphics2D graphics, DrawableHint hint, FontInfo fontInfo) {
      Map<String, String> fontMap = (Map<String, String>)graphics.getRenderingHint(hint);
      if (fontMap == null) {
         return fontInfo;
      }

      String f = fontInfo != null ? fontInfo.getTypeface() : null;
      String mappedTypeface = null;
      if (fontMap.containsKey(f)) {
         mappedTypeface = (String)fontMap.get(f);
      } else if (fontMap.containsKey("*")) {
         mappedTypeface = (String)fontMap.get("*");
      }

      return (FontInfo)(mappedTypeface != null ? new DrawFontInfo(mappedTypeface) : fontInfo);
   }
}

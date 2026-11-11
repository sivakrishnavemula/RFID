package org.apache.poi.sl.usermodel;

import java.awt.Color;
import org.apache.poi.common.usermodel.fonts.FontGroup;
import org.apache.poi.common.usermodel.fonts.FontInfo;
import org.apache.poi.util.Internal;

public interface TextRun {
   String getRawText();

   void setText(String var1);

   TextRun.TextCap getTextCap();

   PaintStyle getFontColor();

   void setFontColor(Color var1);

   void setFontColor(PaintStyle var1);

   Double getFontSize();

   void setFontSize(Double var1);

   String getFontFamily();

   String getFontFamily(FontGroup var1);

   void setFontFamily(String var1);

   void setFontFamily(String var1, FontGroup var2);

   FontInfo getFontInfo(FontGroup var1);

   void setFontInfo(FontInfo var1, FontGroup var2);

   boolean isBold();

   void setBold(boolean var1);

   boolean isItalic();

   void setItalic(boolean var1);

   boolean isUnderlined();

   void setUnderlined(boolean var1);

   boolean isStrikethrough();

   void setStrikethrough(boolean var1);

   boolean isSubscript();

   boolean isSuperscript();

   byte getPitchAndFamily();

   Hyperlink<?, ?> getHyperlink();

   Hyperlink<?, ?> createHyperlink();

   @Internal
   TextRun.FieldType getFieldType();

   enum FieldType {
      SLIDE_NUMBER,
      DATE_TIME;
   }

   enum TextCap {
      NONE,
      SMALL,
      ALL;
   }
}

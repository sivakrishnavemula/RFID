package org.apache.poi.sl.usermodel;

import java.awt.Color;
import java.util.List;
import org.apache.poi.sl.usermodel.TabStop.TabStopType;

public interface TextParagraph<S extends Shape<S, P>, P extends TextParagraph<S, P, T>, T extends TextRun> extends Iterable<T> {
   Double getSpaceBefore();

   void setSpaceBefore(Double var1);

   Double getSpaceAfter();

   void setSpaceAfter(Double var1);

   Double getLeftMargin();

   void setLeftMargin(Double var1);

   Double getRightMargin();

   void setRightMargin(Double var1);

   Double getIndent();

   void setIndent(Double var1);

   int getIndentLevel();

   void setIndentLevel(int var1);

   Double getLineSpacing();

   void setLineSpacing(Double var1);

   String getDefaultFontFamily();

   Double getDefaultFontSize();

   TextParagraph.TextAlign getTextAlign();

   void setTextAlign(TextParagraph.TextAlign var1);

   TextParagraph.FontAlign getFontAlign();

   TextParagraph.BulletStyle getBulletStyle();

   void setBulletStyle(Object... var1);

   Double getDefaultTabSize();

   TextShape<S, P> getParentShape();

   List<T> getTextRuns();

   boolean isHeaderOrFooter();

   List<? extends TabStop> getTabStops();

   void addTabStops(double var1, TabStopType var3);

   void clearTabStops();

   interface BulletStyle {
      String getBulletCharacter();

      String getBulletFont();

      Double getBulletFontSize();

      void setBulletFontColor(Color var1);

      void setBulletFontColor(PaintStyle var1);

      PaintStyle getBulletFontColor();

      AutoNumberingScheme getAutoNumberingScheme();

      Integer getAutoNumberingStartAt();
   }

   enum FontAlign {
      AUTO,
      TOP,
      CENTER,
      BASELINE,
      BOTTOM;
   }

   enum TextAlign {
      LEFT,
      CENTER,
      RIGHT,
      JUSTIFY,
      JUSTIFY_LOW,
      DIST,
      THAI_DIST;
   }
}

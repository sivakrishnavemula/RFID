package org.apache.poi.sl.usermodel;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

public interface TextShape<S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> extends SimpleShape<S, P>, Iterable<P> {
   String getText();

   TextRun setText(String var1);

   TextRun appendText(String var1, boolean var2);

   List<P> getTextParagraphs();

   Insets2D getInsets();

   void setInsets(Insets2D var1);

   double getTextHeight();

   double getTextHeight(Graphics2D var1);

   VerticalAlignment getVerticalAlignment();

   void setVerticalAlignment(VerticalAlignment var1);

   boolean isHorizontalCentered();

   void setHorizontalCentered(Boolean var1);

   boolean getWordWrap();

   void setWordWrap(boolean var1);

   TextShape.TextDirection getTextDirection();

   void setTextDirection(TextShape.TextDirection var1);

   Double getTextRotation();

   void setTextRotation(Double var1);

   void setTextPlaceholder(TextShape.TextPlaceholder var1);

   TextShape.TextPlaceholder getTextPlaceholder();

   Rectangle2D resizeToFitText();

   Rectangle2D resizeToFitText(Graphics2D var1);

   enum TextAutofit {
      NONE,
      NORMAL,
      SHAPE;
   }

   enum TextDirection {
      HORIZONTAL,
      VERTICAL,
      VERTICAL_270,
      STACKED;
   }

   enum TextPlaceholder {
      TITLE,
      BODY,
      CENTER_TITLE,
      CENTER_BODY,
      HALF_BODY,
      QUARTER_BODY,
      NOTES,
      OTHER;
   }
}

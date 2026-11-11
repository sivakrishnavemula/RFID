package org.apache.poi.sl.usermodel;

import java.awt.Color;
import org.apache.poi.sl.usermodel.StrokeStyle.LineCompound;
import org.apache.poi.sl.usermodel.StrokeStyle.LineDash;

public interface TableCell<S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> extends TextShape<S, P> {
   StrokeStyle getBorderStyle(TableCell.BorderEdge var1);

   void setBorderStyle(TableCell.BorderEdge var1, StrokeStyle var2);

   void setBorderWidth(TableCell.BorderEdge var1, double var2);

   void setBorderColor(TableCell.BorderEdge var1, Color var2);

   void setBorderCompound(TableCell.BorderEdge var1, LineCompound var2);

   void setBorderDash(TableCell.BorderEdge var1, LineDash var2);

   void removeBorder(TableCell.BorderEdge var1);

   int getGridSpan();

   int getRowSpan();

   boolean isMerged();

   enum BorderEdge {
      bottom,
      left,
      top,
      right;
   }
}

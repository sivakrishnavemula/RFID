package org.apache.poi.sl.usermodel;

import java.awt.Color;
import org.apache.poi.sl.draw.geom.CustomGeometry;
import org.apache.poi.sl.draw.geom.IAdjustableShape;

public interface SimpleShape<S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>>
   extends Shape<S, P>,
   IAdjustableShape,
   PlaceableShape<S, P> {
   FillStyle getFillStyle();

   LineDecoration getLineDecoration();

   StrokeStyle getStrokeStyle();

   void setStrokeStyle(Object... var1);

   CustomGeometry getGeometry();

   ShapeType getShapeType();

   void setShapeType(ShapeType var1);

   Placeholder getPlaceholder();

   void setPlaceholder(Placeholder var1);

   PlaceholderDetails getPlaceholderDetails();

   boolean isPlaceholder();

   Shadow<S, P> getShadow();

   Color getFillColor();

   void setFillColor(Color var1);

   Hyperlink<S, P> getHyperlink();

   Hyperlink<S, P> createHyperlink();
}

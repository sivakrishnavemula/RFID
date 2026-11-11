package org.apache.poi.sl.usermodel;

import java.awt.geom.Rectangle2D;

public interface PlaceableShape<S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> {
   ShapeContainer<S, P> getParent();

   Sheet<S, P> getSheet();

   Rectangle2D getAnchor();

   void setAnchor(Rectangle2D var1);

   double getRotation();

   void setRotation(double var1);

   void setFlipHorizontal(boolean var1);

   void setFlipVertical(boolean var1);

   boolean getFlipHorizontal();

   boolean getFlipVertical();
}

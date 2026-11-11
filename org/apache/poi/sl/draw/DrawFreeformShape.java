package org.apache.poi.sl.draw;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.poi.sl.draw.geom.Outline;
import org.apache.poi.sl.draw.geom.Path;
import org.apache.poi.sl.usermodel.FillStyle;
import org.apache.poi.sl.usermodel.FreeformShape;
import org.apache.poi.sl.usermodel.StrokeStyle;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.sl.usermodel.TextRun;
import org.apache.poi.sl.usermodel.TextShape;

public class DrawFreeformShape extends DrawAutoShape {
   public DrawFreeformShape(FreeformShape<?, ?> shape) {
      super(shape);
   }

   protected Collection<Outline> computeOutlines(Graphics2D graphics) {
      List<Outline> lst = new ArrayList();
      FreeformShape<?, ?> fsh = (FreeformShape<?, ?>)this.getShape();
      Path2D sh = fsh.getPath();
      AffineTransform tx = (AffineTransform)graphics.getRenderingHint(Drawable.GROUP_TRANSFORM);
      if (tx == null) {
         tx = new AffineTransform();
      }

      Shape canvasShape = tx.createTransformedShape(sh);
      FillStyle fs = fsh.getFillStyle();
      StrokeStyle ss = fsh.getStrokeStyle();
      Path path = new Path(fs != null, ss != null);
      lst.add(new Outline(canvasShape, path));
      return lst;
   }

   protected TextShape<?, ? extends TextParagraph<?, ?, ? extends TextRun>> getShape() {
      return (TextShape<?, ? extends TextParagraph<?, ?, ? extends TextRun>>)this.shape;
   }
}

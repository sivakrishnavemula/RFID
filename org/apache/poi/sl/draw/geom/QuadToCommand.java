package org.apache.poi.sl.draw.geom;

import java.awt.geom.Path2D.Double;
import org.apache.poi.sl.draw.binding.CTAdjPoint2D;

public class QuadToCommand implements PathCommand {
   private String arg1;
   private String arg2;
   private String arg3;
   private String arg4;

   QuadToCommand(CTAdjPoint2D pt1, CTAdjPoint2D pt2) {
      this.arg1 = pt1.getX();
      this.arg2 = pt1.getY();
      this.arg3 = pt2.getX();
      this.arg4 = pt2.getY();
   }

   public void execute(Double path, Context ctx) {
      double x1 = ctx.getValue(this.arg1);
      double y1 = ctx.getValue(this.arg2);
      double x2 = ctx.getValue(this.arg3);
      double y2 = ctx.getValue(this.arg4);
      path.quadTo(x1, y1, x2, y2);
   }
}

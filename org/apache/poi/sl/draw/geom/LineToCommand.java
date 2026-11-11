package org.apache.poi.sl.draw.geom;

import java.awt.geom.Path2D.Double;
import org.apache.poi.sl.draw.binding.CTAdjPoint2D;

public class LineToCommand implements PathCommand {
   private String arg1;
   private String arg2;

   LineToCommand(CTAdjPoint2D pt) {
      this.arg1 = pt.getX();
      this.arg2 = pt.getY();
   }

   LineToCommand(String s1, String s2) {
      this.arg1 = s1;
      this.arg2 = s2;
   }

   public void execute(Double path, Context ctx) {
      double x = ctx.getValue(this.arg1);
      double y = ctx.getValue(this.arg2);
      path.lineTo(x, y);
   }
}

package org.apache.poi.sl.draw.geom;

import org.apache.poi.sl.draw.binding.CTGeomGuide;

public class AdjustValue extends Guide {
   public AdjustValue(CTGeomGuide gd) {
      super(gd.getName(), gd.getFmla());
   }

   public double evaluate(Context ctx) {
      String name = this.getName();
      Guide adj = ctx.getAdjustValue(name);
      return adj != null ? adj.evaluate(ctx) : super.evaluate(ctx);
   }
}

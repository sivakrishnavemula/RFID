package org.apache.poi.sl.draw.geom;

import org.apache.poi.sl.draw.binding.CTGeomGuide;

public class Guide implements Formula {
   private final String name;
   private final String fmla;
   private final Guide.Op op;
   private final String[] operands;

   public Guide(CTGeomGuide gd) {
      this(gd.getName(), gd.getFmla());
   }

   public Guide(String nm, String fm) {
      this.name = nm;
      this.fmla = fm;
      this.operands = fm.split("\\s+");
      this.op = Guide.Op.valueOf(this.operands[0].replace("*", "mul").replace("/", "div").replace("+", "add").replace("-", "sub").replace("?:", "ifelse"));
   }

   public String getName() {
      return this.name;
   }

   String getFormula() {
      return this.fmla;
   }

   public double evaluate(Context ctx) {
      double x = this.operands.length > 1 ? ctx.getValue(this.operands[1]) : 0.0;
      double y = this.operands.length > 2 ? ctx.getValue(this.operands[2]) : 0.0;
      double z = this.operands.length > 3 ? ctx.getValue(this.operands[3]) : 0.0;
      switch (this.op) {
         case abs:
            return Math.abs(x);
         case adddiv:
            return (x + y) / z;
         case addsub:
            return x + y - z;
         case at2:
            return Math.toDegrees(Math.atan2(y, x)) * 60000.0;
         case cos:
            return x * Math.cos(Math.toRadians(y / 60000.0));
         case cat2:
            return x * Math.cos(Math.atan2(z, y));
         case ifelse:
            return x > 0.0 ? y : z;
         case val:
            return x;
         case max:
            return Math.max(x, y);
         case min:
            return Math.min(x, y);
         case mod:
            return Math.sqrt(x * x + y * y + z * z);
         case muldiv:
            return x * y / z;
         case pin:
            if (y < x) {
               return x;
            } else {
               if (y > z) {
                  return z;
               }

               return y;
            }
         case sat2:
            return x * Math.sin(Math.atan2(z, y));
         case sin:
            return x * Math.sin(Math.toRadians(y / 60000.0));
         case sqrt:
            return Math.sqrt(x);
         case tan:
            return x * Math.tan(Math.toRadians(y / 60000.0));
         default:
            return 0.0;
      }
   }

   enum Op {
      muldiv,
      addsub,
      adddiv,
      ifelse,
      val,
      abs,
      sqrt,
      max,
      min,
      at2,
      sin,
      cos,
      tan,
      cat2,
      sat2,
      pin,
      mod;
   }
}

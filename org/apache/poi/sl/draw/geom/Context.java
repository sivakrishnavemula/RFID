package org.apache.poi.sl.draw.geom;

import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

public class Context {
   final Map<String, Double> _ctx = new HashMap();
   final IAdjustableShape _props;
   final Rectangle2D _anchor;

   public Context(CustomGeometry geom, Rectangle2D anchor, IAdjustableShape props) {
      this._props = props;
      this._anchor = anchor;

      for (Guide gd : geom.adjusts) {
         this.evaluate(gd);
      }

      for (Guide gd : geom.guides) {
         this.evaluate(gd);
      }
   }

   public Rectangle2D getShapeAnchor() {
      return this._anchor;
   }

   public Guide getAdjustValue(String name) {
      return this._props.getClass().getName().contains("hslf") ? null : this._props.getAdjustValue(name);
   }

   public double getValue(String key) {
      if (key.matches("(\\+|-)?\\d+")) {
         return Double.parseDouble(key);
      }

      Double val = (Double)this._ctx.get(key);
      return val != null ? val : this.evaluate(BuiltInGuide.valueOf("_" + key));
   }

   public double evaluate(Formula fmla) {
      double result = fmla.evaluate(this);
      if (fmla instanceof Guide) {
         String key = ((Guide)fmla).getName();
         if (key != null) {
            this._ctx.put(key, result);
         }
      }

      return result;
   }
}

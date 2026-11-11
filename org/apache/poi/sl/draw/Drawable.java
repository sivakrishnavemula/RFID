package org.apache.poi.sl.draw;

import java.awt.Graphics2D;
import java.awt.RenderingHints.Key;
import org.apache.poi.util.Internal;

public interface Drawable {
   Drawable.DrawableHint DRAW_FACTORY = new Drawable.DrawableHint(1);
   @Internal
   Drawable.DrawableHint GROUP_TRANSFORM = new Drawable.DrawableHint(2);
   Drawable.DrawableHint IMAGE_RENDERER = new Drawable.DrawableHint(3);
   Drawable.DrawableHint TEXT_RENDERING_MODE = new Drawable.DrawableHint(4);
   Drawable.DrawableHint GRADIENT_SHAPE = new Drawable.DrawableHint(5);
   Drawable.DrawableHint PRESET_GEOMETRY_CACHE = new Drawable.DrawableHint(6);
   int TEXT_AS_CHARACTERS = 1;
   int TEXT_AS_SHAPES = 2;
   Drawable.DrawableHint FONT_HANDLER = new Drawable.DrawableHint(7);
   Drawable.DrawableHint FONT_FALLBACK = new Drawable.DrawableHint(8);
   Drawable.DrawableHint FONT_MAP = new Drawable.DrawableHint(9);
   Drawable.DrawableHint GSAVE = new Drawable.DrawableHint(10);
   Drawable.DrawableHint GRESTORE = new Drawable.DrawableHint(11);
   Drawable.DrawableHint CURRENT_SLIDE = new Drawable.DrawableHint(12);

   void applyTransform(Graphics2D var1);

   void draw(Graphics2D var1);

   void drawContent(Graphics2D var1);

   class DrawableHint extends Key {
      protected DrawableHint(int id) {
         super(id);
      }

      public boolean isCompatibleValue(Object val) {
         return true;
      }

      public String toString() {
         switch (this.intKey()) {
            case 1:
               return "DRAW_FACTORY";
            case 2:
               return "GROUP_TRANSFORM";
            case 3:
               return "IMAGE_RENDERER";
            case 4:
               return "TEXT_RENDERING_MODE";
            case 5:
               return "GRADIENT_SHAPE";
            case 6:
               return "PRESET_GEOMETRY_CACHE";
            case 7:
               return "FONT_HANDLER";
            case 8:
               return "FONT_FALLBACK";
            case 9:
               return "FONT_MAP";
            case 10:
               return "GSAVE";
            case 11:
               return "GRESTORE";
            default:
               return "UNKNOWN_ID " + this.intKey();
         }
      }
   }
}

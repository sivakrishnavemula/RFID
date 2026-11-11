package org.apache.poi.sl.draw;

import java.awt.Graphics2D;
import org.apache.poi.sl.usermodel.MasterSheet;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.sl.usermodel.Shape;
import org.apache.poi.sl.usermodel.SimpleShape;
import org.apache.poi.sl.usermodel.Slide;

public class DrawMasterSheet extends DrawSheet {
   public DrawMasterSheet(MasterSheet<?, ?> sheet) {
      super(sheet);
   }

   protected boolean canDraw(Graphics2D graphics, Shape<?, ?> shape) {
      Slide<?, ?> slide = (Slide<?, ?>)graphics.getRenderingHint(Drawable.CURRENT_SLIDE);
      if (shape instanceof SimpleShape) {
         Placeholder ph = ((SimpleShape)shape).getPlaceholder();
         if (ph != null) {
            return slide.getDisplayPlaceholder(ph);
         }
      }

      return slide.getFollowMasterGraphics();
   }
}

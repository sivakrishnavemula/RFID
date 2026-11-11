package org.apache.poi.sl.draw;

import java.awt.Graphics2D;
import org.apache.poi.sl.usermodel.Background;
import org.apache.poi.sl.usermodel.Slide;

public class DrawSlide extends DrawSheet {
   public DrawSlide(Slide<?, ?> slide) {
      super(slide);
   }

   public void draw(Graphics2D graphics) {
      graphics.setRenderingHint(Drawable.CURRENT_SLIDE, this.sheet);
      Background<?, ?> bg = this.sheet.getBackground();
      if (bg != null) {
         DrawFactory drawFact = DrawFactory.getInstance(graphics);
         Drawable db = drawFact.getDrawable(bg);
         db.draw(graphics);
      }

      super.draw(graphics);
      graphics.setRenderingHint(Drawable.CURRENT_SLIDE, null);
   }
}

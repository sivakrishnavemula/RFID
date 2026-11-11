package org.apache.poi.sl.usermodel;

import java.awt.Graphics2D;

public interface Sheet<S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> extends ShapeContainer<S, P> {
   SlideShow<S, P> getSlideShow();

   boolean getFollowMasterGraphics();

   MasterSheet<S, P> getMasterSheet();

   Background<S, P> getBackground();

   void draw(Graphics2D var1);

   PlaceholderDetails getPlaceholderDetails(Placeholder var1);
}

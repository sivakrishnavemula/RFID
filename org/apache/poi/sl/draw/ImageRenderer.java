package org.apache.poi.sl.draw;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public interface ImageRenderer {
   void loadImage(InputStream var1, String var2) throws IOException;

   void loadImage(byte[] var1, String var2) throws IOException;

   Dimension getDimension();

   void setAlpha(double var1);

   BufferedImage getImage();

   BufferedImage getImage(Dimension var1);

   boolean drawImage(Graphics2D var1, Rectangle2D var2);

   boolean drawImage(Graphics2D var1, Rectangle2D var2, Insets var3);
}

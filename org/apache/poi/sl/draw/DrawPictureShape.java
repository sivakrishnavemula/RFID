package org.apache.poi.sl.draw;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.io.IOException;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.PictureShape;
import org.apache.poi.sl.usermodel.RectAlign;
import org.apache.poi.sl.usermodel.PictureData.PictureType;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

public class DrawPictureShape extends DrawSimpleShape {
   private static final POILogger LOG = POILogFactory.getLogger(DrawPictureShape.class);
   private static final String WMF_IMAGE_RENDERER = "org.apache.poi.hwmf.draw.HwmfSLImageRenderer";

   public DrawPictureShape(PictureShape<?, ?> shape) {
      super(shape);
   }

   public void drawContent(Graphics2D graphics) {
      PictureData data = this.getShape().getPictureData();
      if (data != null) {
         Rectangle2D anchor = getAnchor(graphics, this.getShape());
         Insets insets = this.getShape().getClipping();

         try {
            ImageRenderer renderer = getImageRenderer(graphics, data.getContentType());
            renderer.loadImage(data.getData(), data.getContentType());
            renderer.drawImage(graphics, anchor, insets);
         } catch (IOException e) {
            LOG.log(7, new Object[]{"image can't be loaded/rendered.", e});
         }
      }
   }

   public static ImageRenderer getImageRenderer(Graphics2D graphics, String contentType) {
      ImageRenderer renderer = (ImageRenderer)graphics.getRenderingHint(Drawable.IMAGE_RENDERER);
      if (renderer != null) {
         return renderer;
      }

      if (PictureType.WMF.contentType.equals(contentType)) {
         try {
            Class<? extends ImageRenderer> irc = DrawPictureShape.class.getClassLoader().loadClass("org.apache.poi.hwmf.draw.HwmfSLImageRenderer");
            return (ImageRenderer)irc.newInstance();
         } catch (Exception e) {
            LOG.log(7, new Object[]{"WMF image renderer is not on the classpath - include poi-scratchpad jar!", e});
         }
      }

      return new BitmapImageRenderer();
   }

   protected PictureShape<?, ?> getShape() {
      return (PictureShape<?, ?>)this.shape;
   }

   public void resize() {
      PictureShape<?, ?> ps = this.getShape();
      Dimension dim = ps.getPictureData().getImageDimension();
      Rectangle2D origRect = ps.getAnchor();
      double x = origRect.getX();
      double y = origRect.getY();
      double w = dim.getWidth();
      double h = dim.getHeight();
      ps.setAnchor(new Double(x, y, w, h));
   }

   public void resize(Rectangle2D target) {
      this.resize(target, RectAlign.CENTER);
   }

   public void resize(Rectangle2D target, RectAlign align) {
      PictureShape<?, ?> ps = this.getShape();
      Dimension dim = ps.getPictureData().getImageDimension();
      if (dim.width > 0 && dim.height > 0) {
         double w = target.getWidth();
         double h = target.getHeight();
         double sx = w / dim.width;
         double sy = h / dim.height;
         double dx = 0.0;
         double dy = 0.0;
         if (sx > sy) {
            w = sy * dim.width;
            dx = target.getWidth() - w;
         } else {
            if (!(sy > sx)) {
               ps.setAnchor(target);
               return;
            }

            h = sx * dim.height;
            dy = target.getHeight() - h;
         }

         double x = target.getX();
         double y = target.getY();
         switch (align) {
            case TOP:
               x += dx / 2.0;
               break;
            case TOP_RIGHT:
               x += dx;
               break;
            case RIGHT:
               x += dx;
               y += dy / 2.0;
               break;
            case BOTTOM_RIGHT:
               x += dx;
               y += dy;
               break;
            case BOTTOM:
               x += dx / 2.0;
               y += dy;
               break;
            case BOTTOM_LEFT:
               y += dy;
               break;
            case LEFT:
               y += dy / 2.0;
            case TOP_LEFT:
               break;
            default:
               x += dx / 2.0;
               y += dy / 2.0;
         }

         ps.setAnchor(new Double(x, y, w, h));
      } else {
         ps.setAnchor(target);
      }
   }
}

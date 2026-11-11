package org.apache.poi.sl.draw;

import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import java.util.HashMap;
import java.util.Map;
import org.apache.poi.sl.usermodel.Background;
import org.apache.poi.sl.usermodel.ConnectorShape;
import org.apache.poi.sl.usermodel.FreeformShape;
import org.apache.poi.sl.usermodel.GraphicalFrame;
import org.apache.poi.sl.usermodel.GroupShape;
import org.apache.poi.sl.usermodel.MasterSheet;
import org.apache.poi.sl.usermodel.PictureShape;
import org.apache.poi.sl.usermodel.PlaceableShape;
import org.apache.poi.sl.usermodel.Shape;
import org.apache.poi.sl.usermodel.Sheet;
import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.sl.usermodel.TableShape;
import org.apache.poi.sl.usermodel.TextBox;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.sl.usermodel.TextShape;
import org.apache.poi.util.JvmBugs;

public class DrawFactory {
   protected static final ThreadLocal<DrawFactory> defaultFactory = new ThreadLocal();

   public static void setDefaultFactory(DrawFactory factory) {
      defaultFactory.set(factory);
   }

   public static DrawFactory getInstance(Graphics2D graphics) {
      DrawFactory factory = null;
      boolean isHint = false;
      if (graphics != null) {
         factory = (DrawFactory)graphics.getRenderingHint(Drawable.DRAW_FACTORY);
         isHint = factory != null;
      }

      if (factory == null) {
         factory = (DrawFactory)defaultFactory.get();
      }

      if (factory == null) {
         factory = new DrawFactory();
      }

      if (graphics != null && !isHint) {
         graphics.setRenderingHint(Drawable.DRAW_FACTORY, factory);
      }

      return factory;
   }

   public Drawable getDrawable(Shape<?, ?> shape) {
      if (shape instanceof TextBox) {
         return this.getDrawable((TextBox<?, ?>)shape);
      } else if (shape instanceof FreeformShape) {
         return this.getDrawable((FreeformShape<?, ?>)shape);
      } else if (shape instanceof TextShape) {
         return this.getDrawable((TextShape<?, ?>)shape);
      } else if (shape instanceof TableShape) {
         return this.getDrawable((TableShape<?, ?>)shape);
      } else if (shape instanceof GroupShape) {
         return this.getDrawable((GroupShape<?, ?>)shape);
      } else if (shape instanceof PictureShape) {
         return this.getDrawable((PictureShape<?, ?>)shape);
      } else if (shape instanceof GraphicalFrame) {
         return this.getDrawable((GraphicalFrame<?, ?>)shape);
      } else if (shape instanceof Background) {
         return this.getDrawable((Background<?, ?>)shape);
      } else if (shape instanceof ConnectorShape) {
         return this.getDrawable((ConnectorShape<?, ?>)shape);
      } else if (shape instanceof Slide) {
         return this.getDrawable((Slide<?, ?>)shape);
      } else if (shape instanceof MasterSheet) {
         return this.getDrawable((MasterSheet<?, ?>)shape);
      } else if (shape instanceof Sheet) {
         return this.getDrawable((Sheet<?, ?>)shape);
      } else if (shape.getClass().isAnnotationPresent(DrawNotImplemented.class)) {
         return new DrawNothing(shape);
      } else {
         throw new IllegalArgumentException("Unsupported shape type: " + shape.getClass());
      }
   }

   public DrawSlide getDrawable(Slide<?, ?> sheet) {
      return new DrawSlide(sheet);
   }

   public DrawSheet getDrawable(Sheet<?, ?> sheet) {
      return new DrawSheet(sheet);
   }

   public DrawMasterSheet getDrawable(MasterSheet<?, ?> sheet) {
      return new DrawMasterSheet(sheet);
   }

   public DrawTextBox getDrawable(TextBox<?, ?> shape) {
      return new DrawTextBox(shape);
   }

   public DrawFreeformShape getDrawable(FreeformShape<?, ?> shape) {
      return new DrawFreeformShape(shape);
   }

   public DrawConnectorShape getDrawable(ConnectorShape<?, ?> shape) {
      return new DrawConnectorShape(shape);
   }

   public DrawTableShape getDrawable(TableShape<?, ?> shape) {
      return new DrawTableShape(shape);
   }

   public DrawTextShape getDrawable(TextShape<?, ?> shape) {
      return new DrawTextShape(shape);
   }

   public DrawGroupShape getDrawable(GroupShape<?, ?> shape) {
      return new DrawGroupShape(shape);
   }

   public DrawPictureShape getDrawable(PictureShape<?, ?> shape) {
      return new DrawPictureShape(shape);
   }

   public DrawGraphicalFrame getDrawable(GraphicalFrame<?, ?> shape) {
      return new DrawGraphicalFrame(shape);
   }

   public DrawTextParagraph getDrawable(TextParagraph<?, ?, ?> paragraph) {
      return new DrawTextParagraph(paragraph);
   }

   public DrawBackground getDrawable(Background<?, ?> shape) {
      return new DrawBackground(shape);
   }

   public DrawTextFragment getTextFragment(TextLayout layout, AttributedString str) {
      return new DrawTextFragment(layout, str);
   }

   public DrawPaint getPaint(PlaceableShape<?, ?> shape) {
      return new DrawPaint(shape);
   }

   public void drawShape(Graphics2D graphics, Shape<?, ?> shape, Rectangle2D bounds) {
      Rectangle2D shapeBounds = shape.getAnchor();
      if (!shapeBounds.isEmpty() && (bounds == null || !bounds.isEmpty())) {
         AffineTransform txg = (AffineTransform)graphics.getRenderingHint(Drawable.GROUP_TRANSFORM);
         AffineTransform tx = new AffineTransform();

         try {
            if (bounds != null) {
               double scaleX = bounds.getWidth() / shapeBounds.getWidth();
               double scaleY = bounds.getHeight() / shapeBounds.getHeight();
               tx.translate(bounds.getCenterX(), bounds.getCenterY());
               tx.scale(scaleX, scaleY);
               tx.translate(-shapeBounds.getCenterX(), -shapeBounds.getCenterY());
            }

            graphics.setRenderingHint(Drawable.GROUP_TRANSFORM, tx);
            Drawable d = this.getDrawable(shape);
            d.applyTransform(graphics);
            d.draw(graphics);
         } finally {
            graphics.setRenderingHint(Drawable.GROUP_TRANSFORM, txg);
         }
      }
   }

   public void fixFonts(Graphics2D graphics) {
      if (JvmBugs.hasLineBreakMeasurerBug()) {
         Map<String, String> fontMap = (Map<String, String>)graphics.getRenderingHint(Drawable.FONT_MAP);
         if (fontMap == null) {
            fontMap = new HashMap();
            graphics.setRenderingHint(Drawable.FONT_MAP, fontMap);
         }

         String[][] fonts = new String[][]{
            {"Calibri", "Lucida Sans"}, {"Cambria", "Lucida Bright"}, {"Times New Roman", "Lucida Bright"}, {"serif", "Lucida Bright"}
         };

         for (String[] f : fonts) {
            if (!fontMap.containsKey(f[0])) {
               fontMap.put(f[0], f[1]);
            }
         }
      }
   }

   public DrawFontManager getFontManager(Graphics2D graphics) {
      DrawFontManager fontHandler = (DrawFontManager)graphics.getRenderingHint(Drawable.FONT_HANDLER);
      return (DrawFontManager)(fontHandler != null ? fontHandler : new DrawFontManagerDefault());
   }
}

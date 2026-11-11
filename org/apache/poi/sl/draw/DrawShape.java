package org.apache.poi.sl.draw;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Locale;
import org.apache.poi.sl.usermodel.PlaceableShape;
import org.apache.poi.sl.usermodel.Shape;
import org.apache.poi.sl.usermodel.StrokeStyle;
import org.apache.poi.sl.usermodel.StrokeStyle.LineCap;
import org.apache.poi.sl.usermodel.StrokeStyle.LineDash;

public class DrawShape implements Drawable {
   protected final Shape<?, ?> shape;

   public DrawShape(Shape<?, ?> shape) {
      this.shape = shape;
   }

   static boolean isHSLF(Object shape) {
      return shape.getClass().getName().toLowerCase(Locale.ROOT).contains("hslf");
   }

   public void applyTransform(Graphics2D graphics) {
      if (this.shape instanceof PlaceableShape && graphics != null) {
         PlaceableShape<?, ?> ps = (PlaceableShape<?, ?>)this.shape;
         boolean isHSLF = isHSLF(this.shape);
         Rectangle2D anchor = getAnchor(graphics, ps);
         char[] cmds = isHSLF ? new char[]{'h', 'v', 'r'} : new char[]{'r', 'h', 'v'};

         for (char ch : cmds) {
            switch (ch) {
               case 'h':
                  if (ps.getFlipHorizontal()) {
                     graphics.translate(anchor.getX() + anchor.getWidth(), anchor.getY());
                     graphics.scale(-1.0, 1.0);
                     graphics.translate(-anchor.getX(), -anchor.getY());
                  }
                  break;
               case 'r':
                  double rotation = ps.getRotation();
                  if (rotation != 0.0) {
                     double centerX = anchor.getCenterX();
                     double centerY = anchor.getCenterY();
                     graphics.translate(centerX, centerY);
                     graphics.rotate(Math.toRadians(rotation));
                     graphics.translate(-centerX, -centerY);
                  }
                  break;
               case 'v':
                  if (ps.getFlipVertical()) {
                     graphics.translate(anchor.getX(), anchor.getY() + anchor.getHeight());
                     graphics.scale(1.0, -1.0);
                     graphics.translate(-anchor.getX(), -anchor.getY());
                  }
                  break;
               default:
                  throw new RuntimeException("unexpected transform code " + ch);
            }
         }
      }
   }

   private static double safeScale(double dim1, double dim2) {
      if (dim1 == 0.0) {
         return 1.0;
      } else {
         return dim2 == 0.0 ? 1.0 : dim1 / dim2;
      }
   }

   public void draw(Graphics2D graphics) {
   }

   public void drawContent(Graphics2D graphics) {
   }

   public static Rectangle2D getAnchor(Graphics2D graphics, PlaceableShape<?, ?> shape) {
      boolean isHSLF = isHSLF(shape);
      AffineTransform tx = graphics == null ? null : (AffineTransform)graphics.getRenderingHint(Drawable.GROUP_TRANSFORM);
      if (tx == null) {
         tx = new AffineTransform();
      }

      double rotation = (shape.getRotation() % 360.0 + 360.0) % 360.0;
      int quadrant = ((int)rotation + 45) / 90 % 4;
      Rectangle2D normalizedShape;
      if (quadrant != 1 && quadrant != 3) {
         normalizedShape = shape.getAnchor();
      } else {
         Rectangle2D shapeAnchor = shape.getAnchor();
         Rectangle2D anchorO = tx.createTransformedShape(shapeAnchor).getBounds2D();
         double centerX = anchorO.getCenterX();
         double centerY = anchorO.getCenterY();
         AffineTransform txs2 = new AffineTransform();
         if (!isHSLF) {
            txs2.translate(centerX, centerY);
            txs2.quadrantRotate(1);
            txs2.translate(-centerX, -centerY);
            txs2.concatenate(tx);
         }

         txs2.translate(centerX, centerY);
         txs2.quadrantRotate(3);
         txs2.translate(-centerX, -centerY);
         if (isHSLF) {
            txs2.concatenate(tx);
         }

         Rectangle2D anchorT = txs2.createTransformedShape(shapeAnchor).getBounds2D();
         centerX = safeScale(anchorO.getWidth(), anchorT.getWidth());
         centerY = safeScale(anchorO.getHeight(), anchorT.getHeight());
         double centerXx = shapeAnchor.getCenterX();
         double centerYx = shapeAnchor.getCenterY();
         AffineTransform txs2x = new AffineTransform();
         txs2x.translate(centerXx, centerYx);
         txs2x.scale(centerY, centerX);
         txs2x.translate(-centerXx, -centerYx);
         normalizedShape = txs2x.createTransformedShape(shapeAnchor).getBounds2D();
      }

      if (tx.isIdentity()) {
         return normalizedShape;
      }

      java.awt.Shape anc = tx.createTransformedShape(normalizedShape);
      return anc != null ? anc.getBounds2D() : normalizedShape;
   }

   public static Rectangle2D getAnchor(Graphics2D graphics, Rectangle2D anchor) {
      if (graphics == null) {
         return anchor;
      }

      AffineTransform tx = (AffineTransform)graphics.getRenderingHint(Drawable.GROUP_TRANSFORM);
      if (tx != null && !tx.isIdentity() && tx.createTransformedShape(anchor) != null) {
         anchor = tx.createTransformedShape(anchor).getBounds2D();
      }

      return anchor;
   }

   protected Shape<?, ?> getShape() {
      return this.shape;
   }

   protected static BasicStroke getStroke(StrokeStyle strokeStyle) {
      float lineWidth = (float)strokeStyle.getLineWidth();
      if (lineWidth == 0.0F) {
         lineWidth = 0.25F;
      }

      LineDash lineDash = strokeStyle.getLineDash();
      if (lineDash == null) {
         lineDash = LineDash.SOLID;
      }

      int[] dashPatI = lineDash.pattern;
      float dash_phase = 0.0F;
      float[] dashPatF = null;
      if (dashPatI != null) {
         dashPatF = new float[dashPatI.length];

         for (int i = 0; i < dashPatI.length; i++) {
            dashPatF[i] = dashPatI[i] * Math.max(1.0F, lineWidth);
         }
      }

      LineCap lineCapE = strokeStyle.getLineCap();
      if (lineCapE == null) {
         lineCapE = LineCap.FLAT;
      }

      int lineCap;
      switch (lineCapE) {
         case ROUND:
            lineCap = 1;
            break;
         case SQUARE:
            lineCap = 2;
            break;
         case FLAT:
         default:
            lineCap = 0;
      }

      int lineJoin = 1;
      return new BasicStroke(lineWidth, lineCap, lineJoin, lineWidth, dashPatF, 0.0F);
   }
}

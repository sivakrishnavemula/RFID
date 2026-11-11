package org.apache.poi.sl.draw;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import org.apache.poi.sl.usermodel.Insets2D;
import org.apache.poi.sl.usermodel.PlaceableShape;
import org.apache.poi.sl.usermodel.ShapeContainer;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.sl.usermodel.TextRun;
import org.apache.poi.sl.usermodel.TextShape;
import org.apache.poi.sl.usermodel.TextParagraph.BulletStyle;
import org.apache.poi.sl.usermodel.TextShape.TextDirection;

public class DrawTextShape extends DrawSimpleShape {
   public DrawTextShape(TextShape<?, ?> shape) {
      super(shape);
   }

   public void drawContent(Graphics2D graphics) {
      DrawFactory.getInstance(graphics).fixFonts(graphics);
      TextShape<?, ?> s = this.getShape();
      Rectangle2D anchor = DrawShape.getAnchor(graphics, s);
      if (anchor != null) {
         Insets2D insets = s.getInsets();
         double x = anchor.getX() + insets.left;
         double y = anchor.getY();
         AffineTransform tx = graphics.getTransform();
         boolean vertFlip = s.getFlipVertical();
         boolean horzFlip = s.getFlipHorizontal();
         ShapeContainer<?, ?> sc = s.getParent();

         while (sc instanceof PlaceableShape) {
            PlaceableShape<?, ?> ps = (PlaceableShape<?, ?>)sc;
            vertFlip ^= ps.getFlipVertical();
            horzFlip ^= ps.getFlipHorizontal();
            sc = ps.getParent();
         }

         if (horzFlip ^ vertFlip) {
            double ax = anchor.getX();
            double ay = anchor.getY();
            graphics.translate(ax + anchor.getWidth(), ay);
            graphics.scale(-1.0, 1.0);
            graphics.translate(-ax, -ay);
         }

         Double textRot = s.getTextRotation();
         if (textRot != null && textRot != 0.0) {
            double cx = anchor.getCenterX();
            double cy = anchor.getCenterY();
            graphics.translate(cx, cy);
            graphics.rotate(Math.toRadians(textRot));
            graphics.translate(-cx, -cy);
         }

         switch (s.getVerticalAlignment()) {
            case TOP:
            default:
               y += insets.top;
               break;
            case BOTTOM: {
               double textHeight = this.getTextHeight(graphics);
               y += anchor.getHeight() - textHeight - insets.bottom;
               break;
            }
            case MIDDLE: {
               double textHeight = this.getTextHeight(graphics);
               double delta = anchor.getHeight() - textHeight - insets.top - insets.bottom;
               y += insets.top + delta / 2.0;
            }
         }

         TextDirection textDir = s.getTextDirection();
         if (textDir == TextDirection.VERTICAL || textDir == TextDirection.VERTICAL_270) {
            double deg = textDir == TextDirection.VERTICAL ? 90.0 : 270.0;
            double cx = anchor.getCenterX();
            double cy = anchor.getCenterY();
            graphics.translate(cx, cy);
            graphics.rotate(Math.toRadians(deg));
            graphics.translate(-cx, -cy);
            double w = anchor.getWidth();
            double h = anchor.getHeight();
            double dx = (w - h) / 2.0;
            graphics.translate(dx, -dx);
         }

         this.drawParagraphs(graphics, x, y);
         graphics.setTransform(tx);
      }
   }

   public double drawParagraphs(Graphics2D graphics, double x, double y) {
      DrawFactory fact = DrawFactory.getInstance(graphics);
      double y0 = y;
      Iterator<? extends TextParagraph<?, ?, ? extends TextRun>> paragraphs = this.getShape().iterator();
      boolean isFirstLine = true;

      for (int autoNbrIdx = 0; paragraphs.hasNext(); autoNbrIdx++) {
         TextParagraph<?, ?, ? extends TextRun> p = (TextParagraph<?, ?, ? extends TextRun>)paragraphs.next();
         DrawTextParagraph dp = fact.getDrawable(p);
         BulletStyle bs = p.getBulletStyle();
         if (bs != null && bs.getAutoNumberingScheme() != null) {
            Integer startAt = bs.getAutoNumberingStartAt();
            if (startAt == null) {
               startAt = 1;
            }

            if (startAt > autoNbrIdx) {
               autoNbrIdx = startAt;
            }
         } else {
            autoNbrIdx = -1;
         }

         dp.setAutoNumberingIdx(autoNbrIdx);
         dp.breakText(graphics);
         if (isFirstLine) {
            y += dp.getFirstLineLeading();
         } else {
            Double spaceBefore = p.getSpaceBefore();
            if (spaceBefore == null) {
               spaceBefore = 0.0;
            }

            if (spaceBefore > 0.0) {
               y += spaceBefore * 0.01 * dp.getFirstLineHeight();
            } else {
               y += -spaceBefore;
            }
         }

         isFirstLine = false;
         dp.setPosition(x, y);
         dp.draw(graphics);
         y += dp.getY();
         if (paragraphs.hasNext()) {
            Double spaceAfter = p.getSpaceAfter();
            if (spaceAfter == null) {
               spaceAfter = 0.0;
            }

            if (spaceAfter > 0.0) {
               y += spaceAfter * 0.01 * dp.getLastLineHeight();
            } else {
               y += -spaceAfter;
            }
         }
      }

      return y - y0;
   }

   public double getTextHeight() {
      return this.getTextHeight(null);
   }

   public double getTextHeight(Graphics2D oldGraphics) {
      BufferedImage img = new BufferedImage(1, 1, 1);
      Graphics2D graphics = img.createGraphics();
      if (oldGraphics != null) {
         graphics.addRenderingHints(oldGraphics.getRenderingHints());
         graphics.setTransform(oldGraphics.getTransform());
      }

      DrawFactory.getInstance(graphics).fixFonts(graphics);
      return this.drawParagraphs(graphics, 0.0, 0.0);
   }

   protected TextShape<?, ? extends TextParagraph<?, ?, ? extends TextRun>> getShape() {
      return (TextShape<?, ? extends TextParagraph<?, ?, ? extends TextRun>>)this.shape;
   }
}

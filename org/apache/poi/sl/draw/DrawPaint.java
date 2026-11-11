package org.apache.poi.sl.draw;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D.Double;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.apache.poi.sl.usermodel.AbstractColorStyle;
import org.apache.poi.sl.usermodel.ColorStyle;
import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.sl.usermodel.PlaceableShape;
import org.apache.poi.sl.usermodel.PaintStyle.GradientPaint;
import org.apache.poi.sl.usermodel.PaintStyle.PaintModifier;
import org.apache.poi.sl.usermodel.PaintStyle.SolidPaint;
import org.apache.poi.sl.usermodel.PaintStyle.TexturePaint;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

public class DrawPaint {
   private static final POILogger LOG = POILogFactory.getLogger(DrawPaint.class);
   private static final Color TRANSPARENT = new Color(1.0F, 1.0F, 1.0F, 0.0F);
   protected PlaceableShape<?, ?> shape;

   public DrawPaint(PlaceableShape<?, ?> shape) {
      this.shape = shape;
   }

   public static SolidPaint createSolidPaint(Color color) {
      return color == null ? null : new DrawPaint.SimpleSolidPaint(color);
   }

   public static SolidPaint createSolidPaint(ColorStyle color) {
      return color == null ? null : new DrawPaint.SimpleSolidPaint(color);
   }

   public Paint getPaint(Graphics2D graphics, PaintStyle paint) {
      return this.getPaint(graphics, paint, PaintModifier.NORM);
   }

   public Paint getPaint(Graphics2D graphics, PaintStyle paint, PaintModifier modifier) {
      if (modifier == PaintModifier.NONE) {
         return null;
      } else if (paint instanceof SolidPaint) {
         return this.getSolidPaint((SolidPaint)paint, graphics, modifier);
      } else if (paint instanceof GradientPaint) {
         return this.getGradientPaint((GradientPaint)paint, graphics);
      } else {
         return paint instanceof TexturePaint ? this.getTexturePaint((TexturePaint)paint, graphics) : null;
      }
   }

   protected Paint getSolidPaint(SolidPaint fill, Graphics2D graphics, final PaintModifier modifier) {
      final ColorStyle orig = fill.getSolidColor();
      ColorStyle cs = new AbstractColorStyle() {
         public Color getColor() {
            return orig.getColor();
         }

         public int getAlpha() {
            return orig.getAlpha();
         }

         public int getHueOff() {
            return orig.getHueOff();
         }

         public int getHueMod() {
            return orig.getHueMod();
         }

         public int getSatOff() {
            return orig.getSatOff();
         }

         public int getSatMod() {
            return orig.getSatMod();
         }

         public int getLumOff() {
            return orig.getLumOff();
         }

         public int getLumMod() {
            return orig.getLumMod();
         }

         public int getShade() {
            int shade = orig.getShade();
            switch (modifier) {
               case DARKEN:
                  return Math.min(100000, Math.max(0, shade) + 40000);
               case DARKEN_LESS:
                  return Math.min(100000, Math.max(0, shade) + 20000);
               default:
                  return shade;
            }
         }

         public int getTint() {
            int tint = orig.getTint();
            switch (modifier) {
               case LIGHTEN:
                  return Math.min(100000, Math.max(0, tint) + 40000);
               case LIGHTEN_LESS:
                  return Math.min(100000, Math.max(0, tint) + 20000);
               default:
                  return tint;
            }
         }
      };
      return applyColorTransform(cs);
   }

   protected Paint getGradientPaint(GradientPaint fill, Graphics2D graphics) {
      switch (fill.getGradientType()) {
         case linear:
            return this.createLinearGradientPaint(fill, graphics);
         case circular:
            return this.createRadialGradientPaint(fill, graphics);
         case shape:
            return this.createPathGradientPaint(fill, graphics);
         default:
            throw new UnsupportedOperationException("gradient fill of type " + fill + " not supported.");
      }
   }

   protected Paint getTexturePaint(TexturePaint fill, Graphics2D graphics) {
      InputStream is = fill.getImageData();
      if (is == null) {
         return null;
      }

      assert graphics != null;
      ImageRenderer renderer = DrawPictureShape.getImageRenderer(graphics, fill.getContentType());

      try {
         try {
            renderer.loadImage(is, fill.getContentType());
         } finally {
            is.close();
         }
      } catch (IOException e) {
         LOG.log(7, new Object[]{"Can't load image data - using transparent color", e});
         return null;
      }

      int alpha = fill.getAlpha();
      if (0 <= alpha && alpha < 100000) {
         renderer.setAlpha(alpha / 100000.0F);
      }

      Rectangle2D textAnchor = this.shape.getAnchor();
      BufferedImage image;
      if ("image/x-wmf".equals(fill.getContentType())) {
         image = renderer.getImage(new Dimension((int)textAnchor.getWidth(), (int)textAnchor.getHeight()));
      } else {
         image = renderer.getImage();
      }

      if (image == null) {
         LOG.log(7, new Object[]{"Can't load image data"});
         return null;
      } else {
         return new java.awt.TexturePaint(image, textAnchor);
      }
   }

   public static Color applyColorTransform(ColorStyle color) {
      if (color != null && color.getColor() != null) {
         Color result = color.getColor();
         double alpha = getAlpha(result, color);
         double[] hsl = RGB2HSL(result);
         applyHslModOff(hsl, 0, color.getHueMod(), color.getHueOff());
         applyHslModOff(hsl, 1, color.getSatMod(), color.getSatOff());
         applyHslModOff(hsl, 2, color.getLumMod(), color.getLumOff());
         applyShade(hsl, color);
         applyTint(hsl, color);
         return HSL2RGB(hsl[0], hsl[1], hsl[2], alpha);
      } else {
         return TRANSPARENT;
      }
   }

   private static double getAlpha(Color c, ColorStyle fc) {
      double alpha = c.getAlpha() / 255.0;
      int fcAlpha = fc.getAlpha();
      if (fcAlpha != -1) {
         alpha *= fcAlpha / 100000.0;
      }

      return Math.min(1.0, Math.max(0.0, alpha));
   }

   private static void applyHslModOff(double[] hsl, int hslPart, int mod, int off) {
      if (mod == -1) {
         mod = 100000;
      }

      if (off == -1) {
         off = 0;
      }

      if (mod != 100000 || off != 0) {
         double fOff = off / 1000.0;
         double fMod = mod / 100000.0;
         hsl[hslPart] = hsl[hslPart] * fMod + fOff;
      }
   }

   private static void applyShade(double[] hsl, ColorStyle fc) {
      int shade = fc.getShade();
      if (shade != -1) {
         double shadePct = shade / 100000.0;
         hsl[2] *= 1.0 - shadePct;
      }
   }

   private static void applyTint(double[] hsl, ColorStyle fc) {
      int tint = fc.getTint();
      if (tint != -1) {
         double tintPct = tint / 100000.0;
         hsl[2] = hsl[2] * (1.0 - tintPct) + (100.0 - 100.0 * (1.0 - tintPct));
      }
   }

   protected Paint createLinearGradientPaint(GradientPaint fill, Graphics2D graphics) {
      double angle = fill.getGradientAngle();
      if (!fill.isRotatedWithShape()) {
         angle -= this.shape.getRotation();
      }

      Rectangle2D anchor = DrawShape.getAnchor(graphics, this.shape);
      double h = anchor.getHeight();
      double w = anchor.getWidth();
      double x = anchor.getX();
      double y = anchor.getY();
      AffineTransform at = AffineTransform.getRotateInstance(Math.toRadians(angle), anchor.getCenterX(), anchor.getCenterY());
      double diagonal = Math.sqrt(h * h + w * w);
      Point2D p1 = new Double(x + w / 2.0 - diagonal / 2.0, y + h / 2.0);
      p1 = at.transform(p1, null);
      Point2D p2 = new Double(x + w, y + h / 2.0);
      p2 = at.transform(p2, null);
      if (p1.equals(p2)) {
         return null;
      }

      float[] fractions = fill.getGradientFractions();
      Color[] colors = new Color[fractions.length];
      int i = 0;

      for (ColorStyle fc : fill.getGradientColors()) {
         colors[i++] = fc == null ? TRANSPARENT : applyColorTransform(fc);
      }

      return new LinearGradientPaint(p1, p2, fractions, colors);
   }

   protected Paint createRadialGradientPaint(GradientPaint fill, Graphics2D graphics) {
      Rectangle2D anchor = DrawShape.getAnchor(graphics, this.shape);
      Point2D pCenter = new Double(anchor.getX() + anchor.getWidth() / 2.0, anchor.getY() + anchor.getHeight() / 2.0);
      float radius = (float)Math.max(anchor.getWidth(), anchor.getHeight());
      float[] fractions = fill.getGradientFractions();
      Color[] colors = new Color[fractions.length];
      int i = 0;

      for (ColorStyle fc : fill.getGradientColors()) {
         colors[i++] = applyColorTransform(fc);
      }

      return new RadialGradientPaint(pCenter, radius, fractions, colors);
   }

   protected Paint createPathGradientPaint(GradientPaint fill, Graphics2D graphics) {
      float[] fractions = fill.getGradientFractions();
      Color[] colors = new Color[fractions.length];
      int i = 0;

      for (ColorStyle fc : fill.getGradientColors()) {
         colors[i++] = applyColorTransform(fc);
      }

      return new PathGradientPaint(colors, fractions);
   }

   public static Color HSL2RGB(double h, double s, double l, double alpha) {
      s = Math.max(0.0, Math.min(100.0, s));
      l = Math.max(0.0, Math.min(100.0, l));
      if (!(alpha < 0.0) && !(alpha > 1.0)) {
         h %= 360.0;
         h /= 360.0;
         s /= 100.0;
         l /= 100.0;
         double q = l < 0.5 ? l * (1.0 + s) : l + s - s * l;
         double p = 2.0 * l - q;
         double r = Math.max(0.0, HUE2RGB(p, q, h + 0.33333334F));
         double g = Math.max(0.0, HUE2RGB(p, q, h));
         double b = Math.max(0.0, HUE2RGB(p, q, h - 0.33333334F));
         r = Math.min(r, 1.0);
         g = Math.min(g, 1.0);
         b = Math.min(b, 1.0);
         return new Color((float)r, (float)g, (float)b, (float)alpha);
      } else {
         String message = "Color parameter outside of expected range - Alpha: " + alpha;
         throw new IllegalArgumentException(message);
      }
   }

   private static double HUE2RGB(double p, double q, double h) {
      if (h < 0.0) {
         h++;
      }

      if (h > 1.0) {
         h--;
      }

      if (6.0 * h < 1.0) {
         return p + (q - p) * 6.0 * h;
      } else if (2.0 * h < 1.0) {
         return q;
      } else {
         return 3.0 * h < 2.0 ? p + (q - p) * 6.0 * (0.6666667F - h) : p;
      }
   }

   private static double[] RGB2HSL(Color color) {
      float[] rgb = color.getRGBColorComponents(null);
      double r = rgb[0];
      double g = rgb[1];
      double b = rgb[2];
      double min = Math.min(r, Math.min(g, b));
      double max = Math.max(r, Math.max(g, b));
      double h = 0.0;
      if (max == min) {
         h = 0.0;
      } else if (max == r) {
         h = (60.0 * (g - b) / (max - min) + 360.0) % 360.0;
      } else if (max == g) {
         h = 60.0 * (b - r) / (max - min) + 120.0;
      } else if (max == b) {
         h = 60.0 * (r - g) / (max - min) + 240.0;
      }

      double l = (max + min) / 2.0;
      double s;
      if (max == min) {
         s = 0.0;
      } else if (l <= 0.5) {
         s = (max - min) / (max + min);
      } else {
         s = (max - min) / (2.0 - max - min);
      }

      return new double[]{h, s * 100.0, l * 100.0};
   }

   public static int srgb2lin(float sRGB) {
      return sRGB <= 0.04045 ? (int)Math.rint(100000.0 * sRGB / 12.92) : (int)Math.rint(100000.0 * Math.pow((sRGB + 0.055) / 1.055, 2.4));
   }

   public static float lin2srgb(int linRGB) {
      return linRGB <= 0.0031308 ? (float)(linRGB / 100000.0 * 12.92) : (float)(1.055 * Math.pow(linRGB / 100000.0, 0.41666666F) - 0.055);
   }

   private static class SimpleSolidPaint implements SolidPaint {
      private final ColorStyle solidColor;

      SimpleSolidPaint(final Color color) {
         if (color == null) {
            throw new NullPointerException("Color needs to be specified");
         }

         this.solidColor = new AbstractColorStyle() {
            public Color getColor() {
               return new Color(color.getRed(), color.getGreen(), color.getBlue());
            }

            public int getAlpha() {
               return (int)Math.round(color.getAlpha() * 100000.0 / 255.0);
            }

            public int getHueOff() {
               return -1;
            }

            public int getHueMod() {
               return -1;
            }

            public int getSatOff() {
               return -1;
            }

            public int getSatMod() {
               return -1;
            }

            public int getLumOff() {
               return -1;
            }

            public int getLumMod() {
               return -1;
            }

            public int getShade() {
               return -1;
            }

            public int getTint() {
               return -1;
            }
         };
      }

      SimpleSolidPaint(ColorStyle color) {
         if (color == null) {
            throw new NullPointerException("Color needs to be specified");
         }

         this.solidColor = color;
      }

      public ColorStyle getSolidColor() {
         return this.solidColor;
      }

      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else {
            return !(o instanceof SolidPaint) ? false : Objects.equals(this.getSolidColor(), ((SolidPaint)o).getSolidColor());
         }
      }

      public int hashCode() {
         return Objects.hash(new Object[]{this.solidColor});
      }
   }
}

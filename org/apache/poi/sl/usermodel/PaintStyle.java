package org.apache.poi.sl.usermodel;

import java.io.InputStream;

public interface PaintStyle {
   interface GradientPaint extends PaintStyle {
      double getGradientAngle();

      ColorStyle[] getGradientColors();

      float[] getGradientFractions();

      boolean isRotatedWithShape();

      PaintStyle.GradientPaint.GradientType getGradientType();

      enum GradientType {
         linear,
         circular,
         shape;
      }
   }

   enum PaintModifier {
      NONE,
      NORM,
      LIGHTEN,
      LIGHTEN_LESS,
      DARKEN,
      DARKEN_LESS;
   }

   interface SolidPaint extends PaintStyle {
      ColorStyle getSolidColor();
   }

   interface TexturePaint extends PaintStyle {
      InputStream getImageData();

      String getContentType();

      int getAlpha();
   }
}

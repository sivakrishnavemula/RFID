package org.apache.poi.util;

public class Units {
   public static final int EMU_PER_PIXEL = 9525;
   public static final int EMU_PER_POINT = 12700;
   public static final int EMU_PER_CENTIMETER = 360000;
   public static final int MASTER_DPI = 576;
   public static final int PIXEL_DPI = 96;
   public static final int POINT_DPI = 72;
   public static final float DEFAULT_CHARACTER_WIDTH = 7.0017F;
   public static final int EMU_PER_CHARACTER = 66691;

   public static int toEMU(double points) {
      return (int)Math.rint(12700.0 * points);
   }

   public static int pixelToEMU(int pixels) {
      return pixels * 9525;
   }

   public static double toPoints(long emu) {
      return emu / 12700.0;
   }

   public static double fixedPointToDouble(int fixedPoint) {
      int i = fixedPoint >> 16;
      int f = fixedPoint & 65535;
      return i + f / 65536.0;
   }

   public static int doubleToFixedPoint(double floatPoint) {
      double fractionalPart = floatPoint % 1.0;
      double integralPart = floatPoint - fractionalPart;
      int i = (int)Math.floor(integralPart);
      int f = (int)Math.rint(fractionalPart * 65536.0);
      return i << 16 | f & 65535;
   }

   public static double masterToPoints(int masterDPI) {
      double points = masterDPI;
      points *= 72.0;
      return points / 576.0;
   }

   public static int pointsToMaster(double points) {
      points *= 576.0;
      points /= 72.0;
      return (int)Math.rint(points);
   }

   public static int pointsToPixel(double points) {
      points *= 96.0;
      points /= 72.0;
      return (int)Math.rint(points);
   }

   public static double pixelToPoints(int pixel) {
      double points = pixel;
      points *= 72.0;
      return points / 96.0;
   }

   public static int charactersToEMU(double characters) {
      return (int)characters * 66691;
   }

   public static int columnWidthToEMU(int columnWidth) {
      return charactersToEMU(columnWidth / 256.0);
   }

   public static int TwipsToEMU(short twips) {
      return (int)(twips / 20.0 * 12700.0);
   }
}

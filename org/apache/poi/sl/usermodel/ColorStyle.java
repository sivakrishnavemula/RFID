package org.apache.poi.sl.usermodel;

import java.awt.Color;

public interface ColorStyle {
   Color getColor();

   int getAlpha();

   int getHueOff();

   int getHueMod();

   int getSatOff();

   int getSatMod();

   int getLumOff();

   int getLumMod();

   int getShade();

   int getTint();
}

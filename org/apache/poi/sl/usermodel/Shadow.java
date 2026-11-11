package org.apache.poi.sl.usermodel;

import org.apache.poi.sl.usermodel.PaintStyle.SolidPaint;

public interface Shadow<S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> {
   SimpleShape<S, P> getShadowParent();

   double getDistance();

   double getAngle();

   double getBlur();

   SolidPaint getFillStyle();
}

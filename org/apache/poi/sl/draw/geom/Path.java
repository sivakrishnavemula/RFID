package org.apache.poi.sl.draw.geom;

import java.awt.geom.Path2D.Double;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.sl.draw.binding.CTAdjPoint2D;
import org.apache.poi.sl.draw.binding.CTPath2D;
import org.apache.poi.sl.draw.binding.CTPath2DArcTo;
import org.apache.poi.sl.draw.binding.CTPath2DClose;
import org.apache.poi.sl.draw.binding.CTPath2DCubicBezierTo;
import org.apache.poi.sl.draw.binding.CTPath2DLineTo;
import org.apache.poi.sl.draw.binding.CTPath2DMoveTo;
import org.apache.poi.sl.draw.binding.CTPath2DQuadBezierTo;
import org.apache.poi.sl.usermodel.PaintStyle.PaintModifier;

public class Path {
   private final List<PathCommand> commands;
   PaintModifier _fill;
   boolean _stroke;
   long _w;
   long _h;

   public Path() {
      this(true, true);
   }

   public Path(boolean fill, boolean stroke) {
      this.commands = new ArrayList();
      this._w = -1L;
      this._h = -1L;
      this._fill = fill ? PaintModifier.NORM : PaintModifier.NONE;
      this._stroke = stroke;
   }

   public Path(CTPath2D spPath) {
      switch (spPath.getFill()) {
         case NONE:
            this._fill = PaintModifier.NONE;
            break;
         case DARKEN:
            this._fill = PaintModifier.DARKEN;
            break;
         case DARKEN_LESS:
            this._fill = PaintModifier.DARKEN_LESS;
            break;
         case LIGHTEN:
            this._fill = PaintModifier.LIGHTEN;
            break;
         case LIGHTEN_LESS:
            this._fill = PaintModifier.LIGHTEN_LESS;
            break;
         case NORM:
         default:
            this._fill = PaintModifier.NORM;
      }

      this._stroke = spPath.isStroke();
      this._w = spPath.isSetW() ? spPath.getW() : -1L;
      this._h = spPath.isSetH() ? spPath.getH() : -1L;
      this.commands = new ArrayList();

      for (Object ch : spPath.getCloseOrMoveToOrLnTo()) {
         if (ch instanceof CTPath2DMoveTo) {
            CTAdjPoint2D pt = ((CTPath2DMoveTo)ch).getPt();
            this.commands.add(new MoveToCommand(pt));
         } else if (ch instanceof CTPath2DLineTo) {
            CTAdjPoint2D pt = ((CTPath2DLineTo)ch).getPt();
            this.commands.add(new LineToCommand(pt));
         } else if (ch instanceof CTPath2DArcTo) {
            CTPath2DArcTo arc = (CTPath2DArcTo)ch;
            this.commands.add(new ArcToCommand(arc));
         } else if (ch instanceof CTPath2DQuadBezierTo) {
            CTPath2DQuadBezierTo bez = (CTPath2DQuadBezierTo)ch;
            CTAdjPoint2D pt1 = (CTAdjPoint2D)bez.getPt().get(0);
            CTAdjPoint2D pt2 = (CTAdjPoint2D)bez.getPt().get(1);
            this.commands.add(new QuadToCommand(pt1, pt2));
         } else if (ch instanceof CTPath2DCubicBezierTo) {
            CTPath2DCubicBezierTo bez = (CTPath2DCubicBezierTo)ch;
            CTAdjPoint2D pt1 = (CTAdjPoint2D)bez.getPt().get(0);
            CTAdjPoint2D pt2 = (CTAdjPoint2D)bez.getPt().get(1);
            CTAdjPoint2D pt3 = (CTAdjPoint2D)bez.getPt().get(2);
            this.commands.add(new CurveToCommand(pt1, pt2, pt3));
         } else {
            if (!(ch instanceof CTPath2DClose)) {
               throw new IllegalStateException("Unsupported path segment: " + ch);
            }

            this.commands.add(new ClosePathCommand());
         }
      }
   }

   public void addCommand(PathCommand cmd) {
      this.commands.add(cmd);
   }

   public Double getPath(Context ctx) {
      Double path = new Double();

      for (PathCommand cmd : this.commands) {
         cmd.execute(path, ctx);
      }

      return path;
   }

   public boolean isStroked() {
      return this._stroke;
   }

   public boolean isFilled() {
      return this._fill != PaintModifier.NONE;
   }

   public PaintModifier getFill() {
      return this._fill;
   }

   public long getW() {
      return this._w;
   }

   public long getH() {
      return this._h;
   }
}

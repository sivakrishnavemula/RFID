package org.apache.poi.sl.draw;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Line2D.Double;
import org.apache.poi.sl.usermodel.GroupShape;
import org.apache.poi.sl.usermodel.StrokeStyle;
import org.apache.poi.sl.usermodel.TableCell;
import org.apache.poi.sl.usermodel.TableShape;
import org.apache.poi.sl.usermodel.StrokeStyle.LineCompound;
import org.apache.poi.sl.usermodel.StrokeStyle.LineDash;
import org.apache.poi.sl.usermodel.TableCell.BorderEdge;
import org.apache.poi.util.Internal;

public class DrawTableShape extends DrawShape {
   @Internal
   public static final int borderSize = 2;

   public DrawTableShape(TableShape<?, ?> shape) {
      super(shape);
   }

   protected Drawable getGroupShape(Graphics2D graphics) {
      if (this.shape instanceof GroupShape) {
         DrawFactory df = DrawFactory.getInstance(graphics);
         return df.getDrawable((GroupShape)this.shape);
      } else {
         return null;
      }
   }

   public void applyTransform(Graphics2D graphics) {
      Drawable d = this.getGroupShape(graphics);
      if (d != null) {
         d.applyTransform(graphics);
      } else {
         super.applyTransform(graphics);
      }
   }

   public void draw(Graphics2D graphics) {
      Drawable d = this.getGroupShape(graphics);
      if (d != null) {
         d.draw(graphics);
      } else {
         TableShape<?, ?> ts = this.getShape();
         DrawPaint drawPaint = DrawFactory.getInstance(graphics).getPaint(ts);
         int rows = ts.getNumberOfRows();
         int cols = ts.getNumberOfColumns();

         for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
               TableCell<?, ?> tc = ts.getCell(row, col);
               if (tc != null && !tc.isMerged()) {
                  Paint fillPaint = drawPaint.getPaint(graphics, tc.getFillStyle().getPaint());
                  graphics.setPaint(fillPaint);
                  Rectangle2D cellAnc = tc.getAnchor();
                  graphics.fill(cellAnc);

                  for (BorderEdge edge : BorderEdge.values()) {
                     StrokeStyle stroke = tc.getBorderStyle(edge);
                     if (stroke != null) {
                        graphics.setStroke(getStroke(stroke));
                        Paint linePaint = drawPaint.getPaint(graphics, stroke.getPaint());
                        graphics.setPaint(linePaint);
                        double x = cellAnc.getX();
                        double y = cellAnc.getY();
                        double w = cellAnc.getWidth();
                        double h = cellAnc.getHeight();
                        Line2D line;
                        switch (edge) {
                           case bottom:
                           default:
                              line = new Double(x - 2.0, y + h, x + w + 2.0, y + h);
                              break;
                           case left:
                              line = new Double(x, y, x, y + h + 2.0);
                              break;
                           case right:
                              line = new Double(x + w, y, x + w, y + h + 2.0);
                              break;
                           case top:
                              line = new Double(x - 2.0, y, x + w + 2.0, y);
                        }

                        graphics.draw(line);
                     }
                  }
               }
            }
         }

         this.drawContent(graphics);
      }
   }

   public void drawContent(Graphics2D graphics) {
      Drawable d = this.getGroupShape(graphics);
      if (d != null) {
         d.drawContent(graphics);
      } else {
         TableShape<?, ?> ts = this.getShape();
         DrawFactory df = DrawFactory.getInstance(graphics);
         int rows = ts.getNumberOfRows();
         int cols = ts.getNumberOfColumns();

         for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
               TableCell<?, ?> tc = ts.getCell(row, col);
               if (tc != null) {
                  DrawTextShape dts = df.getDrawable(tc);
                  dts.drawContent(graphics);
               }
            }
         }
      }
   }

   protected TableShape<?, ?> getShape() {
      return (TableShape<?, ?>)this.shape;
   }

   public void setAllBorders(Object... args) {
      TableShape<?, ?> table = this.getShape();
      int rows = table.getNumberOfRows();
      int cols = table.getNumberOfColumns();
      BorderEdge[] edges = new BorderEdge[]{BorderEdge.top, BorderEdge.left, null, null};

      for (int row = 0; row < rows; row++) {
         for (int col = 0; col < cols; col++) {
            edges[2] = col == cols - 1 ? BorderEdge.right : null;
            edges[3] = row == rows - 1 ? BorderEdge.bottom : null;
            setEdges(table.getCell(row, col), edges, args);
         }
      }
   }

   public void setOutsideBorders(Object... args) {
      if (args.length != 0) {
         TableShape<?, ?> table = this.getShape();
         int rows = table.getNumberOfRows();
         int cols = table.getNumberOfColumns();
         BorderEdge[] edges = new BorderEdge[4];

         for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
               edges[0] = col == 0 ? BorderEdge.left : null;
               edges[1] = col == cols - 1 ? BorderEdge.right : null;
               edges[2] = row == 0 ? BorderEdge.top : null;
               edges[3] = row == rows - 1 ? BorderEdge.bottom : null;
               setEdges(table.getCell(row, col), edges, args);
            }
         }
      }
   }

   public void setInsideBorders(Object... args) {
      if (args.length != 0) {
         TableShape<?, ?> table = this.getShape();
         int rows = table.getNumberOfRows();
         int cols = table.getNumberOfColumns();
         BorderEdge[] edges = new BorderEdge[2];

         for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
               edges[0] = col > 0 && col < cols - 1 ? BorderEdge.right : null;
               edges[1] = row > 0 && row < rows - 1 ? BorderEdge.bottom : null;
               setEdges(table.getCell(row, col), edges, args);
            }
         }
      }
   }

   private static void setEdges(TableCell<?, ?> cell, BorderEdge[] edges, Object... args) {
      if (cell != null) {
         for (BorderEdge be : edges) {
            if (be != null) {
               if (args.length == 0) {
                  cell.removeBorder(be);
               } else {
                  for (Object o : args) {
                     if (o instanceof java.lang.Double) {
                        cell.setBorderWidth(be, (java.lang.Double)o);
                     } else if (o instanceof Color) {
                        cell.setBorderColor(be, (Color)o);
                     } else if (o instanceof LineDash) {
                        cell.setBorderDash(be, (LineDash)o);
                     } else if (o instanceof LineCompound) {
                        cell.setBorderCompound(be, (LineCompound)o);
                     }
                  }
               }
            }
         }
      }
   }
}

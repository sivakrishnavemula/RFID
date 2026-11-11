package org.apache.poi.sl.draw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Ellipse2D.Double;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.apache.poi.sl.draw.binding.CTCustomGeometry2D;
import org.apache.poi.sl.draw.geom.Context;
import org.apache.poi.sl.draw.geom.CustomGeometry;
import org.apache.poi.sl.draw.geom.Outline;
import org.apache.poi.sl.draw.geom.Path;
import org.apache.poi.sl.usermodel.LineDecoration;
import org.apache.poi.sl.usermodel.Shadow;
import org.apache.poi.sl.usermodel.SimpleShape;
import org.apache.poi.sl.usermodel.LineDecoration.DecorationShape;
import org.apache.poi.sl.usermodel.LineDecoration.DecorationSize;
import org.apache.poi.sl.usermodel.PaintStyle.SolidPaint;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.StaxHelper;
import org.apache.poi.util.Units;

public class DrawSimpleShape extends DrawShape {
   private static final double DECO_SIZE_POW = 1.5;

   public DrawSimpleShape(SimpleShape<?, ?> shape) {
      super(shape);
   }

   public void draw(Graphics2D graphics) {
      DrawPaint drawPaint = DrawFactory.getInstance(graphics).getPaint(this.getShape());
      Paint fill = drawPaint.getPaint(graphics, this.getShape().getFillStyle().getPaint());
      Paint line = drawPaint.getPaint(graphics, this.getShape().getStrokeStyle().getPaint());
      BasicStroke stroke = this.getStroke();
      graphics.setStroke(stroke);
      Collection<Outline> elems = this.computeOutlines(graphics);
      this.drawShadow(graphics, elems, fill, line);
      if (fill != null) {
         for (Outline o : elems) {
            if (o.getPath().isFilled()) {
               Paint fillMod = drawPaint.getPaint(graphics, this.getShape().getFillStyle().getPaint(), o.getPath().getFill());
               if (fillMod != null) {
                  graphics.setPaint(fillMod);
                  Shape s = o.getOutline();
                  graphics.setRenderingHint(Drawable.GRADIENT_SHAPE, s);
                  graphics.fill(s);
               }
            }
         }
      }

      this.drawContent(graphics);
      if (line != null) {
         graphics.setPaint(line);
         graphics.setStroke(stroke);

         for (Outline o : elems) {
            if (o.getPath().isStroked()) {
               Shape s = o.getOutline();
               graphics.setRenderingHint(Drawable.GRADIENT_SHAPE, s);
               graphics.draw(s);
            }
         }
      }

      this.drawDecoration(graphics, line, stroke);
   }

   protected void drawDecoration(Graphics2D graphics, Paint line, BasicStroke stroke) {
      if (line != null) {
         graphics.setPaint(line);
         List<Outline> lst = new ArrayList();
         LineDecoration deco = this.getShape().getLineDecoration();
         Outline head = this.getHeadDecoration(graphics, deco, stroke);
         if (head != null) {
            lst.add(head);
         }

         Outline tail = this.getTailDecoration(graphics, deco, stroke);
         if (tail != null) {
            lst.add(tail);
         }

         for (Outline o : lst) {
            Shape s = o.getOutline();
            Path p = o.getPath();
            graphics.setRenderingHint(Drawable.GRADIENT_SHAPE, s);
            if (p.isFilled()) {
               graphics.fill(s);
            }

            if (p.isStroked()) {
               graphics.draw(s);
            }
         }
      }
   }

   protected Outline getTailDecoration(Graphics2D graphics, LineDecoration deco, BasicStroke stroke) {
      if (deco != null && stroke != null) {
         DecorationSize tailLength = deco.getTailLength();
         if (tailLength == null) {
            tailLength = DecorationSize.MEDIUM;
         }

         DecorationSize tailWidth = deco.getTailWidth();
         if (tailWidth == null) {
            tailWidth = DecorationSize.MEDIUM;
         }

         double lineWidth = Math.max(2.5, stroke.getLineWidth());
         Rectangle2D anchor = getAnchor(graphics, this.getShape());
         double x2 = anchor.getX() + anchor.getWidth();
         double y2 = anchor.getY() + anchor.getHeight();
         double alpha = Math.atan(anchor.getHeight() / anchor.getWidth());
         AffineTransform at = new AffineTransform();
         Shape tailShape = null;
         Path p = null;
         double scaleY = Math.pow(1.5, tailWidth.ordinal() + 1.0);
         double scaleX = Math.pow(1.5, tailLength.ordinal() + 1.0);
         DecorationShape tailShapeEnum = deco.getTailShape();
         if (tailShapeEnum == null) {
            return null;
         }

         switch (tailShapeEnum) {
            case OVAL:
               p = new Path();
               tailShape = new Double(0.0, 0.0, lineWidth * scaleX, lineWidth * scaleY);
               Rectangle2D bounds = tailShape.getBounds2D();
               at.translate(x2 - bounds.getWidth() / 2.0, y2 - bounds.getHeight() / 2.0);
               at.rotate(alpha, bounds.getX() + bounds.getWidth() / 2.0, bounds.getY() + bounds.getHeight() / 2.0);
               break;
            case STEALTH:
            case ARROW:
               p = new Path(false, true);
               java.awt.geom.Path2D.Double arrow = new java.awt.geom.Path2D.Double();
               arrow.moveTo(-lineWidth * scaleX, -lineWidth * scaleY / 2.0);
               arrow.lineTo(0.0, 0.0);
               arrow.lineTo(-lineWidth * scaleX, lineWidth * scaleY / 2.0);
               tailShape = arrow;
               at.translate(x2, y2);
               at.rotate(alpha);
               break;
            case TRIANGLE:
               p = new Path();
               java.awt.geom.Path2D.Double triangle = new java.awt.geom.Path2D.Double();
               triangle.moveTo(-lineWidth * scaleX, -lineWidth * scaleY / 2.0);
               triangle.lineTo(0.0, 0.0);
               triangle.lineTo(-lineWidth * scaleX, lineWidth * scaleY / 2.0);
               triangle.closePath();
               tailShape = triangle;
               at.translate(x2, y2);
               at.rotate(alpha);
         }

         if (tailShape != null) {
            tailShape = at.createTransformedShape(tailShape);
         }

         return tailShape == null ? null : new Outline(tailShape, p);
      } else {
         return null;
      }
   }

   protected Outline getHeadDecoration(Graphics2D graphics, LineDecoration deco, BasicStroke stroke) {
      if (deco != null && stroke != null) {
         DecorationSize headLength = deco.getHeadLength();
         if (headLength == null) {
            headLength = DecorationSize.MEDIUM;
         }

         DecorationSize headWidth = deco.getHeadWidth();
         if (headWidth == null) {
            headWidth = DecorationSize.MEDIUM;
         }

         double lineWidth = Math.max(2.5, stroke.getLineWidth());
         Rectangle2D anchor = getAnchor(graphics, this.getShape());
         double x1 = anchor.getX();
         double y1 = anchor.getY();
         double alpha = Math.atan(anchor.getHeight() / anchor.getWidth());
         AffineTransform at = new AffineTransform();
         Shape headShape = null;
         Path p = null;
         double scaleY = Math.pow(1.5, headWidth.ordinal() + 1.0);
         double scaleX = Math.pow(1.5, headLength.ordinal() + 1.0);
         DecorationShape headShapeEnum = deco.getHeadShape();
         if (headShapeEnum == null) {
            return null;
         }

         switch (headShapeEnum) {
            case OVAL:
               p = new Path();
               headShape = new Double(0.0, 0.0, lineWidth * scaleX, lineWidth * scaleY);
               Rectangle2D bounds = headShape.getBounds2D();
               at.translate(x1 - bounds.getWidth() / 2.0, y1 - bounds.getHeight() / 2.0);
               at.rotate(alpha, bounds.getX() + bounds.getWidth() / 2.0, bounds.getY() + bounds.getHeight() / 2.0);
               break;
            case STEALTH:
            case ARROW:
               p = new Path(false, true);
               java.awt.geom.Path2D.Double arrow = new java.awt.geom.Path2D.Double();
               arrow.moveTo(lineWidth * scaleX, -lineWidth * scaleY / 2.0);
               arrow.lineTo(0.0, 0.0);
               arrow.lineTo(lineWidth * scaleX, lineWidth * scaleY / 2.0);
               headShape = arrow;
               at.translate(x1, y1);
               at.rotate(alpha);
               break;
            case TRIANGLE:
               p = new Path();
               java.awt.geom.Path2D.Double triangle = new java.awt.geom.Path2D.Double();
               triangle.moveTo(lineWidth * scaleX, -lineWidth * scaleY / 2.0);
               triangle.lineTo(0.0, 0.0);
               triangle.lineTo(lineWidth * scaleX, lineWidth * scaleY / 2.0);
               triangle.closePath();
               headShape = triangle;
               at.translate(x1, y1);
               at.rotate(alpha);
         }

         if (headShape != null) {
            headShape = at.createTransformedShape(headShape);
         }

         return headShape == null ? null : new Outline(headShape, p);
      } else {
         return null;
      }
   }

   public BasicStroke getStroke() {
      return getStroke(this.getShape().getStrokeStyle());
   }

   protected void drawShadow(Graphics2D graphics, Collection<Outline> outlines, Paint fill, Paint line) {
      Shadow<?, ?> shadow = this.getShape().getShadow();
      if (shadow != null && (fill != null || line != null)) {
         SolidPaint shadowPaint = shadow.getFillStyle();
         Color shadowColor = DrawPaint.applyColorTransform(shadowPaint.getSolidColor());
         double shapeRotation = this.getShape().getRotation();
         if (this.getShape().getFlipVertical()) {
            shapeRotation += 180.0;
         }

         double angle = shadow.getAngle() - shapeRotation;
         double dist = shadow.getDistance();
         double dx = dist * Math.cos(Math.toRadians(angle));
         double dy = dist * Math.sin(Math.toRadians(angle));
         graphics.translate(dx, dy);

         for (Outline o : outlines) {
            Shape s = o.getOutline();
            Path p = o.getPath();
            graphics.setRenderingHint(Drawable.GRADIENT_SHAPE, s);
            graphics.setPaint(shadowColor);
            if (fill != null && p.isFilled()) {
               graphics.fill(s);
            } else if (line != null && p.isStroked()) {
               graphics.draw(s);
            }
         }

         graphics.translate(-dx, -dy);
      }
   }

   protected static CustomGeometry getCustomGeometry(String name) {
      return getCustomGeometry(name, null);
   }

   protected static CustomGeometry getCustomGeometry(String name, Graphics2D graphics) {
      Map<String, CustomGeometry> presets = graphics == null ? null : (Map)graphics.getRenderingHint(Drawable.PRESET_GEOMETRY_CACHE);
      if (presets == null) {
         presets = new HashMap();
         if (graphics != null) {
            graphics.setRenderingHint(Drawable.PRESET_GEOMETRY_CACHE, presets);
         }

         String packageName = "org.apache.poi.sl.draw.binding";
         InputStream presetIS = Drawable.class.getResourceAsStream("presetShapeDefinitions.xml");
         EventFilter startElementFilter = new EventFilter() {
            public boolean accept(XMLEvent event) {
               return event.isStartElement();
            }
         };

         try {
            XMLInputFactory staxFactory = StaxHelper.newXMLInputFactory();
            XMLEventReader staxReader = staxFactory.createXMLEventReader(presetIS);
            XMLEventReader staxFiltRd = staxFactory.createFilteredReader(staxReader, startElementFilter);
            staxFiltRd.nextEvent();
            JAXBContext jaxbContext = JAXBContext.newInstance(packageName);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            while (staxFiltRd.peek() != null) {
               StartElement evRoot = (StartElement)staxFiltRd.peek();
               String cusName = evRoot.getName().getLocalPart();
               JAXBElement<CTCustomGeometry2D> el = unmarshaller.unmarshal(staxReader, CTCustomGeometry2D.class);
               CTCustomGeometry2D cusGeom = (CTCustomGeometry2D)el.getValue();
               presets.put(cusName, new CustomGeometry(cusGeom));
            }

            staxFiltRd.close();
            staxReader.close();
         } catch (Exception e) {
            throw new RuntimeException("Unable to load preset geometries.", e);
         } finally {
            IOUtils.closeQuietly(presetIS);
         }
      }

      return (CustomGeometry)presets.get(name);
   }

   protected Collection<Outline> computeOutlines(Graphics2D graphics) {
      SimpleShape<?, ?> sh = this.getShape();
      List<Outline> lst = new ArrayList();
      CustomGeometry geom = sh.getGeometry();
      if (geom == null) {
         return lst;
      }

      Rectangle2D anchor = getAnchor(graphics, sh);
      if (anchor == null) {
         return lst;
      }

      for (Path p : geom) {
         double w = p.getW();
         double h = p.getH();
         double scaleX = Units.toPoints(1L);
         double scaleY = scaleX;
         if (w == -1.0) {
            w = Units.toEMU(anchor.getWidth());
         } else {
            scaleX = anchor.getWidth() / w;
         }

         if (h == -1.0) {
            h = Units.toEMU(anchor.getHeight());
         } else {
            scaleY = anchor.getHeight() / h;
         }

         Rectangle2D pathAnchor = new java.awt.geom.Rectangle2D.Double(0.0, 0.0, w, h);
         Context ctx = new Context(geom, pathAnchor, sh);
         Shape gp = p.getPath(ctx);
         AffineTransform at = new AffineTransform();
         at.translate(anchor.getX(), anchor.getY());
         at.scale(scaleX, scaleY);
         Shape canvasShape = at.createTransformedShape(gp);
         lst.add(new Outline(canvasShape, p));
      }

      return lst;
   }

   protected SimpleShape<?, ?> getShape() {
      return (SimpleShape<?, ?>)this.shape;
   }
}

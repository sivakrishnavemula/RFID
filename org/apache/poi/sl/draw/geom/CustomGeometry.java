package org.apache.poi.sl.draw.geom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.sl.draw.binding.CTCustomGeometry2D;
import org.apache.poi.sl.draw.binding.CTGeomGuide;
import org.apache.poi.sl.draw.binding.CTGeomGuideList;
import org.apache.poi.sl.draw.binding.CTGeomRect;
import org.apache.poi.sl.draw.binding.CTPath2D;
import org.apache.poi.sl.draw.binding.CTPath2DList;

public class CustomGeometry implements Iterable<Path> {
   final List<Guide> adjusts = new ArrayList();
   final List<Guide> guides = new ArrayList();
   final List<Path> paths = new ArrayList();
   Path textBounds;

   public CustomGeometry(CTCustomGeometry2D geom) {
      CTGeomGuideList avLst = geom.getAvLst();
      if (avLst != null) {
         for (CTGeomGuide gd : avLst.getGd()) {
            this.adjusts.add(new AdjustValue(gd));
         }
      }

      CTGeomGuideList gdLst = geom.getGdLst();
      if (gdLst != null) {
         for (CTGeomGuide gd : gdLst.getGd()) {
            this.guides.add(new Guide(gd));
         }
      }

      CTPath2DList pathLst = geom.getPathLst();
      if (pathLst != null) {
         for (CTPath2D spPath : pathLst.getPath()) {
            this.paths.add(new Path(spPath));
         }
      }

      CTGeomRect rect = geom.getRect();
      if (rect != null) {
         this.textBounds = new Path();
         this.textBounds.addCommand(new MoveToCommand(rect.getL(), rect.getT()));
         this.textBounds.addCommand(new LineToCommand(rect.getR(), rect.getT()));
         this.textBounds.addCommand(new LineToCommand(rect.getR(), rect.getB()));
         this.textBounds.addCommand(new LineToCommand(rect.getL(), rect.getB()));
         this.textBounds.addCommand(new ClosePathCommand());
      }
   }

   public Iterator<Path> iterator() {
      return this.paths.iterator();
   }

   public Path getTextBounds() {
      return this.textBounds;
   }
}

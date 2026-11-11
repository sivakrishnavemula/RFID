package org.apache.poi.sl.draw.binding;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CT_Path2DList", propOrder = "path")
public class CTPath2DList {
   protected List<CTPath2D> path;

   public List<CTPath2D> getPath() {
      if (this.path == null) {
         this.path = new ArrayList();
      }

      return this.path;
   }

   public boolean isSetPath() {
      return this.path != null && !this.path.isEmpty();
   }

   public void unsetPath() {
      this.path = null;
   }
}

package org.apache.poi.hssf.usermodel;

import org.apache.poi.ddf.EscherChildAnchorRecord;
import org.apache.poi.ddf.EscherClientAnchorRecord;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.ss.usermodel.ChildAnchor;

public abstract class HSSFAnchor implements ChildAnchor {
   protected boolean _isHorizontallyFlipped;
   protected boolean _isVerticallyFlipped;

   public HSSFAnchor() {
      this.createEscherAnchor();
   }

   public HSSFAnchor(int dx1, int dy1, int dx2, int dy2) {
      this.createEscherAnchor();
      this.setDx1(dx1);
      this.setDy1(dy1);
      this.setDx2(dx2);
      this.setDy2(dy2);
   }

   public static HSSFAnchor createAnchorFromEscher(EscherContainerRecord container) {
      if (null != container.getChildById((short)-4081)) {
         return new HSSFChildAnchor((EscherChildAnchorRecord)container.getChildById((short)-4081));
      } else {
         return null != container.getChildById((short)-4080) ? new HSSFClientAnchor((EscherClientAnchorRecord)container.getChildById((short)-4080)) : null;
      }
   }

   public abstract boolean isHorizontallyFlipped();

   public abstract boolean isVerticallyFlipped();

   protected abstract EscherRecord getEscherAnchor();

   protected abstract void createEscherAnchor();
}

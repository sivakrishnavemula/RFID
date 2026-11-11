package org.apache.poi.sl.usermodel;

public interface TabStop {
   double getPositionInPoints();

   void setPositionInPoints(double var1);

   TabStop.TabStopType getType();

   void setType(TabStop.TabStopType var1);

   enum TabStopType {
      LEFT(0, 1),
      CENTER(1, 2),
      RIGHT(2, 3),
      DECIMAL(3, 4);

      public final int nativeId;
      public final int ooxmlId;

      TabStopType(int nativeId, int ooxmlId) {
         this.nativeId = nativeId;
         this.ooxmlId = ooxmlId;
      }

      public static TabStop.TabStopType fromNativeId(int nativeId) {
         for (TabStop.TabStopType tst : values()) {
            if (tst.nativeId == nativeId) {
               return tst;
            }
         }

         return null;
      }

      public static TabStop.TabStopType fromOoxmlId(int ooxmlId) {
         for (TabStop.TabStopType tst : values()) {
            if (tst.ooxmlId == ooxmlId) {
               return tst;
            }
         }

         return null;
      }
   }
}

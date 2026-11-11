package org.apache.poi.sl.usermodel;

public interface LineDecoration {
   LineDecoration.DecorationShape getHeadShape();

   LineDecoration.DecorationSize getHeadWidth();

   LineDecoration.DecorationSize getHeadLength();

   LineDecoration.DecorationShape getTailShape();

   LineDecoration.DecorationSize getTailWidth();

   LineDecoration.DecorationSize getTailLength();

   enum DecorationShape {
      NONE(0, 1),
      TRIANGLE(1, 2),
      STEALTH(2, 3),
      DIAMOND(3, 4),
      OVAL(4, 5),
      ARROW(5, 6);

      public final int nativeId;
      public final int ooxmlId;

      DecorationShape(int nativeId, int ooxmlId) {
         this.nativeId = nativeId;
         this.ooxmlId = ooxmlId;
      }

      public static LineDecoration.DecorationShape fromNativeId(int nativeId) {
         for (LineDecoration.DecorationShape ld : values()) {
            if (ld.nativeId == nativeId) {
               return ld;
            }
         }

         return null;
      }

      public static LineDecoration.DecorationShape fromOoxmlId(int ooxmlId) {
         for (LineDecoration.DecorationShape ds : values()) {
            if (ds.ooxmlId == ooxmlId) {
               return ds;
            }
         }

         return null;
      }
   }

   enum DecorationSize {
      SMALL(0, 1),
      MEDIUM(1, 2),
      LARGE(2, 3);

      public final int nativeId;
      public final int ooxmlId;

      DecorationSize(int nativeId, int ooxmlId) {
         this.nativeId = nativeId;
         this.ooxmlId = ooxmlId;
      }

      public static LineDecoration.DecorationSize fromNativeId(int nativeId) {
         for (LineDecoration.DecorationSize ld : values()) {
            if (ld.nativeId == nativeId) {
               return ld;
            }
         }

         return null;
      }

      public static LineDecoration.DecorationSize fromOoxmlId(int ooxmlId) {
         for (LineDecoration.DecorationSize ds : values()) {
            if (ds.ooxmlId == ooxmlId) {
               return ds;
            }
         }

         return null;
      }
   }
}

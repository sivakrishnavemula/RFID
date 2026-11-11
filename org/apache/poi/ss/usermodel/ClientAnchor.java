package org.apache.poi.ss.usermodel;

import org.apache.poi.util.Internal;

public interface ClientAnchor {
   short getCol1();

   void setCol1(int var1);

   short getCol2();

   void setCol2(int var1);

   int getRow1();

   void setRow1(int var1);

   int getRow2();

   void setRow2(int var1);

   int getDx1();

   void setDx1(int var1);

   int getDy1();

   void setDy1(int var1);

   int getDy2();

   void setDy2(int var1);

   int getDx2();

   void setDx2(int var1);

   void setAnchorType(ClientAnchor.AnchorType var1);

   ClientAnchor.AnchorType getAnchorType();

   enum AnchorType {
      MOVE_AND_RESIZE(0),
      DONT_MOVE_DO_RESIZE(1),
      MOVE_DONT_RESIZE(2),
      DONT_MOVE_AND_RESIZE(3);

      public final short value;

      AnchorType(int value) {
         this.value = (short)value;
      }

      @Internal
      public static ClientAnchor.AnchorType byId(int value) {
         return values()[value];
      }
   }
}

package org.apache.poi.ss.usermodel;

public interface Drawing<T extends Shape> extends ShapeContainer<T> {
   Picture createPicture(ClientAnchor var1, int var2);

   Comment createCellComment(ClientAnchor var1);

   ClientAnchor createAnchor(int var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8);

   ObjectData createObjectData(ClientAnchor var1, int var2, int var3);
}

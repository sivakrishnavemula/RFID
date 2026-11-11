package org.apache.poi.sl.usermodel;

import java.util.List;

public interface ShapeContainer<S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> extends Iterable<S> {
   List<S> getShapes();

   void addShape(S var1);

   boolean removeShape(S var1);

   AutoShape<S, P> createAutoShape();

   FreeformShape<S, P> createFreeform();

   TextBox<S, P> createTextBox();

   ConnectorShape<S, P> createConnector();

   GroupShape<S, P> createGroup();

   PictureShape<S, P> createPicture(PictureData var1);

   TableShape<S, P> createTable(int var1, int var2);

   ObjectShape<?, ?> createOleShape(PictureData var1);
}

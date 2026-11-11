package org.apache.poi.sl.usermodel;

import java.awt.Insets;

public interface PictureShape<S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> extends SimpleShape<S, P> {
   PictureData getPictureData();

   Insets getClipping();
}

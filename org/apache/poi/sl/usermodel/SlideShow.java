package org.apache.poi.sl.usermodel;

import java.awt.Dimension;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import org.apache.poi.extractor.POITextExtractor;
import org.apache.poi.sl.usermodel.PictureData.PictureType;

public interface SlideShow<S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> extends Closeable {
   Slide<S, P> createSlide() throws IOException;

   List<? extends Slide<S, P>> getSlides();

   MasterSheet<S, P> createMasterSheet() throws IOException;

   List<? extends MasterSheet<S, P>> getSlideMasters();

   Resources getResources();

   Dimension getPageSize();

   void setPageSize(Dimension var1);

   List<? extends PictureData> getPictureData();

   PictureData addPicture(byte[] var1, PictureType var2) throws IOException;

   PictureData addPicture(InputStream var1, PictureType var2) throws IOException;

   PictureData addPicture(File var1, PictureType var2) throws IOException;

   PictureData findPictureData(byte[] var1);

   void write(OutputStream var1) throws IOException;

   POITextExtractor getMetadataTextExtractor();

   Object getPersistDocument();
}

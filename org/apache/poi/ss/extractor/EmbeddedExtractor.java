package org.apache.poi.ss.extractor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.hpsf.ClassID;
import org.apache.poi.hpsf.ClassIDPredefined;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.poifs.filesystem.Ole10Native;
import org.apache.poi.poifs.filesystem.Ole10NativeException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.ObjectData;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.PictureData;
import org.apache.poi.ss.usermodel.Shape;
import org.apache.poi.ss.usermodel.ShapeContainer;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.StringUtil;

public class EmbeddedExtractor implements Iterable<EmbeddedExtractor> {
   private static final POILogger LOG = POILogFactory.getLogger(EmbeddedExtractor.class);
   private static final int MAX_RECORD_LENGTH = 1000000;
   private static final String CONTENT_TYPE_BYTES = "binary/octet-stream";
   private static final String CONTENT_TYPE_PDF = "application/pdf";
   private static final String CONTENT_TYPE_DOC = "application/msword";
   private static final String CONTENT_TYPE_XLS = "application/vnd.ms-excel";

   public Iterator<EmbeddedExtractor> iterator() {
      EmbeddedExtractor[] ee = new EmbeddedExtractor[]{
         new EmbeddedExtractor.Ole10Extractor(),
         new EmbeddedExtractor.PdfExtractor(),
         new EmbeddedExtractor.BiffExtractor(),
         new EmbeddedExtractor.OOXMLExtractor(),
         new EmbeddedExtractor.FsExtractor()
      };
      return Arrays.asList(ee).iterator();
   }

   public EmbeddedData extractOne(DirectoryNode src) throws IOException {
      for (EmbeddedExtractor ee : this) {
         if (ee.canExtract(src)) {
            return ee.extract(src);
         }
      }

      return null;
   }

   public EmbeddedData extractOne(Picture src) throws IOException {
      for (EmbeddedExtractor ee : this) {
         if (ee.canExtract(src)) {
            return ee.extract(src);
         }
      }

      return null;
   }

   public List<EmbeddedData> extractAll(Sheet sheet) throws IOException {
      Drawing<?> patriarch = sheet.getDrawingPatriarch();
      if (null == patriarch) {
         return Collections.emptyList();
      }

      List<EmbeddedData> embeddings = new ArrayList();
      this.extractAll(patriarch, embeddings);
      return embeddings;
   }

   protected void extractAll(ShapeContainer<?> parent, List<EmbeddedData> embeddings) throws IOException {
      for (Shape shape : parent) {
         EmbeddedData data = null;
         if (shape instanceof ObjectData) {
            ObjectData od = (ObjectData)shape;

            try {
               if (od.hasDirectoryEntry()) {
                  data = this.extractOne((DirectoryNode)od.getDirectory());
               } else {
                  data = new EmbeddedData(od.getFileName(), od.getObjectData(), od.getContentType());
               }
            } catch (Exception e) {
               LOG.log(5, new Object[]{"Entry not found / readable - ignoring OLE embedding", e});
            }
         } else if (shape instanceof Picture) {
            data = this.extractOne((Picture)shape);
         } else if (shape instanceof ShapeContainer) {
            this.extractAll((ShapeContainer<?>)shape, embeddings);
         }

         if (data != null) {
            data.setShape(shape);
            String filename = data.getFilename();
            String extension = filename != null && filename.lastIndexOf(46) != -1 ? filename.substring(filename.lastIndexOf(46)) : ".bin";
            if (filename == null || filename.isEmpty() || filename.startsWith("MBD") || filename.startsWith("Root Entry")) {
               filename = shape.getShapeName();
               if (filename != null) {
                  filename = filename + extension;
               }
            }

            if (filename == null || filename.isEmpty()) {
               filename = "picture_" + embeddings.size() + extension;
            }

            filename = filename.trim();
            data.setFilename(filename);
            embeddings.add(data);
         }
      }
   }

   public boolean canExtract(DirectoryNode source) {
      return false;
   }

   public boolean canExtract(Picture source) {
      return false;
   }

   protected EmbeddedData extract(DirectoryNode dn) throws IOException {
      assert this.canExtract(dn);
      ByteArrayOutputStream bos = new ByteArrayOutputStream(20000);
      POIFSFileSystem dest = new POIFSFileSystem();
      Throwable var4 = null;

      try {
         copyNodes(dn, dest.getRoot());
         dest.writeFilesystem(bos);
      } catch (Throwable var13) {
         var4 = var13;
         throw var13;
      } finally {
         if (dest != null) {
            if (var4 != null) {
               try {
                  dest.close();
               } catch (Throwable var12) {
                  var4.addSuppressed(var12);
               }
            } else {
               dest.close();
            }
         }
      }

      return new EmbeddedData(dn.getName(), bos.toByteArray(), "binary/octet-stream");
   }

   protected EmbeddedData extract(Picture source) throws IOException {
      return null;
   }

   protected static void copyNodes(DirectoryNode src, DirectoryNode dest) throws IOException {
      for (Entry e : src) {
         if (e instanceof DirectoryNode) {
            DirectoryNode srcDir = (DirectoryNode)e;
            DirectoryNode destDir = (DirectoryNode)dest.createDirectory(srcDir.getName());
            destDir.setStorageClsid(srcDir.getStorageClsid());
            copyNodes(srcDir, destDir);
         } else {
            InputStream is = src.createDocumentInputStream(e);
            Throwable var17 = null;

            try {
               dest.createDocument(e.getName(), is);
            } catch (Throwable var14) {
               var17 = var14;
               throw var14;
            } finally {
               if (is != null) {
                  if (var17 != null) {
                     try {
                        is.close();
                     } catch (Throwable var13) {
                        var17.addSuppressed(var13);
                     }
                  } else {
                     is.close();
                  }
               }
            }
         }
      }
   }

   private static int indexOf(byte[] data, int offset, byte[] pattern) {
      int[] failure = computeFailure(pattern);
      int j = 0;
      if (data.length == 0) {
         return -1;
      }

      for (int i = offset; i < data.length; i++) {
         while (j > 0 && pattern[j] != data[i]) {
            j = failure[j - 1];
         }

         if (pattern[j] == data[i]) {
            j++;
         }

         if (j == pattern.length) {
            return i - pattern.length + 1;
         }
      }

      return -1;
   }

   private static int[] computeFailure(byte[] pattern) {
      int[] failure = new int[pattern.length];
      int j = 0;

      for (int i = 1; i < pattern.length; i++) {
         while (j > 0 && pattern[j] != pattern[i]) {
            j = failure[j - 1];
         }

         if (pattern[j] == pattern[i]) {
            j++;
         }

         failure[i] = j;
      }

      return failure;
   }

   static class BiffExtractor extends EmbeddedExtractor {
      @Override
      public boolean canExtract(DirectoryNode dn) {
         return this.canExtractExcel(dn) || this.canExtractWord(dn);
      }

      protected boolean canExtractExcel(DirectoryNode dn) {
         ClassIDPredefined clsId = ClassIDPredefined.lookup(dn.getStorageClsid());
         return ClassIDPredefined.EXCEL_V7 == clsId || ClassIDPredefined.EXCEL_V8 == clsId || dn.hasEntry("Workbook");
      }

      protected boolean canExtractWord(DirectoryNode dn) {
         ClassIDPredefined clsId = ClassIDPredefined.lookup(dn.getStorageClsid());
         return ClassIDPredefined.WORD_V7 == clsId || ClassIDPredefined.WORD_V8 == clsId || dn.hasEntry("WordDocument");
      }

      @Override
      public EmbeddedData extract(DirectoryNode dn) throws IOException {
         EmbeddedData ed = super.extract(dn);
         if (this.canExtractExcel(dn)) {
            ed.setFilename(dn.getName() + ".xls");
            ed.setContentType("application/vnd.ms-excel");
         } else if (this.canExtractWord(dn)) {
            ed.setFilename(dn.getName() + ".doc");
            ed.setContentType("application/msword");
         }

         return ed;
      }
   }

   static class FsExtractor extends EmbeddedExtractor {
      @Override
      public boolean canExtract(DirectoryNode dn) {
         return true;
      }

      @Override
      public EmbeddedData extract(DirectoryNode dn) throws IOException {
         EmbeddedData ed = super.extract(dn);
         ed.setFilename(dn.getName() + ".ole");
         return ed;
      }
   }

   static class OOXMLExtractor extends EmbeddedExtractor {
      @Override
      public boolean canExtract(DirectoryNode dn) {
         return dn.hasEntry("package");
      }

      @Override
      public EmbeddedData extract(DirectoryNode dn) throws IOException {
         ClassIDPredefined clsId = ClassIDPredefined.lookup(dn.getStorageClsid());
         String contentType = null;
         String ext = null;
         if (clsId != null) {
            contentType = clsId.getContentType();
            ext = clsId.getFileExtension();
         }

         if (contentType == null || ext == null) {
            contentType = "application/zip";
            ext = ".zip";
         }

         DocumentInputStream dis = dn.createDocumentInputStream("package");
         byte[] data = IOUtils.toByteArray(dis);
         dis.close();
         return new EmbeddedData(dn.getName() + ext, data, contentType);
      }
   }

   public static class Ole10Extractor extends EmbeddedExtractor {
      @Override
      public boolean canExtract(DirectoryNode dn) {
         ClassID clsId = dn.getStorageClsid();
         return ClassIDPredefined.lookup(clsId) == ClassIDPredefined.OLE_V1_PACKAGE;
      }

      @Override
      public EmbeddedData extract(DirectoryNode dn) throws IOException {
         try {
            Ole10Native ole10 = Ole10Native.createFromEmbeddedOleObject(dn);
            return new EmbeddedData(ole10.getFileName(), ole10.getDataBuffer(), "binary/octet-stream");
         } catch (Ole10NativeException e) {
            throw new IOException(e);
         }
      }
   }

   static class PdfExtractor extends EmbeddedExtractor {
      static ClassID PdfClassID = new ClassID("{B801CA65-A1FC-11D0-85AD-444553540000}");

      @Override
      public boolean canExtract(DirectoryNode dn) {
         ClassID clsId = dn.getStorageClsid();
         return PdfClassID.equals(clsId) || dn.hasEntry("CONTENTS");
      }

      @Override
      public EmbeddedData extract(DirectoryNode dn) throws IOException {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         Throwable var3 = null;

         try {
            InputStream is = dn.createDocumentInputStream("CONTENTS");
            Throwable var5 = null;

            try {
               IOUtils.copy(is, bos);
               return new EmbeddedData(dn.getName() + ".pdf", bos.toByteArray(), "application/pdf");
            } catch (Throwable var29) {
               var5 = var29;
               throw var29;
            } finally {
               if (is != null) {
                  if (var5 != null) {
                     try {
                        is.close();
                     } catch (Throwable var28) {
                        var5.addSuppressed(var28);
                     }
                  } else {
                     is.close();
                  }
               }
            }
         } catch (Throwable var31) {
            var3 = var31;
            throw var31;
         } finally {
            if (bos != null) {
               if (var3 != null) {
                  try {
                     bos.close();
                  } catch (Throwable var27) {
                     var3.addSuppressed(var27);
                  }
               } else {
                  bos.close();
               }
            }
         }
      }

      @Override
      public boolean canExtract(Picture source) {
         PictureData pd = source.getPictureData();
         return pd != null && pd.getPictureType() == 2;
      }

      @Override
      protected EmbeddedData extract(Picture source) throws IOException {
         PictureData pd = source.getPictureData();
         if (pd != null && pd.getPictureType() == 2) {
            byte[] pictureBytes = pd.getData();
            int idxStart = EmbeddedExtractor.indexOf(pictureBytes, 0, "%PDF-".getBytes(LocaleUtil.CHARSET_1252));
            if (idxStart == -1) {
               return null;
            }

            int idxEnd = EmbeddedExtractor.indexOf(pictureBytes, idxStart, "%%EOF".getBytes(LocaleUtil.CHARSET_1252));
            if (idxEnd == -1) {
               return null;
            }

            int pictureBytesLen = idxEnd - idxStart + 6;
            byte[] pdfBytes = IOUtils.safelyAllocate(pictureBytesLen, 1000000);
            System.arraycopy(pictureBytes, idxStart, pdfBytes, 0, pictureBytesLen);
            String filename = source.getShapeName().trim();
            if (!StringUtil.endsWithIgnoreCase(filename, ".pdf")) {
               filename = filename + ".pdf";
            }

            return new EmbeddedData(filename, pdfBytes, "application/pdf");
         } else {
            return null;
         }
      }
   }
}

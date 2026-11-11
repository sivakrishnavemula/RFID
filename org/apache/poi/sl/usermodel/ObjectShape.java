package org.apache.poi.sl.usermodel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.sl.usermodel.ObjectMetaData.Application;
import org.apache.poi.util.IOUtils;

public interface ObjectShape<S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> extends Shape<S, P>, PlaceableShape<S, P> {
   PictureData getPictureData();

   String getProgId();

   String getFullName();

   OutputStream updateObjectData(Application var1, ObjectMetaData var2) throws IOException;

   default InputStream readObjectData() throws IOException {
      String progId = this.getProgId();
      if (progId == null) {
         throw new IllegalStateException(
            "Ole object hasn't been initialized or provided in the source xml. use updateObjectData() first or check the corresponding slideXXX.xml"
         );
      }

      Application app = Application.lookup(progId);
      ByteArrayOutputStream bos = new ByteArrayOutputStream(50000);
      InputStream is = FileMagic.prepareToCheckMagic(this.readObjectDataRaw());
      Throwable var5 = null;

      try {
         FileMagic fm = FileMagic.valueOf(is);
         if (fm == FileMagic.OLE2) {
            POIFSFileSystem poifs = new POIFSFileSystem(is);
            Throwable var8 = null;

            try {
               String[] names = new String[]{app == null ? null : app.getMetaData().getOleEntry(), "Package", "Contents", "CONTENTS", "CONTENTSV30"};
               DirectoryNode root = poifs.getRoot();
               String entryName = null;

               for (String n : names) {
                  if (root.hasEntry(n)) {
                     entryName = n;
                     break;
                  }
               }

               if (entryName == null) {
                  poifs.writeFilesystem(bos);
               } else {
                  InputStream is2 = poifs.createDocumentInputStream(entryName);
                  Throwable var65 = null;

                  try {
                     IOUtils.copy(is2, bos);
                  } catch (Throwable var58) {
                     var65 = var58;
                     throw var58;
                  } finally {
                     if (is2 != null) {
                        if (var65 != null) {
                           try {
                              is2.close();
                           } catch (Throwable var57) {
                              var65.addSuppressed(var57);
                           }
                        } else {
                           is2.close();
                        }
                     }
                  }
               }
            } catch (Throwable var60) {
               var8 = var60;
               throw var60;
            } finally {
               if (poifs != null) {
                  if (var8 != null) {
                     try {
                        poifs.close();
                     } catch (Throwable var56) {
                        var8.addSuppressed(var56);
                     }
                  } else {
                     poifs.close();
                  }
               }
            }
         } else {
            IOUtils.copy(is, bos);
         }
      } catch (Throwable var62) {
         var5 = var62;
         throw var62;
      } finally {
         if (is != null) {
            if (var5 != null) {
               try {
                  is.close();
               } catch (Throwable var55) {
                  var5.addSuppressed(var55);
               }
            } else {
               is.close();
            }
         }
      }

      return new ByteArrayInputStream(bos.toByteArray());
   }

   default InputStream readObjectDataRaw() throws IOException {
      return this.getObjectData().getInputStream();
   }

   ObjectData getObjectData();
}

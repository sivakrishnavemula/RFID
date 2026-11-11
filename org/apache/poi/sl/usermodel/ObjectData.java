package org.apache.poi.sl.usermodel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

public interface ObjectData {
   InputStream getInputStream() throws IOException;

   OutputStream getOutputStream() throws IOException;

   default byte[] getBytes() throws IOException {
      InputStream is = this.getInputStream();
      Throwable var2 = null;

      try {
         return IOUtils.toByteArray(is);
      } catch (Throwable var12) {
         var2 = var12;
         throw var12;
      } finally {
         if (is != null) {
            if (var2 != null) {
               try {
                  is.close();
               } catch (Throwable var11) {
                  var2.addSuppressed(var11);
               }
            } else {
               is.close();
            }
         }
      }
   }

   default boolean hasDirectoryEntry() {
      try {
         InputStream is = FileMagic.prepareToCheckMagic(this.getInputStream());
         Throwable var17 = null;

         try {
            FileMagic fm = FileMagic.valueOf(is);
            return fm == FileMagic.OLE2;
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
      } catch (IOException e) {
         POILogger LOG = POILogFactory.getLogger(ObjectData.class);
         LOG.log(5, new Object[]{"Can't determine filemagic of ole stream", e});
         return false;
      }
   }

   default DirectoryEntry getDirectory() throws IOException {
      InputStream is = this.getInputStream();
      Throwable var2 = null;

      try {
         return new POIFSFileSystem(is).getRoot();
      } catch (Throwable var12) {
         var2 = var12;
         throw var12;
      } finally {
         if (is != null) {
            if (var2 != null) {
               try {
                  is.close();
               } catch (Throwable var11) {
                  var2.addSuppressed(var11);
               }
            } else {
               is.close();
            }
         }
      }
   }

   String getOLE2ClassName();

   String getFileName();
}

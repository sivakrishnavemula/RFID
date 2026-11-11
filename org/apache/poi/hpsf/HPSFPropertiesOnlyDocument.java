package org.apache.poi.hpsf;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.POIDocument;
import org.apache.poi.poifs.filesystem.EntryUtils;
import org.apache.poi.poifs.filesystem.FilteringDirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public class HPSFPropertiesOnlyDocument extends POIDocument {
   public HPSFPropertiesOnlyDocument(POIFSFileSystem fs) {
      super(fs);
   }

   public void write() throws IOException {
      POIFSFileSystem fs = this.getDirectory().getFileSystem();
      this.validateInPlaceWritePossible();
      this.writeProperties(fs, null);
      fs.writeFilesystem();
   }

   public void write(File newFile) throws IOException {
      POIFSFileSystem fs = POIFSFileSystem.create(newFile);
      Throwable var3 = null;

      try {
         this.write(fs);
         fs.writeFilesystem();
      } catch (Throwable var12) {
         var3 = var12;
         throw var12;
      } finally {
         if (fs != null) {
            if (var3 != null) {
               try {
                  fs.close();
               } catch (Throwable var11) {
                  var3.addSuppressed(var11);
               }
            } else {
               fs.close();
            }
         }
      }
   }

   public void write(OutputStream out) throws IOException {
      POIFSFileSystem fs = new POIFSFileSystem();
      Throwable var3 = null;

      try {
         this.write(fs);
         fs.writeFilesystem(out);
      } catch (Throwable var12) {
         var3 = var12;
         throw var12;
      } finally {
         if (fs != null) {
            if (var3 != null) {
               try {
                  fs.close();
               } catch (Throwable var11) {
                  var3.addSuppressed(var11);
               }
            } else {
               fs.close();
            }
         }
      }
   }

   private void write(POIFSFileSystem fs) throws IOException {
      List<String> excepts = new ArrayList(2);
      this.writeProperties(fs, excepts);
      FilteringDirectoryNode src = new FilteringDirectoryNode(this.getDirectory(), excepts);
      FilteringDirectoryNode dest = new FilteringDirectoryNode(fs.getRoot(), excepts);
      EntryUtils.copyNodes(src, dest);
   }
}

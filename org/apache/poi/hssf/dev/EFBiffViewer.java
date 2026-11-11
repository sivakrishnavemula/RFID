package org.apache.poi.hssf.dev;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public class EFBiffViewer {
   String file;

   public void run() throws IOException {
      POIFSFileSystem fs = new POIFSFileSystem(new File(this.file), true);
      Throwable var2 = null;

      try {
         InputStream din = BiffViewer.getPOIFSInputStream(fs);
         Throwable var4 = null;

         try {
            HSSFRequest req = new HSSFRequest();
            req.addListenerForAllRecords(System.out::println);
            HSSFEventFactory factory = new HSSFEventFactory();
            factory.processEvents(req, din);
         } catch (Throwable var28) {
            var4 = var28;
            throw var28;
         } finally {
            if (din != null) {
               if (var4 != null) {
                  try {
                     din.close();
                  } catch (Throwable var27) {
                     var4.addSuppressed(var27);
                  }
               } else {
                  din.close();
               }
            }
         }
      } catch (Throwable var30) {
         var2 = var30;
         throw var30;
      } finally {
         if (fs != null) {
            if (var2 != null) {
               try {
                  fs.close();
               } catch (Throwable var26) {
                  var2.addSuppressed(var26);
               }
            } else {
               fs.close();
            }
         }
      }
   }

   public void setFile(String file) {
      this.file = file;
   }

   public static void main(String[] args) throws IOException {
      if (args.length == 1 && !args[0].equals("--help")) {
         EFBiffViewer viewer = new EFBiffViewer();
         viewer.setFile(args[0]);
         viewer.run();
      } else {
         System.out.println("EFBiffViewer");
         System.out.println("Outputs biffview of records based on HSSFEventFactory");
         System.out.println("usage: java org.apache.poi.hssf.dev.EBBiffViewer filename");
      }
   }
}

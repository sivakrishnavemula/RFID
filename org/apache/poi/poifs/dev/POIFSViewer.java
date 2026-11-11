package org.apache.poi.poifs.dev;

import java.io.File;
import java.io.IOException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public final class POIFSViewer {
   private POIFSViewer() {
   }

   public static void main(String[] args) {
      if (args.length == 0) {
         System.err.println("Must specify at least one file to view");
         System.exit(1);
      }

      boolean printNames = args.length > 1;

      for (String arg : args) {
         viewFile(arg, printNames);
      }
   }

   private static void viewFile(String filename, boolean printName) {
      if (printName) {
         StringBuffer flowerbox = new StringBuffer();
         flowerbox.append(".");

         for (int j = 0; j < filename.length(); j++) {
            flowerbox.append("-");
         }

         flowerbox.append(".");
         System.out.println(flowerbox);
         System.out.println("|" + filename + "|");
         System.out.println(flowerbox);
      }

      try {
         POIFSFileSystem fs = new POIFSFileSystem(new File(filename));

         for (String s : POIFSViewEngine.inspectViewable(fs, true, 0, "  ")) {
            System.out.print(s);
         }

         fs.close();
      } catch (IOException e) {
         System.out.println(e.getMessage());
      }
   }
}

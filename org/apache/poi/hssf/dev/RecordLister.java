package org.apache.poi.hssf.dev;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.RecordFactory;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public class RecordLister {
   String file;

   public void run() throws IOException {
      POIFSFileSystem fs = new POIFSFileSystem(new File(this.file), true);
      Throwable var2 = null;

      try {
         InputStream din = BiffViewer.getPOIFSInputStream(fs);
         Throwable var4 = null;

         try {
            RecordInputStream rinp = new RecordInputStream(din);

            while (rinp.hasNextRecord()) {
               int sid = rinp.getNextSid();
               rinp.nextRecord();
               int size = rinp.available();
               Class<? extends Record> clz = RecordFactory.getRecordClass(sid);
               System.out.print(formatSID(sid) + " - " + formatSize(size) + " bytes");
               if (clz != null) {
                  System.out.print("  \t");
                  System.out.print(clz.getName().replace("org.apache.poi.hssf.record.", ""));
               }

               System.out.println();
               byte[] data = rinp.readRemainder();
               if (data.length > 0) {
                  System.out.print("   ");
                  System.out.println(formatData(data));
               }
            }
         } catch (Throwable var31) {
            var4 = var31;
            throw var31;
         } finally {
            if (din != null) {
               if (var4 != null) {
                  try {
                     din.close();
                  } catch (Throwable var30) {
                     var4.addSuppressed(var30);
                  }
               } else {
                  din.close();
               }
            }
         }
      } catch (Throwable var33) {
         var2 = var33;
         throw var33;
      } finally {
         if (fs != null) {
            if (var2 != null) {
               try {
                  fs.close();
               } catch (Throwable var29) {
                  var2.addSuppressed(var29);
               }
            } else {
               fs.close();
            }
         }
      }
   }

   private static String formatSID(int sid) {
      String hex = Integer.toHexString(sid);
      String dec = Integer.toString(sid);
      StringBuilder s = new StringBuilder();
      s.append("0x");

      for (int i = hex.length(); i < 4; i++) {
         s.append('0');
      }

      s.append(hex);
      s.append(" (");

      for (int i = dec.length(); i < 4; i++) {
         s.append('0');
      }

      s.append(dec);
      s.append(")");
      return s.toString();
   }

   private static String formatSize(int size) {
      String hex = Integer.toHexString(size);
      String dec = Integer.toString(size);
      StringBuilder s = new StringBuilder();

      for (int i = hex.length(); i < 3; i++) {
         s.append('0');
      }

      s.append(hex);
      s.append(" (");

      for (int i = dec.length(); i < 3; i++) {
         s.append('0');
      }

      s.append(dec);
      s.append(")");
      return s.toString();
   }

   private static String formatData(byte[] data) {
      if (data != null && data.length != 0) {
         StringBuilder s = new StringBuilder();
         if (data.length > 9) {
            s.append(byteToHex(data[0]));
            s.append(' ');
            s.append(byteToHex(data[1]));
            s.append(' ');
            s.append(byteToHex(data[2]));
            s.append(' ');
            s.append(byteToHex(data[3]));
            s.append(' ');
            s.append(" .... ");
            s.append(' ');
            s.append(byteToHex(data[data.length - 4]));
            s.append(' ');
            s.append(byteToHex(data[data.length - 3]));
            s.append(' ');
            s.append(byteToHex(data[data.length - 2]));
            s.append(' ');
            s.append(byteToHex(data[data.length - 1]));
         } else {
            for (byte aData : data) {
               s.append(byteToHex(aData));
               s.append(' ');
            }
         }

         return s.toString();
      } else {
         return "";
      }
   }

   private static String byteToHex(byte b) {
      int i = b;
      if (i < 0) {
         i += 256;
      }

      String s = Integer.toHexString(i);
      return i < 16 ? "0" + s : s;
   }

   public void setFile(String file) {
      this.file = file;
   }

   public static void main(String[] args) throws IOException {
      if (args.length == 1 && !args[0].equals("--help")) {
         RecordLister viewer = new RecordLister();
         viewer.setFile(args[0]);
         viewer.run();
      } else {
         System.out.println("RecordLister");
         System.out.println("Outputs the summary of the records in file order");
         System.out.println("usage: java org.apache.poi.hssf.dev.RecordLister filename");
      }
   }
}

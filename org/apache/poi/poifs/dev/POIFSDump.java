package org.apache.poi.poifs.dev;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.DocumentNode;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.poifs.filesystem.POIFSStream;
import org.apache.poi.poifs.property.PropertyTable;
import org.apache.poi.poifs.storage.HeaderBlock;
import org.apache.poi.util.IOUtils;

public final class POIFSDump {
   private static final int MAX_RECORD_LENGTH = 100000;

   private POIFSDump() {
   }

   public static void main(String[] args) throws IOException {
      if (args.length == 0) {
         System.err.println("Must specify at least one file to dump");
         System.exit(1);
      }

      boolean dumpProps = false;
      boolean dumpMini = false;

      for (String filename : args) {
         if (filename.equalsIgnoreCase("-dumprops") || filename.equalsIgnoreCase("-dump-props") || filename.equalsIgnoreCase("-dump-properties")) {
            dumpProps = true;
         } else if (!filename.equalsIgnoreCase("-dumpmini")
            && !filename.equalsIgnoreCase("-dump-mini")
            && !filename.equalsIgnoreCase("-dump-ministream")
            && !filename.equalsIgnoreCase("-dump-mini-stream")) {
            System.out.println("Dumping " + filename);
            FileInputStream is = new FileInputStream(filename);
            Throwable var8 = null;

            try {
               POIFSFileSystem fs = new POIFSFileSystem(is);
               Throwable var10 = null;

               try {
                  DirectoryEntry root = fs.getRoot();
                  String filenameWithoutPath = new File(filename).getName();
                  File dumpDir = new File(filenameWithoutPath + "_dump");
                  File file = new File(dumpDir, root.getName());
                  if (!file.exists() && !file.mkdirs()) {
                     throw new IOException("Could not create directory " + file);
                  }

                  dump(root, file);
                  if (dumpProps) {
                     HeaderBlock header = fs.getHeaderBlock();
                     dump(fs, header.getPropertyStart(), "properties", file);
                  }

                  if (dumpMini) {
                     PropertyTable props = fs.getPropertyTable();
                     int startBlock = props.getRoot().getStartBlock();
                     if (startBlock == -2) {
                        System.err.println("No Mini Stream in file");
                     } else {
                        dump(fs, startBlock, "mini-stream", file);
                     }
                  }
               } catch (Throwable var38) {
                  var10 = var38;
                  throw var38;
               } finally {
                  if (fs != null) {
                     if (var10 != null) {
                        try {
                           fs.close();
                        } catch (Throwable var37) {
                           var10.addSuppressed(var37);
                        }
                     } else {
                        fs.close();
                     }
                  }
               }
            } catch (Throwable var40) {
               var8 = var40;
               throw var40;
            } finally {
               if (is != null) {
                  if (var8 != null) {
                     try {
                        is.close();
                     } catch (Throwable var36) {
                        var8.addSuppressed(var36);
                     }
                  } else {
                     is.close();
                  }
               }
            }
         } else {
            dumpMini = true;
         }
      }
   }

   public static void dump(DirectoryEntry root, File parent) throws IOException {
      Iterator<Entry> it = root.getEntries();

      while (it.hasNext()) {
         Entry entry = (Entry)it.next();
         if (entry instanceof DocumentNode) {
            DocumentNode node = (DocumentNode)entry;
            DocumentInputStream is = new DocumentInputStream(node);
            byte[] bytes = IOUtils.toByteArray(is);
            is.close();
            OutputStream out = new FileOutputStream(new File(parent, node.getName().trim()));
            Throwable var8 = null;

            try {
               out.write(bytes);
            } catch (Throwable var17) {
               var8 = var17;
               throw var17;
            } finally {
               if (out != null) {
                  if (var8 != null) {
                     try {
                        out.close();
                     } catch (Throwable var16) {
                        var8.addSuppressed(var16);
                     }
                  } else {
                     out.close();
                  }
               }
            }
         } else if (entry instanceof DirectoryEntry) {
            DirectoryEntry dir = (DirectoryEntry)entry;
            File file = new File(parent, entry.getName());
            if (!file.exists() && !file.mkdirs()) {
               throw new IOException("Could not create directory " + file);
            }

            dump(dir, file);
         } else {
            System.err.println("Skipping unsupported POIFS entry: " + entry);
         }
      }
   }

   public static void dump(POIFSFileSystem fs, int startBlock, String name, File parent) throws IOException {
      File file = new File(parent, name);
      FileOutputStream out = new FileOutputStream(file);
      Throwable var6 = null;

      try {
         POIFSStream stream = new POIFSStream(fs, startBlock);
         byte[] b = IOUtils.safelyAllocate(fs.getBigBlockSize(), 100000);

         for (ByteBuffer bb : stream) {
            int len = bb.remaining();
            bb.get(b);
            out.write(b, 0, len);
         }
      } catch (Throwable var19) {
         var6 = var19;
         throw var19;
      } finally {
         if (out != null) {
            if (var6 != null) {
               try {
                  out.close();
               } catch (Throwable var18) {
                  var6.addSuppressed(var18);
               }
            } else {
               out.close();
            }
         }
      }
   }
}

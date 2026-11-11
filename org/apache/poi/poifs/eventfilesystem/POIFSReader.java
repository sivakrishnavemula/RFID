package org.apache.poi.poifs.eventfilesystem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.POIFSDocument;
import org.apache.poi.poifs.filesystem.POIFSDocumentPath;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.poifs.property.DirectoryProperty;
import org.apache.poi.poifs.property.DocumentProperty;
import org.apache.poi.poifs.property.Property;
import org.apache.poi.poifs.property.PropertyTable;
import org.apache.poi.poifs.property.RootProperty;
import org.apache.poi.util.IOUtils;

public class POIFSReader {
   private final POIFSReaderRegistry registry = new POIFSReaderRegistry();
   private boolean registryClosed = false;
   private boolean notifyEmptyDirectories;

   public void read(InputStream stream) throws IOException {
      POIFSFileSystem poifs = new POIFSFileSystem(stream);
      Throwable var3 = null;

      try {
         this.read(poifs);
      } catch (Throwable var12) {
         var3 = var12;
         throw var12;
      } finally {
         if (poifs != null) {
            if (var3 != null) {
               try {
                  poifs.close();
               } catch (Throwable var11) {
                  var3.addSuppressed(var11);
               }
            } else {
               poifs.close();
            }
         }
      }
   }

   public void read(File poifsFile) throws IOException {
      POIFSFileSystem poifs = new POIFSFileSystem(poifsFile, true);
      Throwable var3 = null;

      try {
         this.read(poifs);
      } catch (Throwable var12) {
         var3 = var12;
         throw var12;
      } finally {
         if (poifs != null) {
            if (var3 != null) {
               try {
                  poifs.close();
               } catch (Throwable var11) {
                  var3.addSuppressed(var11);
               }
            } else {
               poifs.close();
            }
         }
      }
   }

   public void read(POIFSFileSystem poifs) throws IOException {
      this.registryClosed = true;
      PropertyTable properties = poifs.getPropertyTable();
      RootProperty root = properties.getRoot();
      this.processProperties(poifs, root, new POIFSDocumentPath());
   }

   public void registerListener(POIFSReaderListener listener) {
      if (listener == null) {
         throw new NullPointerException();
      }

      if (this.registryClosed) {
         throw new IllegalStateException();
      }

      this.registry.registerListener(listener);
   }

   public void registerListener(POIFSReaderListener listener, String name) {
      this.registerListener(listener, null, name);
   }

   public void registerListener(POIFSReaderListener listener, POIFSDocumentPath path, String name) {
      if (listener == null || name == null || name.length() == 0) {
         throw new NullPointerException();
      }

      if (this.registryClosed) {
         throw new IllegalStateException();
      }

      this.registry.registerListener(listener, path == null ? new POIFSDocumentPath() : path, name);
   }

   public void setNotifyEmptyDirectories(boolean notifyEmptyDirectories) {
      this.notifyEmptyDirectories = notifyEmptyDirectories;
   }

   public static void main(String[] args) throws IOException {
      if (args.length == 0) {
         System.err.println("at least one argument required: input filename(s)");
         System.exit(1);
      }

      for (String arg : args) {
         POIFSReader reader = new POIFSReader();
         reader.registerListener(POIFSReader::readEntry);
         System.out.println("reading " + arg);
         reader.read(new File(arg));
      }
   }

   private static void readEntry(POIFSReaderEvent event) {
      POIFSDocumentPath path = event.getPath();
      StringBuilder sb = new StringBuilder();

      try {
         DocumentInputStream istream = event.getStream();
         Throwable var4 = null;

         try {
            sb.setLength(0);
            int pathLength = path.length();

            for (int k = 0; k < pathLength; k++) {
               sb.append("/").append(path.getComponent(k));
            }

            byte[] data = IOUtils.toByteArray(istream);
            sb.append("/").append(event.getName()).append(": ").append(data.length).append(" bytes read");
            System.out.println(sb);
         } catch (Throwable var15) {
            var4 = var15;
            throw var15;
         } finally {
            if (istream != null) {
               if (var4 != null) {
                  try {
                     istream.close();
                  } catch (Throwable var14) {
                     var4.addSuppressed(var14);
                  }
               } else {
                  istream.close();
               }
            }
         }
      } catch (IOException var17) {
      }
   }

   private void processProperties(POIFSFileSystem poifs, DirectoryProperty dir, POIFSDocumentPath path) {
      boolean hasChildren = false;

      for (Property property : dir) {
         hasChildren = true;
         String name = property.getName();
         if (property.isDirectory()) {
            POIFSDocumentPath new_path = new POIFSDocumentPath(path, new String[]{name});
            this.processProperties(poifs, (DirectoryProperty)property, new_path);
         } else {
            POIFSDocument document = null;

            for (POIFSReaderListener rl : this.registry.getListeners(path, name)) {
               if (document == null) {
                  document = new POIFSDocument((DocumentProperty)property, poifs);
               }

               DocumentInputStream dis = new DocumentInputStream(document);
               Throwable var12 = null;

               try {
                  POIFSReaderEvent pe = new POIFSReaderEvent(dis, path, name);
                  rl.processPOIFSReaderEvent(pe);
               } catch (Throwable var21) {
                  var12 = var21;
                  throw var21;
               } finally {
                  if (dis != null) {
                     if (var12 != null) {
                        try {
                           dis.close();
                        } catch (Throwable var20) {
                           var12.addSuppressed(var20);
                        }
                     } else {
                        dis.close();
                     }
                  }
               }
            }
         }
      }

      if (!hasChildren && this.notifyEmptyDirectories) {
         for (POIFSReaderListener rl : this.registry.getListeners(path, ".")) {
            POIFSReaderEvent pe = new POIFSReaderEvent(null, path, null);
            rl.processPOIFSReaderEvent(pe);
         }
      }
   }
}

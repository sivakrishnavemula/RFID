package org.apache.poi.poifs.filesystem;

import java.io.EOFException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.poi.hpsf.MarkUnsupportedException;
import org.apache.poi.hpsf.NoPropertySetStreamException;
import org.apache.poi.hpsf.PropertySet;
import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.util.Internal;

@Internal
public final class EntryUtils {
   private EntryUtils() {
   }

   @Internal
   public static void copyNodeRecursively(Entry entry, DirectoryEntry target) throws IOException {
      if (entry.isDirectoryEntry()) {
         DirectoryEntry dirEntry = (DirectoryEntry)entry;
         DirectoryEntry newTarget = target.createDirectory(entry.getName());
         newTarget.setStorageClsid(dirEntry.getStorageClsid());
         Iterator<Entry> entries = dirEntry.getEntries();

         while (entries.hasNext()) {
            copyNodeRecursively((Entry)entries.next(), newTarget);
         }
      } else {
         DocumentEntry dentry = (DocumentEntry)entry;
         DocumentInputStream dstream = new DocumentInputStream(dentry);
         target.createDocument(dentry.getName(), dstream);
         dstream.close();
      }
   }

   public static void copyNodes(DirectoryEntry sourceRoot, DirectoryEntry targetRoot) throws IOException {
      for (Entry entry : sourceRoot) {
         copyNodeRecursively(entry, targetRoot);
      }
   }

   public static void copyNodes(POIFSFileSystem source, POIFSFileSystem target) throws IOException {
      copyNodes(source.getRoot(), target.getRoot());
   }

   public static void copyNodes(POIFSFileSystem source, POIFSFileSystem target, List<String> excepts) throws IOException {
      copyNodes(new FilteringDirectoryNode(source.getRoot(), excepts), new FilteringDirectoryNode(target.getRoot(), excepts));
   }

   public static boolean areDirectoriesIdentical(DirectoryEntry dirA, DirectoryEntry dirB) {
      return new EntryUtils.DirectoryDelegate(dirA).equals(new EntryUtils.DirectoryDelegate(dirB));
   }

   public static boolean areDocumentsIdentical(DocumentEntry docA, DocumentEntry docB) throws IOException {
      try {
         return new EntryUtils.DocumentDelegate(docA).equals(new EntryUtils.DocumentDelegate(docB));
      } catch (RuntimeException e) {
         if (e.getCause() instanceof IOException) {
            throw (IOException)e.getCause();
         } else {
            throw e;
         }
      }
   }

   private static class DirectoryDelegate implements EntryUtils.POIDelegate {
      final DirectoryEntry dir;

      DirectoryDelegate(DirectoryEntry dir) {
         this.dir = dir;
      }

      private Map<String, EntryUtils.POIDelegate> entries() {
         return (Map<String, EntryUtils.POIDelegate>)StreamSupport.stream(this.dir.spliterator(), false)
            .collect(Collectors.toMap(Entry::getName, EntryUtils.DirectoryDelegate::toDelegate));
      }

      private static EntryUtils.POIDelegate toDelegate(Entry entry) {
         return entry.isDirectoryEntry() ? new EntryUtils.DirectoryDelegate((DirectoryEntry)entry) : new EntryUtils.DocumentDelegate((DocumentEntry)entry);
      }

      public int hashCode() {
         return this.dir.getName().hashCode();
      }

      public boolean equals(Object other) {
         if (!(other instanceof EntryUtils.DirectoryDelegate)) {
            return false;
         } else {
            EntryUtils.DirectoryDelegate dd = (EntryUtils.DirectoryDelegate)other;
            if (this == dd) {
               return true;
            } else if (!Objects.equals(this.dir.getName(), dd.dir.getName())) {
               return false;
            } else {
               return this.dir.getEntryCount() != dd.dir.getEntryCount() ? false : this.entries().equals(dd.entries());
            }
         }
      }
   }

   private static class DocumentDelegate implements EntryUtils.POIDelegate {
      final DocumentEntry doc;

      DocumentDelegate(DocumentEntry doc) {
         this.doc = doc;
      }

      public int hashCode() {
         return this.doc.getName().hashCode();
      }

      public boolean equals(Object other) {
         if (!(other instanceof EntryUtils.DocumentDelegate)) {
            return false;
         }

         EntryUtils.DocumentDelegate dd = (EntryUtils.DocumentDelegate)other;
         if (this == dd) {
            return true;
         }

         if (!Objects.equals(this.doc.getName(), dd.doc.getName())) {
            return false;
         }

         try {
            DocumentInputStream inpA = new DocumentInputStream(this.doc);
            Throwable var4 = null;

            try {
               DocumentInputStream inpB = new DocumentInputStream(dd.doc);
               Throwable var6 = null;

               try {
                  if (PropertySet.isPropertySetStream(inpA) && PropertySet.isPropertySetStream(inpB)) {
                     PropertySet ps1 = PropertySetFactory.create(inpA);
                     PropertySet ps2 = PropertySetFactory.create(inpB);
                     return ps1.equals(ps2);
                  } else {
                     return isEqual(inpA, inpB);
                  }
               } catch (Throwable var37) {
                  var6 = var37;
                  throw var37;
               } finally {
                  if (inpB != null) {
                     if (var6 != null) {
                        try {
                           inpB.close();
                        } catch (Throwable var36) {
                           var6.addSuppressed(var36);
                        }
                     } else {
                        inpB.close();
                     }
                  }
               }
            } catch (Throwable var39) {
               var4 = var39;
               throw var39;
            } finally {
               if (inpA != null) {
                  if (var4 != null) {
                     try {
                        inpA.close();
                     } catch (Throwable var35) {
                        var4.addSuppressed(var35);
                     }
                  } else {
                     inpA.close();
                  }
               }
            }
         } catch (MarkUnsupportedException | NoPropertySetStreamException | IOException ex) {
            throw new RuntimeException(ex);
         }
      }

      private static boolean isEqual(DocumentInputStream i1, DocumentInputStream i2) throws IOException {
         byte[] buf1 = new byte[4096];
         byte[] buf2 = new byte[4096];

         try {
            int len;
            while ((len = i1.read(buf1)) > 0) {
               i2.readFully(buf2, 0, len);

               for (int i = 0; i < len; i++) {
                  if (buf1[i] != buf2[i]) {
                     return false;
                  }
               }
            }

            return i2.read() < 0;
         } catch (EOFException | RuntimeException ioe) {
            return false;
         }
      }
   }

   private interface POIDelegate {
   }
}

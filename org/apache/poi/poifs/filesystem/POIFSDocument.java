package org.apache.poi.poifs.filesystem;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import org.apache.poi.poifs.dev.POIFSViewable;
import org.apache.poi.poifs.property.DocumentProperty;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.IOUtils;

public final class POIFSDocument implements POIFSViewable, Iterable<ByteBuffer> {
   private static final int MAX_RECORD_LENGTH = 100000;
   private DocumentProperty _property;
   private POIFSFileSystem _filesystem;
   private POIFSStream _stream;
   private int _block_size;

   public POIFSDocument(DocumentNode document) {
      this((DocumentProperty)document.getProperty(), ((DirectoryNode)document.getParent()).getNFileSystem());
   }

   public POIFSDocument(DocumentProperty property, POIFSFileSystem filesystem) {
      this._property = property;
      this._filesystem = filesystem;
      if (property.getSize() < 4096) {
         this._stream = new POIFSStream(this._filesystem.getMiniStore(), property.getStartBlock());
         this._block_size = this._filesystem.getMiniStore().getBlockStoreBlockSize();
      } else {
         this._stream = new POIFSStream(this._filesystem, property.getStartBlock());
         this._block_size = this._filesystem.getBlockStoreBlockSize();
      }
   }

   public POIFSDocument(String name, POIFSFileSystem filesystem, InputStream stream) throws IOException {
      this._filesystem = filesystem;
      int length = this.store(stream);
      this._property = new DocumentProperty(name, length);
      this._property.setStartBlock(this._stream.getStartBlock());
      this._property.setDocument(this);
   }

   public POIFSDocument(String name, int size, POIFSFileSystem filesystem, POIFSWriterListener writer) throws IOException {
      this._filesystem = filesystem;
      if (size < 4096) {
         this._stream = new POIFSStream(filesystem.getMiniStore());
         this._block_size = this._filesystem.getMiniStore().getBlockStoreBlockSize();
      } else {
         this._stream = new POIFSStream(filesystem);
         this._block_size = this._filesystem.getBlockStoreBlockSize();
      }

      this._property = new DocumentProperty(name, size);
      this._property.setStartBlock(this._stream.getStartBlock());
      this._property.setDocument(this);
      DocumentOutputStream os = new DocumentOutputStream(this, size);
      Throwable var6 = null;

      try {
         POIFSDocumentPath path = new POIFSDocumentPath(name.split("\\\\"));
         String docName = path.getComponent(path.length() - 1);
         POIFSWriterEvent event = new POIFSWriterEvent(os, path, docName, size);
         writer.processPOIFSWriterEvent(event);
      } catch (Throwable var17) {
         var6 = var17;
         throw var17;
      } finally {
         if (os != null) {
            if (var6 != null) {
               try {
                  os.close();
               } catch (Throwable var16) {
                  var6.addSuppressed(var16);
               }
            } else {
               os.close();
            }
         }
      }
   }

   private int store(InputStream stream) throws IOException {
      int bigBlockSize = 4096;
      BufferedInputStream bis = new BufferedInputStream(stream, 4097);
      bis.mark(4096);
      long streamBlockSize = IOUtils.skipFully(bis, 4096L);
      if (streamBlockSize < 4096L) {
         this._stream = new POIFSStream(this._filesystem.getMiniStore());
         this._block_size = this._filesystem.getMiniStore().getBlockStoreBlockSize();
      } else {
         this._stream = new POIFSStream(this._filesystem);
         this._block_size = this._filesystem.getBlockStoreBlockSize();
      }

      bis.reset();
      OutputStream os = this._stream.getOutputStream();
      Throwable var9 = null;

      long length;
      try {
         length = IOUtils.copy(bis, os);
         int usedInBlock = (int)(length % this._block_size);
         if (usedInBlock != 0 && usedInBlock != this._block_size) {
            int toBlockEnd = this._block_size - usedInBlock;
            byte[] padding = IOUtils.safelyAllocate(toBlockEnd, 100000);
            Arrays.fill(padding, (byte)-1);
            os.write(padding);
         }
      } catch (Throwable var20) {
         var9 = var20;
         throw var20;
      } finally {
         if (os != null) {
            if (var9 != null) {
               try {
                  os.close();
               } catch (Throwable var19) {
                  var9.addSuppressed(var19);
               }
            } else {
               os.close();
            }
         }
      }

      return (int)length;
   }

   void free() throws IOException {
      this._stream.free();
      this._property.setStartBlock(-2);
   }

   POIFSFileSystem getFileSystem() {
      return this._filesystem;
   }

   int getDocumentBlockSize() {
      return this._block_size;
   }

   public Iterator<ByteBuffer> iterator() {
      return this.getBlockIterator();
   }

   Iterator<ByteBuffer> getBlockIterator() {
      return (this.getSize() > 0 ? this._stream : Collections.emptyList()).iterator();
   }

   public int getSize() {
      return this._property.getSize();
   }

   public void replaceContents(InputStream stream) throws IOException {
      this.free();
      int size = this.store(stream);
      this._property.setStartBlock(this._stream.getStartBlock());
      this._property.updateSize(size);
   }

   DocumentProperty getDocumentProperty() {
      return this._property;
   }

   public Object[] getViewableArray() {
      String result = "<NO DATA>";
      if (this.getSize() > 0) {
         byte[] data = IOUtils.safelyAllocate(this.getSize(), 100000);
         int offset = 0;

         for (ByteBuffer buffer : this._stream) {
            int length = Math.min(this._block_size, data.length - offset);
            buffer.get(data, offset, length);
            offset += length;
         }

         result = HexDump.dump(data, 0L, 0);
      }

      return new String[]{result};
   }

   public Iterator<Object> getViewableIterator() {
      return Collections.emptyList().iterator();
   }

   public boolean preferArray() {
      return true;
   }

   public String getShortDescription() {
      return "Document: \"" + this._property.getName() + "\" size = " + this.getSize();
   }
}

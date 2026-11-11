package org.apache.poi.poifs.filesystem;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import org.apache.poi.poifs.filesystem.BlockStore.ChainLoopDetector;

public class POIFSStream implements Iterable<ByteBuffer> {
   private BlockStore blockStore;
   private int startBlock;
   private OutputStream outStream;

   public POIFSStream(BlockStore blockStore, int startBlock) {
      this.blockStore = blockStore;
      this.startBlock = startBlock;
   }

   public POIFSStream(BlockStore blockStore) {
      this.blockStore = blockStore;
      this.startBlock = -2;
   }

   public int getStartBlock() {
      return this.startBlock;
   }

   public Iterator<ByteBuffer> iterator() {
      return this.getBlockIterator();
   }

   Iterator<ByteBuffer> getBlockIterator() {
      if (this.startBlock == -2) {
         throw new IllegalStateException("Can't read from a new stream before it has been written to");
      } else {
         return new POIFSStream.StreamBlockByteBufferIterator(this.startBlock);
      }
   }

   void updateContents(byte[] contents) throws IOException {
      OutputStream os = this.getOutputStream();
      os.write(contents);
      os.close();
   }

   public OutputStream getOutputStream() throws IOException {
      if (this.outStream == null) {
         this.outStream = new POIFSStream.StreamBlockByteBuffer();
      }

      return this.outStream;
   }

   public void free() throws IOException {
      ChainLoopDetector loopDetector = this.blockStore.getChainLoopDetector();
      this.free(loopDetector);
   }

   private void free(ChainLoopDetector loopDetector) {
      int nextBlock = this.startBlock;

      while (nextBlock != -2) {
         int thisBlock = nextBlock;
         loopDetector.claim(thisBlock);
         nextBlock = this.blockStore.getNextBlock(thisBlock);
         this.blockStore.setNextBlock(thisBlock, -1);
      }

      this.startBlock = -2;
   }

   protected class StreamBlockByteBuffer extends OutputStream {
      byte[] oneByte = new byte[1];
      ByteBuffer buffer;
      ChainLoopDetector loopDetector = POIFSStream.this.blockStore.getChainLoopDetector();
      int prevBlock = -2;
      int nextBlock = POIFSStream.this.startBlock;

      StreamBlockByteBuffer() throws IOException {
      }

      void createBlockIfNeeded() throws IOException {
         if (this.buffer == null || !this.buffer.hasRemaining()) {
            int thisBlock = this.nextBlock;
            if (thisBlock == -2) {
               thisBlock = POIFSStream.this.blockStore.getFreeBlock();
               this.loopDetector.claim(thisBlock);
               this.nextBlock = -2;
               if (this.prevBlock != -2) {
                  POIFSStream.this.blockStore.setNextBlock(this.prevBlock, thisBlock);
               }

               POIFSStream.this.blockStore.setNextBlock(thisBlock, -2);
               if (POIFSStream.this.startBlock == -2) {
                  POIFSStream.this.startBlock = thisBlock;
               }
            } else {
               this.loopDetector.claim(thisBlock);
               this.nextBlock = POIFSStream.this.blockStore.getNextBlock(thisBlock);
            }

            this.buffer = POIFSStream.this.blockStore.createBlockIfNeeded(thisBlock);
            this.prevBlock = thisBlock;
         }
      }

      public void write(int b) throws IOException {
         this.oneByte[0] = (byte)(b & 0xFF);
         this.write(this.oneByte);
      }

      public void write(byte[] b, int off, int len) throws IOException {
         if (off < 0 || off > b.length || len < 0 || off + len > b.length || off + len < 0) {
            throw new IndexOutOfBoundsException();
         }

         if (len != 0) {
            do {
               this.createBlockIfNeeded();
               int writeBytes = Math.min(this.buffer.remaining(), len);
               this.buffer.put(b, off, writeBytes);
               off += writeBytes;
               len -= writeBytes;
            } while (len > 0);
         }
      }

      public void close() throws IOException {
         POIFSStream toFree = new POIFSStream(POIFSStream.this.blockStore, this.nextBlock);
         toFree.free(this.loopDetector);
         if (this.prevBlock != -2) {
            POIFSStream.this.blockStore.setNextBlock(this.prevBlock, -2);
         }
      }
   }

   protected class StreamBlockByteBufferIterator implements Iterator<ByteBuffer> {
      private ChainLoopDetector loopDetector;
      private int nextBlock;

      StreamBlockByteBufferIterator(int firstBlock) {
         this.nextBlock = firstBlock;

         try {
            this.loopDetector = POIFSStream.this.blockStore.getChainLoopDetector();
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      }

      public boolean hasNext() {
         return this.nextBlock != -2;
      }

      public ByteBuffer next() {
         if (this.nextBlock == -2) {
            throw new IndexOutOfBoundsException("Can't read past the end of the stream");
         }

         try {
            this.loopDetector.claim(this.nextBlock);
            ByteBuffer data = POIFSStream.this.blockStore.getBlockAt(this.nextBlock);
            this.nextBlock = POIFSStream.this.blockStore.getNextBlock(this.nextBlock);
            return data;
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      }

      public void remove() {
         throw new UnsupportedOperationException();
      }
   }
}

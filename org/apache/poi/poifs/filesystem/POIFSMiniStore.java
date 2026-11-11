package org.apache.poi.poifs.filesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.poifs.filesystem.BlockStore.ChainLoopDetector;
import org.apache.poi.poifs.property.RootProperty;
import org.apache.poi.poifs.storage.BATBlock;
import org.apache.poi.poifs.storage.HeaderBlock;
import org.apache.poi.poifs.storage.BATBlock.BATBlockAndIndex;

public class POIFSMiniStore extends BlockStore {
   private POIFSFileSystem _filesystem;
   private POIFSStream _mini_stream;
   private List<BATBlock> _sbat_blocks;
   private HeaderBlock _header;
   private RootProperty _root;

   POIFSMiniStore(POIFSFileSystem filesystem, RootProperty root, List<BATBlock> sbats, HeaderBlock header) {
      this._filesystem = filesystem;
      this._sbat_blocks = sbats;
      this._header = header;
      this._root = root;
      this._mini_stream = new POIFSStream(filesystem, root.getStartBlock());
   }

   protected ByteBuffer getBlockAt(int offset) {
      int byteOffset = offset * 64;
      int bigBlockNumber = byteOffset / this._filesystem.getBigBlockSize();
      int bigBlockOffset = byteOffset % this._filesystem.getBigBlockSize();
      Iterator<ByteBuffer> it = this._mini_stream.getBlockIterator();

      for (int i = 0; i < bigBlockNumber; i++) {
         it.next();
      }

      ByteBuffer dataBlock = (ByteBuffer)it.next();
      if (dataBlock == null) {
         throw new IndexOutOfBoundsException("Big block " + bigBlockNumber + " outside stream");
      }

      dataBlock.position(dataBlock.position() + bigBlockOffset);
      ByteBuffer miniBuffer = dataBlock.slice();
      miniBuffer.limit(64);
      return miniBuffer;
   }

   protected ByteBuffer createBlockIfNeeded(int offset) throws IOException {
      boolean firstInStore = false;
      if (this._mini_stream.getStartBlock() == -2) {
         firstInStore = true;
      }

      if (!firstInStore) {
         try {
            return this.getBlockAt(offset);
         } catch (IndexOutOfBoundsException var7) {
         }
      }

      int newBigBlock = this._filesystem.getFreeBlock();
      this._filesystem.createBlockIfNeeded(newBigBlock);
      if (firstInStore) {
         this._filesystem._get_property_table().getRoot().setStartBlock(newBigBlock);
         this._mini_stream = new POIFSStream(this._filesystem, newBigBlock);
      } else {
         ChainLoopDetector loopDetector = this._filesystem.getChainLoopDetector();
         int block = this._mini_stream.getStartBlock();

         while (true) {
            loopDetector.claim(block);
            int next = this._filesystem.getNextBlock(block);
            if (next == -2) {
               this._filesystem.setNextBlock(block, newBigBlock);
               break;
            }

            block = next;
         }
      }

      this._filesystem.setNextBlock(newBigBlock, -2);
      return this.createBlockIfNeeded(offset);
   }

   protected BATBlockAndIndex getBATBlockAndIndex(int offset) {
      return BATBlock.getSBATBlockAndIndex(offset, this._header, this._sbat_blocks);
   }

   protected int getNextBlock(int offset) {
      BATBlockAndIndex bai = this.getBATBlockAndIndex(offset);
      return bai.getBlock().getValueAt(bai.getIndex());
   }

   protected void setNextBlock(int offset, int nextBlock) {
      BATBlockAndIndex bai = this.getBATBlockAndIndex(offset);
      bai.getBlock().setValueAt(bai.getIndex(), nextBlock);
   }

   protected int getFreeBlock() throws IOException {
      int sectorsPerSBAT = this._filesystem.getBigBlockSizeDetails().getBATEntriesPerBlock();
      int offset = 0;

      for (BATBlock sbat : this._sbat_blocks) {
         if (sbat.hasFreeSectors()) {
            for (int j = 0; j < sectorsPerSBAT; j++) {
               int sbatValue = sbat.getValueAt(j);
               if (sbatValue == -1) {
                  return offset + j;
               }
            }
         }

         offset += sectorsPerSBAT;
      }

      BATBlock newSBAT = BATBlock.createEmptyBATBlock(this._filesystem.getBigBlockSizeDetails(), false);
      int batForSBAT = this._filesystem.getFreeBlock();
      newSBAT.setOurBlockIndex(batForSBAT);
      if (this._header.getSBATCount() == 0) {
         this._header.setSBATStart(batForSBAT);
         this._header.setSBATBlockCount(1);
      } else {
         ChainLoopDetector loopDetector = this._filesystem.getChainLoopDetector();
         int batOffset = this._header.getSBATStart();

         while (true) {
            loopDetector.claim(batOffset);
            int nextBat = this._filesystem.getNextBlock(batOffset);
            if (nextBat == -2) {
               this._filesystem.setNextBlock(batOffset, batForSBAT);
               this._header.setSBATBlockCount(this._header.getSBATCount() + 1);
               break;
            }

            batOffset = nextBat;
         }
      }

      this._filesystem.setNextBlock(batForSBAT, -2);
      this._sbat_blocks.add(newSBAT);
      return offset;
   }

   protected ChainLoopDetector getChainLoopDetector() {
      return new ChainLoopDetector(this, this._root.getSize());
   }

   protected int getBlockStoreBlockSize() {
      return 64;
   }

   void syncWithDataSource() throws IOException {
      int blocksUsed = 0;

      for (BATBlock sbat : this._sbat_blocks) {
         ByteBuffer block = this._filesystem.getBlockAt(sbat.getOurBlockIndex());
         sbat.writeData(block);
         if (!sbat.hasFreeSectors()) {
            blocksUsed += this._filesystem.getBigBlockSizeDetails().getBATEntriesPerBlock();
         } else {
            blocksUsed += sbat.getUsedSectors(false);
         }
      }

      this._filesystem._get_property_table().getRoot().setSize(blocksUsed);
   }
}

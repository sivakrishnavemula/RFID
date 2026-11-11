package org.apache.poi.poifs.property;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.apache.poi.poifs.common.POIFSBigBlockSize;
import org.apache.poi.poifs.filesystem.BATManaged;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.poifs.filesystem.POIFSStream;
import org.apache.poi.poifs.storage.HeaderBlock;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

public final class PropertyTable implements BATManaged {
   private static final POILogger _logger = POILogFactory.getLogger(PropertyTable.class);
   private static final int MAX_RECORD_LENGTH = 100000;
   private final HeaderBlock _header_block;
   private final List<Property> _properties = new ArrayList();
   private final POIFSBigBlockSize _bigBigBlockSize;

   public PropertyTable(HeaderBlock headerBlock) {
      this._header_block = headerBlock;
      this._bigBigBlockSize = headerBlock.getBigBlockSize();
      this.addProperty(new RootProperty());
   }

   public PropertyTable(HeaderBlock headerBlock, POIFSFileSystem filesystem) throws IOException {
      this(headerBlock, new POIFSStream(filesystem, headerBlock.getPropertyStart()));
   }

   PropertyTable(HeaderBlock headerBlock, Iterable<ByteBuffer> dataSource) throws IOException {
      this._header_block = headerBlock;
      this._bigBigBlockSize = headerBlock.getBigBlockSize();

      for (ByteBuffer bb : dataSource) {
         byte[] data;
         if (bb.hasArray() && bb.arrayOffset() == 0 && bb.array().length == this._bigBigBlockSize.getBigBlockSize()) {
            data = bb.array();
         } else {
            data = IOUtils.safelyAllocate(this._bigBigBlockSize.getBigBlockSize(), 100000);
            int toRead = data.length;
            if (bb.remaining() < this._bigBigBlockSize.getBigBlockSize()) {
               _logger.log(
                  5, new Object[]{"Short Property Block, ", bb.remaining(), " bytes instead of the expected " + this._bigBigBlockSize.getBigBlockSize()}
               );
               toRead = bb.remaining();
            }

            bb.get(data, 0, toRead);
         }

         PropertyFactory.convertToProperties(data, this._properties);
      }

      this.populatePropertyTree((DirectoryProperty)this._properties.get(0));
   }

   public void addProperty(Property property) {
      this._properties.add(property);
   }

   public void removeProperty(Property property) {
      this._properties.remove(property);
   }

   public RootProperty getRoot() {
      return (RootProperty)this._properties.get(0);
   }

   public int getStartBlock() {
      return this._header_block.getPropertyStart();
   }

   public void setStartBlock(int index) {
      this._header_block.setPropertyStart(index);
   }

   public int countBlocks() {
      long rawSize = this._properties.size() * 128L;
      int blkSize = this._bigBigBlockSize.getBigBlockSize();
      int numBlocks = (int)(rawSize / blkSize);
      if (rawSize % blkSize != 0L) {
         numBlocks++;
      }

      return numBlocks;
   }

   public void preWrite() {
      List<Property> pList = new ArrayList();
      int i = 0;

      for (Property p : this._properties) {
         if (p != null) {
            p.setIndex(i++);
            pList.add(p);
         }
      }

      for (Property p : pList) {
         p.preWrite();
      }
   }

   public void write(POIFSStream stream) throws IOException {
      OutputStream os = stream.getOutputStream();

      for (Property property : this._properties) {
         if (property != null) {
            property.writeData(os);
         }
      }

      os.close();
      if (this.getStartBlock() != stream.getStartBlock()) {
         this.setStartBlock(stream.getStartBlock());
      }
   }

   private void populatePropertyTree(DirectoryProperty root) throws IOException {
      int index = root.getChildIndex();
      if (Property.isValidIndex(index)) {
         Stack<Property> children = new Stack();
         children.push(this._properties.get(index));

         while (!children.empty()) {
            Property property = (Property)children.pop();
            if (property != null) {
               root.addChild(property);
               if (property.isDirectory()) {
                  this.populatePropertyTree((DirectoryProperty)property);
               }

               index = property.getPreviousChildIndex();
               if (this.isValidIndex(index)) {
                  children.push(this._properties.get(index));
               }

               index = property.getNextChildIndex();
               if (this.isValidIndex(index)) {
                  children.push(this._properties.get(index));
               }
            }
         }
      }
   }

   private boolean isValidIndex(int index) {
      if (!Property.isValidIndex(index)) {
         return false;
      }

      if (index >= 0 && index < this._properties.size()) {
         return true;
      }

      _logger.log(5, new Object[]{"Property index " + index + "outside the valid range 0.." + this._properties.size()});
      return false;
   }
}

package org.apache.poi.poifs.filesystem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LittleEndianOutputStream;
import org.apache.poi.util.StringUtil;

public class Ole10Native {
   public static final String OLE10_NATIVE = "\u0001Ole10Native";
   protected static final String ISO1 = "ISO-8859-1";
   private static final int MAX_RECORD_LENGTH = 100000000;
   private static final byte[] OLE_MARKER_BYTES = new byte[]{1, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
   private static final String OLE_MARKER_NAME = "\u0001Ole";
   private int totalSize;
   private short flags1 = 2;
   private String label;
   private String fileName;
   private short flags2;
   private short unknown1 = 3;
   private String command;
   private byte[] dataBuffer;
   private short flags3;
   private Ole10Native.EncodingMode mode;

   public static Ole10Native createFromEmbeddedOleObject(POIFSFileSystem poifs) throws IOException, Ole10NativeException {
      return createFromEmbeddedOleObject(poifs.getRoot());
   }

   public static Ole10Native createFromEmbeddedOleObject(DirectoryNode directory) throws IOException, Ole10NativeException {
      DocumentEntry nativeEntry = (DocumentEntry)directory.getEntry("\u0001Ole10Native");
      byte[] data = IOUtils.safelyAllocate(nativeEntry.getSize(), 100000000);
      int readBytes = directory.createDocumentInputStream(nativeEntry).read(data);
      assert readBytes == data.length;
      return new Ole10Native(data, 0);
   }

   public Ole10Native(String label, String filename, String command, byte[] data) {
      this.setLabel(label);
      this.setFileName(filename);
      this.setCommand(command);
      this.setDataBuffer(data);
      this.mode = Ole10Native.EncodingMode.parsed;
   }

   public Ole10Native(byte[] data, int offset) throws Ole10NativeException {
      int ofs = offset;
      if (data.length < offset + 2) {
         throw new Ole10NativeException("data is too small");
      }

      this.totalSize = LittleEndian.getInt(data, ofs);
      ofs += 4;
      this.mode = Ole10Native.EncodingMode.unparsed;
      if (LittleEndian.getShort(data, ofs) == 2) {
         if (Character.isISOControl(data[ofs + 2])) {
            this.mode = Ole10Native.EncodingMode.compact;
         } else {
            this.mode = Ole10Native.EncodingMode.parsed;
         }
      }

      int dataSize;
      switch (this.mode) {
         case parsed:
            this.flags1 = LittleEndian.getShort(data, ofs);
            ofs += 2;
            int len = getStringLength(data, ofs);
            this.label = StringUtil.getFromCompressedUnicode(data, ofs, len - 1);
            ofs += len;
            len = getStringLength(data, ofs);
            this.fileName = StringUtil.getFromCompressedUnicode(data, ofs, len - 1);
            ofs += len;
            this.flags2 = LittleEndian.getShort(data, ofs);
            ofs += 2;
            this.unknown1 = LittleEndian.getShort(data, ofs);
            ofs += 2;
            len = LittleEndian.getInt(data, ofs);
            ofs += 4;
            this.command = StringUtil.getFromCompressedUnicode(data, ofs, len - 1);
            ofs += len;
            if (this.totalSize < ofs) {
               throw new Ole10NativeException("Invalid Ole10Native");
            }

            dataSize = LittleEndian.getInt(data, ofs);
            ofs += 4;
            if (dataSize < 0 || this.totalSize - (ofs - 4) < dataSize) {
               throw new Ole10NativeException("Invalid Ole10Native");
            }
            break;
         case compact:
            this.flags1 = LittleEndian.getShort(data, ofs);
            ofs += 2;
            dataSize = this.totalSize - 2;
            break;
         case unparsed:
         default:
            dataSize = this.totalSize;
      }

      if ((long)dataSize + ofs > data.length) {
         throw new Ole10NativeException("Invalid Ole10Native: declared data length > available data");
      }

      this.dataBuffer = IOUtils.safelyAllocate(dataSize, 100000000);
      System.arraycopy(data, ofs, this.dataBuffer, 0, dataSize);
      ofs += dataSize;
   }

   public static void createOleMarkerEntry(DirectoryEntry parent) throws IOException {
      if (!parent.hasEntry("\u0001Ole")) {
         parent.createDocument("\u0001Ole", new ByteArrayInputStream(OLE_MARKER_BYTES));
      }
   }

   public static void createOleMarkerEntry(POIFSFileSystem poifs) throws IOException {
      createOleMarkerEntry(poifs.getRoot());
   }

   private static int getStringLength(byte[] data, int ofs) {
      int len = 0;

      while (len + ofs < data.length && data[ofs + len] != 0) {
         len++;
      }

      return len + 1;
   }

   public int getTotalSize() {
      return this.totalSize;
   }

   public short getFlags1() {
      return this.flags1;
   }

   public String getLabel() {
      return this.label;
   }

   public String getFileName() {
      return this.fileName;
   }

   public short getFlags2() {
      return this.flags2;
   }

   public short getUnknown1() {
      return this.unknown1;
   }

   public String getCommand() {
      return this.command;
   }

   public int getDataSize() {
      return this.dataBuffer.length;
   }

   public byte[] getDataBuffer() {
      return this.dataBuffer;
   }

   public short getFlags3() {
      return this.flags3;
   }

   public void writeOut(OutputStream out) throws IOException {
      LittleEndianOutputStream leosOut = new LittleEndianOutputStream(out);
      switch (this.mode) {
         case parsed:
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            LittleEndianOutputStream leos = new LittleEndianOutputStream(bos);
            leos.writeShort(this.getFlags1());
            leos.write(this.getLabel().getBytes("ISO-8859-1"));
            leos.write(0);
            leos.write(this.getFileName().getBytes("ISO-8859-1"));
            leos.write(0);
            leos.writeShort(this.getFlags2());
            leos.writeShort(this.getUnknown1());
            leos.writeInt(this.getCommand().length() + 1);
            leos.write(this.getCommand().getBytes("ISO-8859-1"));
            leos.write(0);
            leos.writeInt(this.getDataSize());
            leos.write(this.getDataBuffer());
            leos.writeShort(this.getFlags3());
            leos.close();
            leosOut.writeInt(bos.size());
            bos.writeTo(out);
            break;
         case compact:
            leosOut.writeInt(this.getDataSize() + 2);
            leosOut.writeShort(this.getFlags1());
            out.write(this.getDataBuffer());
            break;
         case unparsed:
         default:
            leosOut.writeInt(this.getDataSize());
            out.write(this.getDataBuffer());
      }
   }

   public void setFlags1(short flags1) {
      this.flags1 = flags1;
   }

   public void setFlags2(short flags2) {
      this.flags2 = flags2;
   }

   public void setFlags3(short flags3) {
      this.flags3 = flags3;
   }

   public void setLabel(String label) {
      this.label = label;
   }

   public void setFileName(String fileName) {
      this.fileName = fileName;
   }

   public void setCommand(String command) {
      this.command = command;
   }

   public void setUnknown1(short unknown1) {
      this.unknown1 = unknown1;
   }

   public void setDataBuffer(byte[] dataBuffer) {
      this.dataBuffer = (byte[])dataBuffer.clone();
   }

   private enum EncodingMode {
      parsed,
      unparsed,
      compact;
   }
}

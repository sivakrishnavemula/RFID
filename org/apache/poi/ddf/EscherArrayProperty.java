package org.apache.poi.ddf;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;

public final class EscherArrayProperty extends EscherComplexProperty implements Iterable<byte[]> {
   private static final int MAX_RECORD_LENGTH = 100000;
   private static final int FIXED_SIZE = 6;
   private boolean sizeIncludesHeaderSize = true;
   private boolean emptyComplexPart;

   public EscherArrayProperty(short id, byte[] complexData) {
      super(id, checkComplexData(complexData));
      this.emptyComplexPart = complexData == null || complexData.length == 0;
   }

   public EscherArrayProperty(short propertyNumber, boolean isBlipId, byte[] complexData) {
      super(propertyNumber, isBlipId, checkComplexData(complexData));
   }

   private static byte[] checkComplexData(byte[] complexData) {
      return complexData != null && complexData.length != 0 ? complexData : new byte[6];
   }

   public int getNumberOfElementsInArray() {
      return this.emptyComplexPart ? 0 : LittleEndian.getUShort(this.getComplexData(), 0);
   }

   public void setNumberOfElementsInArray(int numberOfElements) {
      int expectedArraySize = numberOfElements * getActualSizeOfElements(this.getSizeOfElements()) + 6;
      if (expectedArraySize != this.getComplexData().length) {
         byte[] newArray = IOUtils.safelyAllocate(expectedArraySize, 100000);
         System.arraycopy(this.getComplexData(), 0, newArray, 0, this.getComplexData().length);
         this.setComplexData(newArray);
      }

      LittleEndian.putShort(this.getComplexData(), 0, (short)numberOfElements);
   }

   public int getNumberOfElementsInMemory() {
      return this.emptyComplexPart ? 0 : LittleEndian.getUShort(this.getComplexData(), 2);
   }

   public void setNumberOfElementsInMemory(int numberOfElements) {
      int expectedArraySize = numberOfElements * getActualSizeOfElements(this.getSizeOfElements()) + 6;
      if (expectedArraySize != this.getComplexData().length) {
         byte[] newArray = IOUtils.safelyAllocate(expectedArraySize, 100000);
         System.arraycopy(this.getComplexData(), 0, newArray, 0, expectedArraySize);
         this.setComplexData(newArray);
      }

      LittleEndian.putShort(this.getComplexData(), 2, (short)numberOfElements);
   }

   public short getSizeOfElements() {
      return this.emptyComplexPart ? 0 : LittleEndian.getShort(this.getComplexData(), 4);
   }

   public void setSizeOfElements(int sizeOfElements) {
      LittleEndian.putShort(this.getComplexData(), 4, (short)sizeOfElements);
      int expectedArraySize = this.getNumberOfElementsInArray() * getActualSizeOfElements(this.getSizeOfElements()) + 6;
      if (expectedArraySize != this.getComplexData().length) {
         byte[] newArray = IOUtils.safelyAllocate(expectedArraySize, 100000);
         System.arraycopy(this.getComplexData(), 0, newArray, 0, 6);
         this.setComplexData(newArray);
      }
   }

   public byte[] getElement(int index) {
      int actualSize = getActualSizeOfElements(this.getSizeOfElements());
      byte[] result = IOUtils.safelyAllocate(actualSize, 100000);
      System.arraycopy(this.getComplexData(), 6 + index * actualSize, result, 0, result.length);
      return result;
   }

   public void setElement(int index, byte[] element) {
      int actualSize = getActualSizeOfElements(this.getSizeOfElements());
      System.arraycopy(element, 0, this.getComplexData(), 6 + index * actualSize, actualSize);
   }

   public String toString() {
      StringBuilder results = new StringBuilder();
      results.append("propNum: ").append(this.getPropertyNumber());
      results.append(", propName: ").append(EscherProperties.getPropertyName(this.getPropertyNumber()));
      results.append(", complex: ").append(this.isComplex());
      results.append(", blipId: ").append(this.isBlipId());
      results.append(", data: \n");
      results.append("    {EscherArrayProperty:\n");
      results.append("     Num Elements: ").append(this.getNumberOfElementsInArray()).append('\n');
      results.append("     Num Elements In Memory: ").append(this.getNumberOfElementsInMemory()).append('\n');
      results.append("     Size of elements: ").append(this.getSizeOfElements()).append('\n');

      for (int i = 0; i < this.getNumberOfElementsInArray(); i++) {
         results.append("     Element ").append(i).append(": ").append(HexDump.toHex(this.getElement(i))).append('\n');
      }

      results.append("}\n");
      return results.toString();
   }

   public String toXml(String tab) {
      StringBuilder builder = new StringBuilder();
      builder.append(tab)
         .append("<")
         .append(this.getClass().getSimpleName())
         .append(" id=\"0x")
         .append(HexDump.toHex(this.getId()))
         .append("\" name=\"")
         .append(this.getName())
         .append("\" blipId=\"")
         .append(this.isBlipId())
         .append("\">\n");

      for (int i = 0; i < this.getNumberOfElementsInArray(); i++) {
         builder.append("\t").append(tab).append("<Element>").append(HexDump.toHex(this.getElement(i))).append("</Element>\n");
      }

      builder.append(tab).append("</").append(this.getClass().getSimpleName()).append(">");
      return builder.toString();
   }

   public int setArrayData(byte[] data, int offset) {
      if (this.emptyComplexPart) {
         this.setComplexData(new byte[0]);
      } else {
         short numElements = LittleEndian.getShort(data, offset);
         short sizeOfElements = LittleEndian.getShort(data, offset + 4);
         int arraySize = getActualSizeOfElements(sizeOfElements) * numElements;
         if (arraySize == this.getComplexData().length) {
            this.setComplexData(new byte[arraySize + 6]);
            this.sizeIncludesHeaderSize = false;
         }

         System.arraycopy(data, offset, this.getComplexData(), 0, this.getComplexData().length);
      }

      return this.getComplexData().length;
   }

   public int serializeSimplePart(byte[] data, int pos) {
      LittleEndian.putShort(data, pos, this.getId());
      int recordSize = this.getComplexData().length;
      if (!this.sizeIncludesHeaderSize) {
         recordSize -= 6;
      }

      LittleEndian.putInt(data, pos + 2, recordSize);
      return 6;
   }

   private static int getActualSizeOfElements(short sizeOfElements) {
      return sizeOfElements < 0 ? (short)(-sizeOfElements >> 2) : sizeOfElements;
   }

   public Iterator<byte[]> iterator() {
      return new Iterator<byte[]>() {
         int idx;

         public boolean hasNext() {
            return this.idx < EscherArrayProperty.this.getNumberOfElementsInArray();
         }

         public byte[] next() {
            if (!this.hasNext()) {
               throw new NoSuchElementException();
            } else {
               return EscherArrayProperty.this.getElement(this.idx++);
            }
         }

         public void remove() {
            throw new UnsupportedOperationException("not yet implemented");
         }
      };
   }
}

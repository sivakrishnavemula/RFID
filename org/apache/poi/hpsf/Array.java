package org.apache.poi.hpsf;

import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndianByteArrayInputStream;

@Internal
public class Array {
   private final Array.ArrayHeader _header = new Array.ArrayHeader();
   private TypedPropertyValue[] _values;

   public void read(LittleEndianByteArrayInputStream lei) {
      this._header.read(lei);
      long numberOfScalarsLong = this._header.getNumberOfScalarValues();
      if (numberOfScalarsLong > 2147483647L) {
         String msg = "Sorry, but POI can't store array of properties with size of " + numberOfScalarsLong + " in memory";
         throw new UnsupportedOperationException(msg);
      }

      int numberOfScalars = (int)numberOfScalarsLong;
      this._values = new TypedPropertyValue[numberOfScalars];
      int paddedType = this._header._type == 12 ? 0 : this._header._type;

      for (int i = 0; i < numberOfScalars; i++) {
         TypedPropertyValue typedPropertyValue = new TypedPropertyValue(paddedType, null);
         typedPropertyValue.read(lei);
         this._values[i] = typedPropertyValue;
         if (paddedType != 0) {
            TypedPropertyValue.skipPadding(lei);
         }
      }
   }

   public TypedPropertyValue[] getValues() {
      return this._values;
   }

   static class ArrayDimension {
      private long _size;
      private int _indexOffset;

      void read(LittleEndianByteArrayInputStream lei) {
         this._size = lei.readUInt();
         this._indexOffset = lei.readInt();
      }
   }

   static class ArrayHeader {
      private Array.ArrayDimension[] _dimensions;
      private int _type;

      void read(LittleEndianByteArrayInputStream lei) {
         this._type = lei.readInt();
         long numDimensionsUnsigned = lei.readUInt();
         if (1L <= numDimensionsUnsigned && numDimensionsUnsigned <= 31L) {
            int numDimensions = (int)numDimensionsUnsigned;
            this._dimensions = new Array.ArrayDimension[numDimensions];

            for (int i = 0; i < numDimensions; i++) {
               Array.ArrayDimension ad = new Array.ArrayDimension();
               ad.read(lei);
               this._dimensions[i] = ad;
            }
         } else {
            String msg = "Array dimension number " + numDimensionsUnsigned + " is not in [1; 31] range";
            throw new IllegalPropertySetDataException(msg);
         }
      }

      long getNumberOfScalarValues() {
         long result = 1L;

         for (Array.ArrayDimension dimension : this._dimensions) {
            result *= dimension._size;
         }

         return result;
      }

      int getType() {
         return this._type;
      }
   }
}

package org.apache.poi.hpsf;

import java.util.ArrayList;
import java.util.List;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndianByteArrayInputStream;

@Internal
public class Vector {
   private final short _type;
   private TypedPropertyValue[] _values;

   public Vector(short type) {
      this._type = type;
   }

   public void read(LittleEndianByteArrayInputStream lei) {
      long longLength = lei.readUInt();
      if (longLength > 2147483647L) {
         throw new UnsupportedOperationException("Vector is too long -- " + longLength);
      }

      int length = (int)longLength;
      List<TypedPropertyValue> values = new ArrayList();
      int paddedType = this._type == 12 ? 0 : this._type;

      for (int i = 0; i < length; i++) {
         TypedPropertyValue value = new TypedPropertyValue(paddedType, null);
         if (paddedType == 0) {
            value.read(lei);
         } else {
            value.readValue(lei);
         }

         values.add(value);
      }

      this._values = (TypedPropertyValue[])values.toArray(new TypedPropertyValue[values.size()]);
   }

   public TypedPropertyValue[] getValues() {
      return this._values;
   }
}

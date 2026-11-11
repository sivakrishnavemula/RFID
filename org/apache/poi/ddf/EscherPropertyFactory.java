package org.apache.poi.ddf;

import java.util.ArrayList;
import java.util.List;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;

public final class EscherPropertyFactory {
   private static final int MAX_RECORD_LENGTH = 100000000;

   public List<EscherProperty> createProperties(byte[] data, int offset, short numProperties) {
      List<EscherProperty> results = new ArrayList();
      int pos = offset;

      for (int i = 0; i < numProperties; i++) {
         short propId = LittleEndian.getShort(data, pos);
         int propData = LittleEndian.getInt(data, pos + 2);
         short propNumber = (short)(propId & 16383);
         boolean isComplex = (propId & -32768) != 0;
         byte propertyType = EscherProperties.getPropertyType(propNumber);
         EscherProperty ep;
         switch (propertyType) {
            case 1:
               ep = new EscherBoolProperty(propId, propData);
               break;
            case 2:
               ep = new EscherRGBProperty(propId, propData);
               break;
            case 3:
               ep = new EscherShapePathProperty(propId, propData);
               break;
            default:
               if (!isComplex) {
                  ep = new EscherSimpleProperty(propId, propData);
               } else if (propertyType == 5) {
                  ep = new EscherArrayProperty(propId, IOUtils.safelyAllocate(propData, 100000000));
               } else {
                  ep = new EscherComplexProperty(propId, IOUtils.safelyAllocate(propData, 100000000));
               }
         }

         results.add(ep);
         pos += 6;
      }

      for (EscherProperty p : results) {
         if (p instanceof EscherComplexProperty) {
            if (p instanceof EscherArrayProperty) {
               pos += ((EscherArrayProperty)p).setArrayData(data, pos);
            } else {
               byte[] complexData = ((EscherComplexProperty)p).getComplexData();
               int leftover = data.length - pos;
               if (leftover < complexData.length) {
                  throw new IllegalStateException(
                     "Could not read complex escher property, length was " + complexData.length + ", but had only " + leftover + " bytes left"
                  );
               }

               System.arraycopy(data, pos, complexData, 0, complexData.length);
               pos += complexData.length;
            }
         }
      }

      return results;
   }
}

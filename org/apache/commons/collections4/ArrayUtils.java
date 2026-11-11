package org.apache.commons.collections4;

class ArrayUtils {
   static final int INDEX_NOT_FOUND = -1;

   static boolean contains(Object[] array, Object objectToFind) {
      return indexOf(array, objectToFind) != -1;
   }

   static <T> int indexOf(T[] array, Object objectToFind) {
      return indexOf(array, objectToFind, 0);
   }

   static int indexOf(Object[] array, Object objectToFind, int startIndex) {
      if (array == null) {
         return -1;
      }

      if (startIndex < 0) {
         startIndex = 0;
      }

      if (objectToFind == null) {
         for (int i = startIndex; i < array.length; i++) {
            if (array[i] == null) {
               return i;
            }
         }
      } else {
         for (int i = startIndex; i < array.length; i++) {
            if (objectToFind.equals(array[i])) {
               return i;
            }
         }
      }

      return -1;
   }
}

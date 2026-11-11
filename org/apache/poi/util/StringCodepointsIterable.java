package org.apache.poi.util;

import java.util.Iterator;

@Internal
public class StringCodepointsIterable implements Iterable<String> {
   private final String string;

   public StringCodepointsIterable(String string) {
      this.string = string;
   }

   public Iterator<String> iterator() {
      return new StringCodepointsIterable.StringCodepointsIterator();
   }

   private class StringCodepointsIterator implements Iterator<String> {
      private int index = 0;

      private StringCodepointsIterator() {
      }

      public void remove() {
         throw new UnsupportedOperationException();
      }

      public boolean hasNext() {
         return this.index < StringCodepointsIterable.this.string.length();
      }

      public String next() {
         int codePoint = StringCodepointsIterable.this.string.codePointAt(this.index);
         this.index = this.index + Character.charCount(codePoint);
         return new String(Character.toChars(codePoint));
      }
   }
}

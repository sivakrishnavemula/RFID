package org.apache.poi.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Internal
public class ReplacingInputStream extends FilterInputStream {
   final int[] buf;
   private int matchedIndex;
   private int unbufferIndex;
   private int replacedIndex;
   private final byte[] pattern;
   private final byte[] replacement;
   private ReplacingInputStream.State state = ReplacingInputStream.State.NOT_MATCHED;

   public ReplacingInputStream(InputStream in, String pattern, String replacement) {
      this(in, pattern.getBytes(StandardCharsets.UTF_8), replacement == null ? null : replacement.getBytes(StandardCharsets.UTF_8));
   }

   public ReplacingInputStream(InputStream in, byte[] pattern, byte[] replacement) {
      super(in);
      if (pattern != null && pattern.length != 0) {
         this.pattern = pattern;
         this.replacement = replacement;
         this.buf = new int[pattern.length];
      } else {
         throw new IllegalArgumentException("pattern length should be > 0");
      }
   }

   public int read(byte[] b, int off, int len) throws IOException {
      if (b == null) {
         throw new NullPointerException();
      }

      if (off < 0 || len < 0 || len > b.length - off) {
         throw new IndexOutOfBoundsException();
      }

      if (len == 0) {
         return 0;
      }

      int c = this.read();
      if (c == -1) {
         return -1;
      }

      b[off] = (byte)c;

      int i;
      for (i = 1; i < len; i++) {
         c = this.read();
         if (c == -1) {
            break;
         }

         b[off + i] = (byte)c;
      }

      return i;
   }

   public int read(byte[] b) throws IOException {
      return this.read(b, 0, b.length);
   }

   public int read() throws IOException {
      switch (this.state) {
         case NOT_MATCHED:
         default:
            int next = super.read();
            if (this.pattern[0] != next) {
               return next;
            }

            Arrays.fill(this.buf, 0);
            this.matchedIndex = 0;
            this.buf[this.matchedIndex++] = next;
            if (this.pattern.length == 1) {
               this.state = ReplacingInputStream.State.REPLACING;
               this.replacedIndex = 0;
            } else {
               this.state = ReplacingInputStream.State.MATCHING;
            }

            return this.read();
         case MATCHING:
            int next = super.read();
            if (this.pattern[this.matchedIndex] == next) {
               this.buf[this.matchedIndex++] = next;
               if (this.matchedIndex == this.pattern.length) {
                  if (this.replacement != null && this.replacement.length != 0) {
                     this.state = ReplacingInputStream.State.REPLACING;
                     this.replacedIndex = 0;
                  } else {
                     this.state = ReplacingInputStream.State.NOT_MATCHED;
                     this.matchedIndex = 0;
                  }
               }
            } else {
               this.buf[this.matchedIndex++] = next;
               this.state = ReplacingInputStream.State.UNBUFFER;
               this.unbufferIndex = 0;
            }

            return this.read();
         case REPLACING:
            int next = this.replacement[this.replacedIndex++];
            if (this.replacedIndex == this.replacement.length) {
               this.state = ReplacingInputStream.State.NOT_MATCHED;
               this.replacedIndex = 0;
            }

            return next;
         case UNBUFFER:
            int next = this.buf[this.unbufferIndex++];
            if (this.unbufferIndex == this.matchedIndex) {
               this.state = ReplacingInputStream.State.NOT_MATCHED;
               this.matchedIndex = 0;
            }

            return next;
      }
   }

   public String toString() {
      return this.state.name() + " " + this.matchedIndex + " " + this.replacedIndex + " " + this.unbufferIndex;
   }

   private enum State {
      NOT_MATCHED,
      MATCHING,
      REPLACING,
      UNBUFFER;
   }
}

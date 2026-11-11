package com.microsoft.sqlserver.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.MessageFormat;

class ReaderInputStream extends InputStream {
   private final Reader reader;
   private final Charset charset;
   private final long readerLength;
   private long readerCharsRead = 0L;
   private boolean atEndOfStream = false;
   private CharBuffer rawChars = null;
   private static final int MAX_CHAR_BUFFER_SIZE = 4000;
   private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
   private ByteBuffer encodedChars = EMPTY_BUFFER;
   private final byte[] oneByte = new byte[1];

   ReaderInputStream(Reader var1, String var2, long var3) throws UnsupportedEncodingException {
      assert var1 != null;
      assert var2 != null;
      assert -1L == var3 || var3 >= 0L;
      this.reader = var1;

      try {
         this.charset = Charset.forName(var2);
      } catch (IllegalCharsetNameException var6) {
         throw new UnsupportedEncodingException(var6.getMessage());
      } catch (UnsupportedCharsetException var7) {
         throw new UnsupportedEncodingException(var7.getMessage());
      }

      this.readerLength = var3;
   }

   public int available() throws IOException {
      assert null != this.reader;
      assert null != this.encodedChars;
      if (0L == this.readerLength) {
         return 0;
      } else if (this.encodedChars.remaining() > 0) {
         return this.encodedChars.remaining();
      } else {
         return this.reader.ready() ? 1 : 0;
      }
   }

   public int read() throws IOException {
      return -1 == this.readInternal(this.oneByte, 0, this.oneByte.length) ? -1 : this.oneByte[0];
   }

   public int read(byte[] var1) throws IOException {
      return this.readInternal(var1, 0, var1.length);
   }

   public int read(byte[] var1, int var2, int var3) throws IOException {
      return this.readInternal(var1, var2, var3);
   }

   private int readInternal(byte[] var1, int var2, int var3) throws IOException {
      assert null != var1;
      assert 0 <= var2 && var2 <= var1.length;
      assert 0 <= var3 && var3 <= var1.length;
      assert var2 <= var1.length - var3;
      if (0 == var3) {
         return 0;
      }

      int var4 = 0;

      while (var4 < var3 && this.encodeChars()) {
         int var5 = this.encodedChars.remaining();
         if (var5 > var3 - var4) {
            var5 = var3 - var4;
         }

         assert var5 > 0;
         this.encodedChars.get(var1, var2 + var4, var5);
         var4 += var5;
      }

      return 0 == var4 && this.atEndOfStream ? -1 : var4;
   }

   private boolean encodeChars() throws IOException {
      if (this.atEndOfStream) {
         return false;
      }

      if (this.encodedChars.hasRemaining()) {
         return true;
      }

      if (null == this.rawChars || !this.rawChars.hasRemaining()) {
         if (null == this.rawChars) {
            this.rawChars = CharBuffer.allocate(-1L != this.readerLength && this.readerLength <= 4000L ? Math.max((int)this.readerLength, 1) : 4000);
         } else {
            this.rawChars.clear();
         }

         while (true) {
            if (this.rawChars.hasRemaining()) {
               int var1 = this.rawChars.position();
               int var2 = 0;

               try {
                  var2 = this.reader.read(this.rawChars);
               } catch (Exception var6) {
                  String var4 = var6.getMessage();
                  if (null == var4) {
                     var4 = SQLServerException.getErrString("R_streamReadReturnedInvalidValue");
                  }

                  IOException var5 = new IOException(var4);
                  var5.initCause(var6);
                  throw var5;
               }

               if (var2 < -1 || 0 == var2) {
                  throw new IOException(SQLServerException.getErrString("R_streamReadReturnedInvalidValue"));
               }

               if (-1 != var2) {
                  assert var2 > 0;
                  if (var2 != this.rawChars.position() - var1) {
                     throw new IOException(SQLServerException.getErrString("R_streamReadReturnedInvalidValue"));
                  }

                  if (-1L != this.readerLength && var2 > this.readerLength - this.readerCharsRead) {
                     MessageFormat var8 = new MessageFormat(SQLServerException.getErrString("R_mismatchedStreamLength"));
                     throw new IOException(var8.format(new Object[]{this.readerLength, this.readerCharsRead}));
                  }

                  this.readerCharsRead += var2;
                  continue;
               }

               if (this.rawChars.position() != var1) {
                  throw new IOException(SQLServerException.getErrString("R_streamReadReturnedInvalidValue"));
               }

               if (-1L != this.readerLength && 0L != this.readerLength - this.readerCharsRead) {
                  MessageFormat var3 = new MessageFormat(SQLServerException.getErrString("R_mismatchedStreamLength"));
                  throw new IOException(var3.format(new Object[]{this.readerLength, this.readerCharsRead}));
               }

               if (0 == this.rawChars.position()) {
                  this.rawChars = null;
                  this.atEndOfStream = true;
                  return false;
               }
            }

            this.rawChars.flip();
            break;
         }
      }

      if (!this.rawChars.hasRemaining()) {
         return false;
      }

      this.encodedChars = this.charset.encode(this.rawChars);
      return true;
   }
}

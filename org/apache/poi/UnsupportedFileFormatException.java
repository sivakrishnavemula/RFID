package org.apache.poi;

public abstract class UnsupportedFileFormatException extends IllegalArgumentException {
   private static final long serialVersionUID = -8281969197282030046L;

   protected UnsupportedFileFormatException(String s) {
      super(s);
   }

   protected UnsupportedFileFormatException(String message, Throwable cause) {
      super(message, cause);
   }
}

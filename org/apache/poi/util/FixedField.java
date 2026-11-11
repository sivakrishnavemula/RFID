package org.apache.poi.util;

import java.io.IOException;
import java.io.InputStream;

public interface FixedField {
   void readFromBytes(byte[] var1) throws ArrayIndexOutOfBoundsException;

   void readFromStream(InputStream var1) throws IOException;

   void writeToBytes(byte[] var1) throws ArrayIndexOutOfBoundsException;

   String toString();
}

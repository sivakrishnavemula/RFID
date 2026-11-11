package com.microsoft.sqlserver.jdbc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

final class ByteArrayOutputStreamToInputStream extends ByteArrayOutputStream {
   ByteArrayInputStream getInputStream() throws SQLServerException {
      return new ByteArrayInputStream(this.buf, 0, this.count);
   }
}

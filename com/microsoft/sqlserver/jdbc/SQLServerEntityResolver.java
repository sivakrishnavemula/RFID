package com.microsoft.sqlserver.jdbc;

import java.io.StringReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

final class SQLServerEntityResolver implements EntityResolver {
   public InputSource resolveEntity(String var1, String var2) {
      return new InputSource(new StringReader(""));
   }
}

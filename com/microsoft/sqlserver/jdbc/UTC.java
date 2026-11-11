package com.microsoft.sqlserver.jdbc;

import java.util.SimpleTimeZone;
import java.util.TimeZone;

enum UTC {
   INSTANCE;

   static final TimeZone timeZone = new SimpleTimeZone(0, "UTC");
}

package com.microsoft.sqlserver.jdbc;

import java.sql.DriverPropertyInfo;
import java.util.Properties;

final class SQLServerDriverPropertyInfo {
   private final String name;
   private final String description;
   private final String defaultValue;
   private final boolean required;
   private final String[] choices;

   final String getName() {
      return this.name;
   }

   SQLServerDriverPropertyInfo(String var1, String var2, boolean var3, String[] var4) {
      this.name = var1;
      this.description = SQLServerResource.getResource("R_" + var1 + "PropertyDescription");
      this.defaultValue = var2;
      this.required = var3;
      this.choices = var4;
   }

   DriverPropertyInfo build(Properties var1) {
      String var2 = this.name.equals(SQLServerDriverStringProperty.PASSWORD.toString()) ? "" : var1.getProperty(this.name);
      if (null == var2) {
         var2 = this.defaultValue;
      }

      DriverPropertyInfo var3 = new DriverPropertyInfo(this.name, var2);
      var3.description = this.description;
      var3.required = this.required;
      var3.choices = this.choices;
      return var3;
   }
}

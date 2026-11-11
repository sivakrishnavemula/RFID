package com.microsoft.sqlserver.jdbc;

final class SQLIdentifier {
   private String serverName = "";
   private String databaseName = "";
   private String schemaName = "";
   private String objectName = "";

   final String getServerName() {
      return this.serverName;
   }

   final void setServerName(String var1) {
      this.serverName = var1;
   }

   final String getDatabaseName() {
      return this.databaseName;
   }

   final void setDatabaseName(String var1) {
      this.databaseName = var1;
   }

   final String getSchemaName() {
      return this.schemaName;
   }

   final void setSchemaName(String var1) {
      this.schemaName = var1;
   }

   final String getObjectName() {
      return this.objectName;
   }

   final void setObjectName(String var1) {
      this.objectName = var1;
   }

   final String asEscapedString() {
      StringBuilder var1 = new StringBuilder(256);
      if (this.serverName.length() > 0) {
         var1.append("[" + this.serverName + "].");
      }

      if (this.databaseName.length() > 0) {
         var1.append("[" + this.databaseName + "].");
      } else {
         assert 0 == this.serverName.length();
      }

      if (this.schemaName.length() > 0) {
         var1.append("[" + this.schemaName + "].");
      } else if (this.databaseName.length() > 0) {
         var1.append('.');
      }

      var1.append("[" + this.objectName + "]");
      return var1.toString();
   }
}

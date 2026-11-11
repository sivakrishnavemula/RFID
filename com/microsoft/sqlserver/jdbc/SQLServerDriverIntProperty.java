package com.microsoft.sqlserver.jdbc;

enum SQLServerDriverIntProperty {
   PACKET_SIZE("packetSize", 8000),
   LOCK_TIMEOUT("lockTimeout", -1),
   LOGIN_TIMEOUT("loginTimeout", 15),
   PORT_NUMBER("portNumber", 1433);

   private String name;
   private int defaultValue;

   SQLServerDriverIntProperty(String var3, int var4) {
      this.name = var3;
      this.defaultValue = var4;
   }

   int getDefaultValue() {
      return this.defaultValue;
   }

   public String toString() {
      return this.name;
   }
}

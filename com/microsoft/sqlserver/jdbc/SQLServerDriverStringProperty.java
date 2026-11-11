package com.microsoft.sqlserver.jdbc;

enum SQLServerDriverStringProperty {
   APPLICATION_INTENT("applicationIntent", ApplicationIntent.READ_WRITE.toString()),
   APPLICATION_NAME("applicationName", "Microsoft JDBC Driver for SQL Server"),
   DATABASE_NAME("databaseName", ""),
   FAILOVER_PARTNER("failoverPartner", ""),
   HOSTNAME_IN_CERTIFICATE("hostNameInCertificate", ""),
   INSTANCE_NAME("instanceName", ""),
   PASSWORD("password", ""),
   RESPONSE_BUFFERING("responseBuffering", "adaptive"),
   SELECT_METHOD("selectMethod", "direct"),
   SERVER_NAME("serverName", ""),
   SERVER_SPN("serverSpn", ""),
   TRUST_STORE("trustStore", ""),
   TRUST_STORE_PASSWORD("trustStorePassword", ""),
   USER("user", ""),
   WORKSTATION_ID("workstationID", ""),
   AUTHENTICATION_SCHEME("authenticationScheme", AuthenticationScheme.nativeAuthentication.toString()),
   AUTHENTICATION("authentication", SqlAuthentication.NotSpecified.toString()),
   ACCESS_TOKEN("accessToken", ""),
   COLUMN_ENCRYPTION("columnEncryptionSetting", ColumnEncryptionSetting.Disabled.toString()),
   KEY_STORE_AUTHENTICATION("keyStoreAuthentication", ""),
   KEY_STORE_SECRET("keyStoreSecret", ""),
   KEY_STORE_LOCATION("keyStoreLocation", "");

   private String name;
   private String defaultValue;

   SQLServerDriverStringProperty(String var3, String var4) {
      this.name = var3;
      this.defaultValue = var4;
   }

   String getDefaultValue() {
      return this.defaultValue;
   }

   public String toString() {
      return this.name;
   }
}

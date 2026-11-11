package com.microsoft.sqlserver.jdbc;

enum SQLServerDriverBooleanProperty {
   DISABLE_STATEMENT_POOLING("disableStatementPooling", true),
   ENCRYPT("encrypt", false),
   INTEGRATED_SECURITY("integratedSecurity", false),
   LAST_UPDATE_COUNT("lastUpdateCount", true),
   MULTI_SUBNET_FAILOVER("multiSubnetFailover", false),
   SERVER_NAME_AS_ACE("serverNameAsACE", false),
   SEND_STRING_PARAMETERS_AS_UNICODE("sendStringParametersAsUnicode", true),
   SEND_TIME_AS_DATETIME("sendTimeAsDatetime", true),
   TRANSPARENT_NETWORK_IP_RESOLUTION("TransparentNetworkIPResolution", true),
   TRUST_SERVER_CERTIFICATE("trustServerCertificate", false),
   XOPEN_STATES("xopenStates", false);

   private String name;
   private boolean defaultValue;

   SQLServerDriverBooleanProperty(String var3, boolean var4) {
      this.name = var3;
      this.defaultValue = var4;
   }

   boolean getDefaultValue() {
      return this.defaultValue;
   }

   public String toString() {
      return this.name;
   }
}

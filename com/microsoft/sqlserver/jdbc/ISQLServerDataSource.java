package com.microsoft.sqlserver.jdbc;

import javax.sql.CommonDataSource;

public interface ISQLServerDataSource extends CommonDataSource {
   void setApplicationIntent(String var1);

   String getApplicationIntent();

   void setApplicationName(String var1);

   String getApplicationName();

   void setDatabaseName(String var1);

   String getDatabaseName();

   void setInstanceName(String var1);

   String getInstanceName();

   void setIntegratedSecurity(boolean var1);

   void setLastUpdateCount(boolean var1);

   boolean getLastUpdateCount();

   void setEncrypt(boolean var1);

   boolean getEncrypt();

   void setTrustServerCertificate(boolean var1);

   boolean getTrustServerCertificate();

   void setTrustStore(String var1);

   String getTrustStore();

   void setTrustStorePassword(String var1);

   void setHostNameInCertificate(String var1);

   String getHostNameInCertificate();

   void setLockTimeout(int var1);

   int getLockTimeout();

   void setPassword(String var1);

   void setPortNumber(int var1);

   int getPortNumber();

   void setSelectMethod(String var1);

   String getSelectMethod();

   void setResponseBuffering(String var1);

   String getResponseBuffering();

   void setSendTimeAsDatetime(boolean var1);

   boolean getSendTimeAsDatetime();

   void setSendStringParametersAsUnicode(boolean var1);

   boolean getSendStringParametersAsUnicode();

   void setServerName(String var1);

   String getServerName();

   void setFailoverPartner(String var1);

   String getFailoverPartner();

   void setMultiSubnetFailover(boolean var1);

   boolean getMultiSubnetFailover();

   void setUser(String var1);

   String getUser();

   void setWorkstationID(String var1);

   String getWorkstationID();

   void setXopenStates(boolean var1);

   boolean getXopenStates();

   void setURL(String var1);

   String getURL();

   void setDescription(String var1);

   String getDescription();

   void setPacketSize(int var1);

   int getPacketSize();

   void setAuthenticationScheme(String var1);

   void setServerSpn(String var1);

   String getServerSpn();
}

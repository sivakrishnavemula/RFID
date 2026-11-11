package com.microsoft.sqlserver.jdbc;

import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials;
import com.microsoft.windowsazure.core.pipeline.filter.ServiceRequestContext;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

class KeyVaultCredential extends KeyVaultCredentials {
   private final String accessTokenType = "Bearer";
   SQLServerKeyVaultAuthenticationCallback authenticationCallback = null;
   String clientId = null;
   String clientKey = null;
   String accessToken = null;

   KeyVaultCredential(SQLServerKeyVaultAuthenticationCallback var1) {
      this.authenticationCallback = var1;
   }

   public Header doAuthenticate(ServiceRequestContext var1, Map<String, String> var2) {
      assert null != var2;
      String var3 = (String)var2.get("authorization");
      String var4 = (String)var2.get("resource");
      this.accessToken = this.authenticationCallback.getAccessToken(var3, var4, "");
      return new BasicHeader("Authorization", "Bearer " + this.accessToken);
   }

   void setAccessToken(String var1) {
      this.accessToken = var1;
   }
}

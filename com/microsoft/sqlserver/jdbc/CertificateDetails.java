package com.microsoft.sqlserver.jdbc;

import java.security.Key;
import java.security.cert.X509Certificate;

class CertificateDetails {
   X509Certificate certificate;
   Key privateKey;

   CertificateDetails(X509Certificate var1, Key var2) {
      this.certificate = var1;
      this.privateKey = var2;
   }
}

package com.rfid;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class Rfid_properties {
   public static String[] props() throws IOException {
      String[] ar = new String[7];
      String baseDir = System.getProperty("user.dir");
      if (LoginPage.path != null && !LoginPage.path.trim().isEmpty()) {
         baseDir = LoginPage.path;
      }

      File propertiesFile = new File(baseDir, "rfid.properties");
      if (!propertiesFile.exists() || !propertiesFile.isFile()) {
         ar[0] = "localhost";
         ar[1] = "RFIDDB";
         ar[2] = "sa";
         ar[3] = "password";
         ar[4] = "127.0.0.1";
         ar[5] = "2112";
         ar[6] = "00000000";
         return ar;
      }

      Properties p = new Properties();
      try (FileReader reader = new FileReader(propertiesFile)) {
         p.load(reader);
      }

      ar[0] = p.getProperty("db.sqlname", "localhost");
      ar[1] = p.getProperty("db.databaseName", "RFIDDB");
      ar[2] = p.getProperty("db.Username", "sa");
      ar[3] = p.getProperty("db.password", "password");
      ar[4] = p.getProperty("rfid.ip", "127.0.0.1");
      ar[5] = p.getProperty("rfid.port", "2112");
      ar[6] = p.getProperty("rfid.password", "00000000");
      return ar;
   }
}

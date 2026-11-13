package com.rfid;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class sql_connection {
   public static Connection CreateConnection() {
      try {
         String[] ar = Rfid_properties.props();
         if (ar == null || ar.length < 4) {
            return null;
         }

         String url = "jdbc:sqlserver://" + ar[0] + ";databaseName=" + ar[1]
               + ";encrypt=true;trustServerCertificate=true";
         String username = ar[2];
         String password = ar[3];
         return DriverManager.getConnection(url, username, password);
      } catch (SQLException ee) {
         System.err.println("Database connection is not configured or SQL Server driver is unavailable: " + ee.getMessage());
      } catch (Exception e) {
         System.err.println("Unable to create database connection: " + e.getMessage());
      }

      return null;
   }

   public static void CloseConnection(Connection connection) {
      try {
         if (connection != null && !connection.isClosed()) {
            connection.close();
         }
      } catch (SQLException e) {
         e.printStackTrace();
      }
   }
}

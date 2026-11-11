package com.rfid;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import javax.swing.JFileChooser;

public class ConvertExcel {
   static String outputDirPath;
   static String fileName;

   public static String convertToExcel(ResultSet rs) throws SQLException, IOException {
      String[] ar = dirSelect();
      String outputDir = ar[1];
      String selectedName = ar[0];
      String status;

      if (selectedName != null && !selectedName.trim().isEmpty()) {
         String outputFilePath = new File(outputDir, selectedName + ".csv").getPath();
         try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilePath))) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
               if (i > 1) {
                  writer.print(",");
               }
               writer.print(escapeCsv(metaData.getColumnLabel(i)));
            }
            writer.println();

            while (rs.next()) {
               for (int i = 1; i <= columnCount; i++) {
                  if (i > 1) {
                     writer.print(",");
                  }
                  writer.print(escapeCsv(rs.getString(i)));
               }
               writer.println();
            }
         }
         status = "File created";
      } else {
         status = "File not created";
      }

      return status;
   }

   public static String[] dirSelect() {
      String[] ar = new String[2];
      JFileChooser c = new JFileChooser();
      c.setDialogTitle("Save");
      int rval = c.showSaveDialog(null);
      if (rval == 0) {
         ar[0] = c.getSelectedFile().getName();
         ar[1] = c.getCurrentDirectory().toString();
      }
      return ar;
   }

   private static String escapeCsv(String value) {
      if (value == null) {
         return "";
      }
      return value.replace("\"", "\"\"");
   }
}

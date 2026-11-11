package com.rfid;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class Order_table_client {
   public static boolean resultSetToTableModel(ResultSet rs, JTable table) throws SQLException {
      DefaultTableModel tableModel = new DefaultTableModel();
      ResultSetMetaData metaData = rs.getMetaData();
      int columnCount = metaData.getColumnCount();

      for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
         tableModel.addColumn(metaData.getColumnLabel(columnIndex));
      }

      Object[] row = new Object[columnCount];
      System.out.println(table.getName());
      System.out.println(columnCount);

      while (rs.next()) {
         for (int i = 0; i < columnCount; i++) {
            row[i] = rs.getObject(i + 1);
         }

         tableModel.addRow(row);
      }

      table.setModel(tableModel);
      return true;
   }
}

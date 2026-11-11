package com.rfid;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

public class TagWrite_Read {
   private JFrame frame;
   static JComboBox comboBox;
   private JTextField textField;
   private JTextField textField_1;
   private JTextField textField_2;
   private JTextField textField_3;
   private JTextField Tagpassword;
   private JTextField Tagreadresult;
   private JTextField PasswordResult;
   private JTextField TagAccessStatus;
   private JButton button;
   private JButton button_1;
   private JButton button_3;
   static JComboBox comboBox_1;
   static JComboBox<String> comboBox_2;
   private JTextField textField_16;
   private static JTable table;
   static JTextArea textArea;
   static JButton btnNewButton;
   private JLabel lblLastCreatedMhe;
   private JTextField textField_10;
   private JButton btnPrintMheNo;
   static JTextField txtMhrcp;
   private JTextField textField_12;
   private JTextField textField_17;
   private static JTable table_1;
   static JTextArea txtNotwritten;
   static JTextArea WriteSuccessv;
   private JTextArea TagreadCmd;
   private JTextArea ReadSucess;
   static String rfidIP = null;
   static String rfidPassword = null;
   static int rfidPort = 0;
   private JTextField textField_4;

   public static void main(String[] args) {
      EventQueue.invokeLater(new Runnable() {
         public void run() {
            try {
               TagWrite_Read window = new TagWrite_Read();
               window.frame.setVisible(true);
               TagWrite_Read.txtMhrcp.requestFocus();

               try {
                  String[] ar = Rfid_properties.props();
                  TagWrite_Read.rfidIP = ar[4];
                  TagWrite_Read.rfidPort = Integer.parseInt(ar[5]);
                  TagWrite_Read.rfidPassword = ar[6];
               } catch (IOException e2) {
                  e2.printStackTrace();
               }
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });
   }

   public TagWrite_Read() {
      this.initialize();
   }

   private void initialize() {
      this.frame = new JFrame();
      this.frame.setBounds(100, 100, 1009, 561);
      this.frame.setDefaultCloseOperation(3);
      JTabbedPane tabbedPane = new JTabbedPane(1);
      this.frame.getContentPane().add(tabbedPane, "Center");
      JPanel panel_1 = new JPanel();
      tabbedPane.addTab("RFID tag read/write", null, panel_1, null);
      panel_1.setLayout(null);
      this.Tagpassword = new JTextField();
      this.Tagpassword.setToolTipText("8 zeros means no password");
      this.Tagpassword.setText("00000000");
      this.Tagpassword.setColumns(10);
      this.Tagpassword.setBounds(98, 461, 86, 20);
      panel_1.add(this.Tagpassword);
      this.Tagreadresult = new JTextField();
      this.Tagreadresult.setFont(new Font("Tahoma", 0, 13));
      this.Tagreadresult.setEditable(false);
      this.Tagreadresult.setColumns(10);
      this.Tagreadresult.setBounds(240, 152, 152, 26);
      panel_1.add(this.Tagreadresult);
      this.PasswordResult = new JTextField();
      this.PasswordResult.setText("NotWritten");
      this.PasswordResult.setFont(new Font("Tahoma", 0, 9));
      this.PasswordResult.setEditable(false);
      this.PasswordResult.setColumns(10);
      this.PasswordResult.setBounds(194, 461, 304, 20);
      panel_1.add(this.PasswordResult);
      this.TagAccessStatus = new JTextField();
      this.TagAccessStatus.setFont(new Font("Tahoma", 0, 9));
      this.TagAccessStatus.setEditable(false);
      this.TagAccessStatus.setColumns(10);
      this.TagAccessStatus.setBounds(524, 461, 217, 20);
      panel_1.add(this.TagAccessStatus);
      this.button = new JButton("TagAccess");
      this.button.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            String[] args = new String[3];
            TagWrite_Read.this.PasswordResult.setText("");
            TagWrite_Read.this.TagAccessStatus.setText("");
            args = Tcp_ip_Socket.TagAccess(TagWrite_Read.rfidIP, TagWrite_Read.rfidPort, TagWrite_Read.rfidPassword);
            TagWrite_Read.this.PasswordResult.setText(args[0]);
            TagWrite_Read.this.TagAccessStatus.setText(args[1]);
         }
      });
      this.button.setBounds(765, 460, 89, 23);
      panel_1.add(this.button);
      this.button_1 = new JButton("Tag Write");
      this.button_1.setEnabled(false);
      this.button_1
         .addActionListener(
            new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                  int tagname_count = 1;
                  String[] Result = new String[2];
                  String[] invResult = new String[2];
                  TagWrite_Read.txtNotwritten.setText("");
                  TagWrite_Read.WriteSuccessv.setText("");
                  TagWrite_Read.this.textField_4.setText("");

                  try {
                     String[] ar = Rfid_properties.props();
                     invResult = ReadInventory.TagRead(TagWrite_Read.rfidIP, TagWrite_Read.rfidPort);
                  } catch (IOException e2) {
                     e2.printStackTrace();
                  }

                  if (invResult[2].contains("Success")) {
                     String tag_name = TagWrite_Read.txtMhrcp.getText();
                     String username = LoginPage.username;

                     try {
                        Connection conn = sql_connection.CreateConnection();
                        Statement st = conn.createStatement();
                        ResultSet rs1 = st.executeQuery("SELECT COUNT(rfid_tag_name) FROM tag_details WHERE rfid_tag_name='" + tag_name + "'");

                        while (rs1.next()) {
                           tagname_count = Integer.parseInt(rs1.getString(1));
                        }

                        if (tagname_count == 0) {
                           Result = Tcp_ip_Socket.TagWrite(TagWrite_Read.rfidIP, TagWrite_Read.rfidPort, tag_name);
                           TagWrite_Read.WriteSuccessv.setForeground(Color.GREEN);
                           TagWrite_Read.txtNotwritten.setForeground(Color.GREEN);
                           TagWrite_Read.txtNotwritten.setText(Result[0]);
                           TagWrite_Read.WriteSuccessv.setText(Result[1]);
                           if (Result[1].contains("Success")) {
                              DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                              LocalDateTime now = LocalDateTime.now();
                              String row_insert_time = dtf.format(now);
                              String equipment = TagWrite_Read.this.textField_12.getText();
                              String MHE = TagWrite_Read.this.textField_17.getText();
                              String query = "INSERT INTO tag_details \r\n(row_insert_time,rfid_tag_name,username,equipment,MHE) \r\nVALUES ('"
                                 + row_insert_time
                                 + "','"
                                 + tag_name
                                 + "','"
                                 + username
                                 + "','"
                                 + equipment
                                 + "','"
                                 + MHE
                                 + "') ";
                              st.execute(query);
                              ResultSet rs = st.executeQuery("SELECT row_id,row_insert_time,rfid_tag_name,username,equipment,MHE FROM tag_details");
                              Order_table_client.resultSetToTableModel(rs, TagWrite_Read.table_1);
                           }
                        } else {
                           TagWrite_Read.WriteSuccessv.setForeground(Color.RED);
                           TagWrite_Read.WriteSuccessv.setText("Tag id already written");
                        }

                        sql_connection.CloseConnection(conn);
                     } catch (SQLException e1) {
                        e1.printStackTrace();
                     }
                  } else {
                     TagWrite_Read.WriteSuccessv.setForeground(Color.RED);
                     TagWrite_Read.txtNotwritten.setForeground(Color.RED);
                     TagWrite_Read.txtNotwritten.setText(invResult[0]);
                     TagWrite_Read.WriteSuccessv.setText(invResult[1]);
                  }

                  TagWrite_Read.this.Tagreadresult.setText("");
                  TagWrite_Read.this.TagreadCmd.setText("");
                  TagWrite_Read.this.ReadSucess.setText("");
               }
            }
         );
      this.button_1.setBounds(418, 84, 152, 30);
      panel_1.add(this.button_1);
      this.button_3 = new JButton("Tag Read");
      this.button_3.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            String[] args1 = new String[3];
            TagWrite_Read.this.Tagreadresult.setText("");
            TagWrite_Read.this.TagreadCmd.setText("");
            TagWrite_Read.this.ReadSucess.setText("");
            TagWrite_Read.this.textField_4.setText("");
            args1 = Tcp_ip_Socket.TagRead(TagWrite_Read.rfidIP, TagWrite_Read.rfidPort);
            TagWrite_Read.this.Tagreadresult.setText(args1[0]);
            System.out.println(args1[0].length());
            TagWrite_Read.this.TagreadCmd.setText(args1[1]);
            TagWrite_Read.this.ReadSucess.setText(args1[2]);
            TagWrite_Read.txtNotwritten.setText("");
            TagWrite_Read.WriteSuccessv.setText("");
         }
      });
      this.button_3.setBounds(78, 150, 152, 30);
      panel_1.add(this.button_3);
      txtMhrcp = new JTextField();
      txtMhrcp.addKeyListener(new KeyAdapter() {
         public void keyReleased(KeyEvent e) {
            if (TagWrite_Read.txtMhrcp.getText().length() == 9) {
               e.consume();
               String barcodeInput = TagWrite_Read.txtMhrcp.getText();
               String tagPrefix = barcodeInput.substring(0, 5);

               try {
                  Connection conn = sql_connection.CreateConnection();
                  Statement st = conn.createStatement();
                  String query = "SELECT product_line,mhe_name FROM productline_mhename WHERE tag_prefix ='" + tagPrefix + "'";
                  ResultSet rs1 = st.executeQuery(query);

                  while (rs1.next()) {
                     TagWrite_Read.this.textField_12.setText(rs1.getString(1));
                     TagWrite_Read.this.textField_17.setText(rs1.getString(2));
                  }

                  sql_connection.CloseConnection(conn);
               } catch (SQLException e1) {
                  e1.printStackTrace();
               }

               TagWrite_Read.this.button_1.setEnabled(true);
            } else {
               TagWrite_Read.this.textField_12.setText("");
               TagWrite_Read.this.textField_17.setText("");
               TagWrite_Read.this.button_1.setEnabled(false);
            }
         }
      });
      txtMhrcp.setFont(new Font("Tahoma", 0, 13));
      txtMhrcp.setColumns(10);
      txtMhrcp.setBounds(143, 31, 217, 30);
      panel_1.add(txtMhrcp);
      JLabel lblBarcode = new JLabel("Barcode");
      lblBarcode.setFont(new Font("Tahoma", 1, 12));
      lblBarcode.setBounds(86, 29, 98, 30);
      panel_1.add(lblBarcode);
      JLabel label_1 = new JLabel("Product Line");
      label_1.setFont(new Font("Tahoma", 1, 12));
      label_1.setBounds(406, 29, 98, 30);
      panel_1.add(label_1);
      JLabel label_4 = new JLabel("MHE Name");
      label_4.setFont(new Font("Tahoma", 1, 12));
      label_4.setBounds(614, 29, 98, 30);
      panel_1.add(label_4);
      this.textField_12 = new JTextField();
      this.textField_12.setBackground(Color.WHITE);
      this.textField_12.setEditable(false);
      this.textField_12.setFont(new Font("Tahoma", 0, 13));
      this.textField_12.setColumns(10);
      this.textField_12.setBounds(497, 31, 74, 30);
      panel_1.add(this.textField_12);
      this.textField_17 = new JTextField();
      this.textField_17.setBackground(Color.WHITE);
      this.textField_17.setEditable(false);
      this.textField_17.setFont(new Font("Tahoma", 0, 13));
      this.textField_17.setColumns(10);
      this.textField_17.setBounds(688, 29, 217, 30);
      panel_1.add(this.textField_17);
      JScrollPane scrollPane_1 = new JScrollPane();
      scrollPane_1.setBounds(78, 203, 827, 183);
      panel_1.add(scrollPane_1);
      table_1 = new JTable();
      scrollPane_1.setViewportView(table_1);
      JButton btnUpdateTable = new JButton("Update Table");
      btnUpdateTable.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            TagWrite_Read.this.textField_4.setText("");

            try {
               Connection conn = sql_connection.CreateConnection();
               Statement st = conn.createStatement();
               ResultSet rs = st.executeQuery("SELECT row_insert_time,rfid_tag_name,username,equipment,MHE FROM tag_details");
               Order_table_client.resultSetToTableModel(rs, TagWrite_Read.table_1);
               sql_connection.CloseConnection(conn);
            } catch (SQLException e1) {
               e1.printStackTrace();
            }
         }
      });
      btnUpdateTable.setBounds(753, 397, 152, 30);
      panel_1.add(btnUpdateTable);
      txtNotwritten = new JTextArea();
      txtNotwritten.setFont(new Font("Tahoma", 0, 14));
      txtNotwritten.setBackground(UIManager.getColor("CheckBox.background"));
      txtNotwritten.setBounds(78, 85, 314, 30);
      panel_1.add(txtNotwritten);
      WriteSuccessv = new JTextArea();
      WriteSuccessv.setFont(new Font("Tahoma", 0, 14));
      WriteSuccessv.setBackground(UIManager.getColor("CheckBox.background"));
      WriteSuccessv.setBounds(596, 83, 314, 30);
      panel_1.add(WriteSuccessv);
      this.TagreadCmd = new JTextArea();
      this.TagreadCmd.setFont(new Font("Tahoma", 0, 13));
      this.TagreadCmd.setBackground(SystemColor.menu);
      this.TagreadCmd.setBounds(407, 152, 290, 26);
      panel_1.add(this.TagreadCmd);
      this.ReadSucess = new JTextArea();
      this.ReadSucess.setFont(new Font("Tahoma", 0, 13));
      this.ReadSucess.setBackground(SystemColor.menu);
      this.ReadSucess.setBounds(707, 153, 271, 24);
      panel_1.add(this.ReadSucess);
      this.textField_4 = new JTextField();
      this.textField_4.setBounds(78, 397, 159, 30);
      panel_1.add(this.textField_4);
      this.textField_4.setColumns(10);
      JButton btnS = new JButton("Search");
      btnS.setFont(new Font("Tahoma", 0, 13));
      btnS.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            String search = TagWrite_Read.this.textField_4.getText();

            try {
               Connection conn = sql_connection.CreateConnection();
               Statement st = conn.createStatement();
               ResultSet rs = st.executeQuery("SELECT row_insert_time,rfid_tag_name,username,equipment,MHE FROM tag_details WHERE MHE like '%" + search + "%'");
               Order_table_client.resultSetToTableModel(rs, TagWrite_Read.table_1);
               sql_connection.CloseConnection(conn);
            } catch (SQLException e1) {
               e1.printStackTrace();
            }
         }
      });
      btnS.setBounds(247, 397, 117, 30);
      panel_1.add(btnS);
      JPanel panel = new JPanel();
      tabbedPane.addTab("Barcode creation", null, panel, null);
      panel.setLayout(null);
      JLabel label = new JLabel("Product Line");
      label.setFont(new Font("Tahoma", 1, 12));
      label.setBounds(71, 32, 98, 30);
      panel.add(label);
      comboBox_1 = new JComboBox();
      comboBox_1.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            TagWrite_Read.textArea.setText("");
            if (TagWrite_Read.comboBox_1.getSelectedItem() != null) {
               String product_line = (String)TagWrite_Read.comboBox_1.getSelectedItem();

               try {
                  Connection conn = sql_connection.CreateConnection();
                  Statement st = conn.createStatement();
                  String query = "SELECT mhe_name FROM productline_mhename WHERE product_line='" + product_line + "'";
                  ResultSet rs = st.executeQuery(query);
                  TagWrite_Read.comboBox_2.removeAllItems();

                  while (rs.next()) {
                     TagWrite_Read.comboBox_2.addItem(rs.getString("mhe_name"));
                  }

                  TagWrite_Read.comboBox_2.addItem(" ");
                  TagWrite_Read.comboBox_2.setSelectedItem(" ");
                  TagWrite_Read.this.textField_16.setText("");
                  TagWrite_Read.this.textField_2.setText("");
                  TagWrite_Read.this.textField_3.setText("");
                  TagWrite_Read.this.textField_10.setText("");
                  sql_connection.CloseConnection(conn);
               } catch (SQLException e1) {
                  e1.printStackTrace();
               }
            }
         }
      });
      comboBox_1.setModel(new DefaultComboBoxModel(new String[]{"RMS", "Mixer", "STC", "PCR", "TBR"}));
      comboBox_1.setBounds(161, 33, 98, 30);
      panel.add(comboBox_1);
      JLabel lblMheName = new JLabel("MHE Name");
      lblMheName.setFont(new Font("Tahoma", 1, 12));
      lblMheName.setBounds(373, 32, 122, 30);
      panel.add(lblMheName);
      comboBox_2 = new JComboBox();
      comboBox_2.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            TagWrite_Read.textArea.setText("");
            if (TagWrite_Read.comboBox_1.getSelectedItem() != null && TagWrite_Read.comboBox_2.getSelectedItem() != null) {
               String mhe_name = (String)TagWrite_Read.comboBox_2.getSelectedItem();
               String tagPrefix = null;

               try {
                  Connection conn = sql_connection.CreateConnection();
                  Statement st = conn.createStatement();
                  String query = "SELECT tag_prefix FROM productline_mhename WHERE mhe_name ='" + mhe_name + "'";

                  for (ResultSet rs = st.executeQuery(query); rs.next(); tagPrefix = rs.getString("tag_prefix")) {
                     TagWrite_Read.this.textField_16.setText(rs.getString("tag_prefix"));
                  }

                  ResultSet rs1 = st.executeQuery("SELECT product_line,mhe_name,mhe_no FROM mhe_number WHERE mhe_name='" + mhe_name + "'");
                  Order_table_client.resultSetToTableModel(rs1, TagWrite_Read.table);
                  ResultSet rs2 = st.executeQuery("SELECT MAX(mhe_no) FROM mhe_number WHERE mhe_name='" + mhe_name + "'");

                  while (rs2.next()) {
                     if (rs2.getString(1) == null) {
                        TagWrite_Read.this.textField_10.setText(tagPrefix + "0000");
                     } else {
                        TagWrite_Read.this.textField_10.setText(rs2.getString(1));
                     }
                  }

                  sql_connection.CloseConnection(conn);
               } catch (SQLException e1) {
                  e1.printStackTrace();
               }

               TagWrite_Read.btnNewButton.setEnabled(true);
               TagWrite_Read.this.btnPrintMheNo.setEnabled(true);
            }
         }
      });
      comboBox_2.setBounds(452, 33, 217, 30);
      panel.add(comboBox_2);
      this.textField_2 = new JTextField();
      this.textField_2.setColumns(10);
      this.textField_2.setBounds(222, 118, 86, 30);
      panel.add(this.textField_2);
      JLabel label_2 = new JLabel("From");
      label_2.setFont(new Font("Tahoma", 1, 12));
      label_2.setBounds(176, 117, 48, 30);
      panel.add(label_2);
      JLabel label_3 = new JLabel("To");
      label_3.setFont(new Font("Tahoma", 1, 12));
      label_3.setBounds(381, 116, 48, 30);
      panel.add(label_3);
      this.textField_3 = new JTextField();
      this.textField_3.setColumns(10);
      this.textField_3.setBounds(415, 117, 80, 30);
      panel.add(this.textField_3);
      JLabel lblTagPrefix = new JLabel("Tag prefix");
      lblTagPrefix.setFont(new Font("Tahoma", 1, 12));
      lblTagPrefix.setBounds(752, 32, 111, 30);
      panel.add(lblTagPrefix);
      this.textField_16 = new JTextField();
      this.textField_16.setEditable(false);
      this.textField_16.setColumns(10);
      this.textField_16.setBounds(825, 33, 86, 30);
      panel.add(this.textField_16);
      JScrollPane scrollPane = new JScrollPane();
      scrollPane.setBounds(111, 269, 784, 214);
      panel.add(scrollPane);
      table = new JTable();
      scrollPane.setViewportView(table);
      btnNewButton = new JButton("Create MHE No");
      btnNewButton.setEnabled(false);
      btnNewButton.addActionListener(
         new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               int from = 0;
               int to = 0;

               try {
                  from = Integer.parseInt(TagWrite_Read.this.textField_2.getText());
                  to = Integer.parseInt(TagWrite_Read.this.textField_3.getText());
               } catch (NumberFormatException n) {
                  TagWrite_Read.textArea.setForeground(Color.RED);
                  TagWrite_Read.textArea.setText("Enter only numbers");
               }

               String product_line = (String)TagWrite_Read.comboBox_1.getSelectedItem();
               String mhe_name = (String)TagWrite_Read.comboBox_2.getSelectedItem();
               String mheNo_prefix = TagWrite_Read.this.textField_16.getText();
               String lastMheNo = TagWrite_Read.this.textField_10.getText();
               int lastFourDigit = Integer.parseInt(lastMheNo.substring(lastMheNo.length() - 4));
               int fromNo = Integer.parseInt(String.format("%04d", new Object[]{from}));
               int mhe_no_count = 0;
               if (fromNo == lastFourDigit + 1) {
                  try {
                     Connection conn = sql_connection.CreateConnection();
                     Statement st = conn.createStatement();

                     for (int i = from; i <= to; i++) {
                        String s = String.format("%04d", new Object[]{i});
                        ResultSet rs1 = st.executeQuery("SELECT DISTINCT COUNT(mhe_no) FROM mhe_number WHERE mhe_no='" + mheNo_prefix + s + "'");

                        while (rs1.next()) {
                           mhe_no_count = Integer.parseInt(rs1.getString(1));
                        }

                        if (mhe_no_count == 0) {
                           String query = "INSERT INTO mhe_number \r\n(product_line,mhe_name,mhe_no) \r\nVALUES ('"
                              + product_line
                              + "','"
                              + mhe_name
                              + "','"
                              + mheNo_prefix
                              + s
                              + "') ";
                           st.execute(query);
                        } else {
                           TagWrite_Read.textArea.setText(mheNo_prefix + s + " is available");
                        }
                     }

                     ResultSet rs = st.executeQuery("SELECT product_line,mhe_name,mhe_no FROM mhe_number WHERE mhe_name='" + mhe_name + "'");
                     Order_table_client.resultSetToTableModel(rs, TagWrite_Read.table);
                     ResultSet rs2 = st.executeQuery("SELECT MAX(mhe_no) FROM mhe_number WHERE mhe_name='" + mhe_name + "'");

                     while (rs2.next()) {
                        TagWrite_Read.this.textField_10.setText(rs2.getString(1));
                     }

                     sql_connection.CloseConnection(conn);
                  } catch (SQLException e1) {
                     e1.printStackTrace();
                  }
               } else {
                  TagWrite_Read.textArea.setText("Enter correct numbers");
               }
            }
         }
      );
      btnNewButton.setBounds(204, 206, 122, 26);
      panel.add(btnNewButton);
      textArea = new JTextArea();
      textArea.setForeground(Color.RED);
      textArea.setBackground(UIManager.getColor("CheckBox.background"));
      textArea.setEditable(false);
      textArea.setBounds(601, 164, 310, 94);
      panel.add(textArea);
      this.lblLastCreatedMhe = new JLabel("Last created MHE No");
      this.lblLastCreatedMhe.setFont(new Font("Tahoma", 1, 12));
      this.lblLastCreatedMhe.setBounds(553, 116, 161, 30);
      panel.add(this.lblLastCreatedMhe);
      this.textField_10 = new JTextField();
      this.textField_10.setEditable(false);
      this.textField_10.setColumns(10);
      this.textField_10.setBounds(691, 117, 111, 30);
      panel.add(this.textField_10);
      this.btnPrintMheNo = new JButton("Export MHE No");
      this.btnPrintMheNo
         .addActionListener(
            new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                  int from = Integer.parseInt(TagWrite_Read.this.textField_2.getText());
                  int to = Integer.parseInt(TagWrite_Read.this.textField_3.getText());
                  String sFrom = String.format("%04d", new Object[]{from});
                  String sTo = String.format("%04d", new Object[]{to});
                  String product_line = (String)TagWrite_Read.comboBox_1.getSelectedItem();
                  String mhe_name = (String)TagWrite_Read.comboBox_2.getSelectedItem();
                  String mheNo_prefix = TagWrite_Read.this.textField_16.getText();
                  String fromMheNO = mheNo_prefix + sFrom;
                  String toMheNO = mheNo_prefix + sTo;
                  if (TagWrite_Read.this.textField_2.getText() != null
                     && TagWrite_Read.this.textField_3.getText() != null
                     && product_line != null
                     && mhe_name != null
                     && mheNo_prefix != null) {
                     try {
                        Connection conn = sql_connection.CreateConnection();
                        Statement st = conn.createStatement();
                        String query = "SELECT product_line,mhe_name,mhe_no FROM mhe_number WHERE mhe_no >= '"
                           + fromMheNO
                           + "' AND mhe_no <= '"
                           + toMheNO
                           + "'";
                        ResultSet rs = st.executeQuery(query);
                        if (rs != null) {
                           String result = ConvertExcel.convertToExcel(rs);
                           TagWrite_Read.textArea.setText(result);
                        } else {
                           TagWrite_Read.textArea.setText("Enter available MHE number");
                        }

                        sql_connection.CloseConnection(conn);
                     } catch (SQLException e1) {
                        e1.printStackTrace();
                        TagWrite_Read.textArea.setText(e1.getMessage());
                     } catch (IOException e1) {
                        e1.printStackTrace();
                        TagWrite_Read.textArea.setText(e1.getMessage());
                     }
                  } else {
                     TagWrite_Read.textArea.setText("Select Product Line, MHE Name, Tag prefix and Enter numbers");
                  }
               }
            }
         );
      this.btnPrintMheNo.setEnabled(false);
      this.btnPrintMheNo.setBounds(415, 206, 122, 26);
      panel.add(this.btnPrintMheNo);
   }
}

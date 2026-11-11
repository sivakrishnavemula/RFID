package com.rfid;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

public class LoginPage {
   private JFrame frmRfidReadWrite;
   private final JLabel lblApollo = new JLabel("");
   static String username;
   private JTextField textField;
   static JTextArea textArea;
   private JPasswordField textField_1;
   static String path;

   public static void main(String[] args) {
      if (GraphicsEnvironment.isHeadless()) {
         System.out.println("RFID application cannot start in a headless environment. Run it on a desktop session.");
         return;
      }

      EventQueue.invokeLater(new Runnable() {
         public void run() {
            try {
               LoginPage.path = System.getProperty("user.dir");
               CodeSource codeSource = LoginPage.class.getProtectionDomain().getCodeSource();
               if (codeSource != null && codeSource.getLocation() != null) {
                  File jarFile = new File(codeSource.getLocation().toURI().getPath());
                  if (jarFile.getParentFile() != null) {
                     LoginPage.path = jarFile.getParentFile().getPath();
                  }
               }
               System.out.println(LoginPage.path);

               LoginPage window = new LoginPage();
               window.frmRfidReadWrite.setVisible(true);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });
   }

   public LoginPage() {
      this.initialize();
   }

   private void initialize() {
      this.frmRfidReadWrite = new JFrame();
      this.frmRfidReadWrite.setTitle("RFID Read Write Application");
      this.frmRfidReadWrite.getContentPane().setForeground(new Color(255, 102, 255));
      this.frmRfidReadWrite.getContentPane().setBackground(Color.WHITE);
      this.frmRfidReadWrite.setBackground(Color.ORANGE);
      this.frmRfidReadWrite.setForeground(Color.WHITE);
      this.frmRfidReadWrite.setFont(new Font("Arial Black", 1, 14));
      this.frmRfidReadWrite.setBounds(100, 100, 1009, 561);
      this.frmRfidReadWrite.setDefaultCloseOperation(3);
      this.frmRfidReadWrite.getContentPane().setLayout(null);
      JPanel panel = new JPanel();
      panel.setBorder(new LineBorder(new Color(0, 0, 0)));
      panel.setBackground(Color.WHITE);
      panel.setBounds(0, 0, 517, 522);
      this.frmRfidReadWrite.getContentPane().add(panel);
      panel.setLayout(null);
      Image imageUrl = getImage("apollologo.png");
      ImageIcon IM = new ImageIcon(imageUrl);
      JLabel label = new JLabel(IM);
      label.setBackground(Color.WHITE);
      label.setBounds(95, 93, 358, 384);
      panel.add(label);
      JPanel panel_1 = new JPanel();
      panel_1.setForeground(new Color(255, 255, 255));
      panel_1.setBorder(new LineBorder(new Color(0, 0, 0)));
      panel_1.setBackground(new Color(211, 211, 211));
      panel_1.setBounds(515, 0, 478, 522);
      this.frmRfidReadWrite.getContentPane().add(panel_1);
      panel_1.setLayout(null);
      JLabel label_1 = new JLabel("User Name");
      label_1.setHorizontalAlignment(2);
      label_1.setForeground(new Color(75, 0, 130));
      label_1.setFont(new Font("Arial Black", 1, 23));
      label_1.setBackground(Color.WHITE);
      label_1.setBounds(81, 202, 186, 48);
      panel_1.add(label_1);
      this.textField = new JTextField();
      this.textField.setForeground(Color.BLACK);
      this.textField.setFont(new Font("Tahoma", 1, 18));
      this.textField.setColumns(10);
      this.textField.setBackground(new Color(245, 245, 245));
      this.textField.setBounds(244, 209, 173, 39);
      panel_1.add(this.textField);
      JLabel label_2 = new JLabel("Password");
      label_2.setHorizontalAlignment(2);
      label_2.setForeground(new Color(75, 0, 130));
      label_2.setFont(new Font("Arial Black", 1, 23));
      label_2.setBounds(81, 273, 186, 48);
      panel_1.add(label_2);
      JButton button = new JButton("Login");
      button.addActionListener(
         new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               LoginPage.username = LoginPage.this.textField.getText();
               char[] ch = LoginPage.this.textField_1.getPassword();
               String password = new String(ch);
               int user_count = 0;

               try {
                  Connection conn = sql_connection.CreateConnection();
                  if (conn == null) {
                     LoginPage.textArea.setForeground(Color.RED);
                     LoginPage.textArea.setText("Database not configured");
                     return;
                  }

                  Statement st = conn.createStatement();
                  ResultSet rs = st.executeQuery("SELECT COUNT(username) FROM user_details WHERE username='" + LoginPage.username + "'");

                  while (rs.next()) {
                     user_count = Integer.parseInt(rs.getString(1));
                  }

                  if (user_count != 1) {
                     LoginPage.textArea.setForeground(Color.RED);
                     LoginPage.textArea.setText("Invalid Username");
                  } else {
                     String loginUsername = null;
                     String loginPassword = null;

                     for (ResultSet rs1 = st.executeQuery("SELECT username,password FROM user_details WHERE username='" + LoginPage.username + "'");
                        rs1.next();
                        loginPassword = rs1.getString("password")
                     ) {
                        loginUsername = rs1.getString("username");
                     }

                     if (loginUsername != null && loginUsername.equals(LoginPage.username) && loginPassword != null && loginPassword.equals(password)) {
                        LoginPage.textArea.setForeground(Color.GREEN);
                        LoginPage.textArea.setText("Success");
                        TagWrite_Read.main(null);
                        LoginPage.this.frmRfidReadWrite.dispose();
                     } else {
                        LoginPage.textArea.setForeground(Color.RED);
                        LoginPage.textArea.setText("Invalid Password");
                     }
                  }

                  sql_connection.CloseConnection(conn);
               } catch (SQLException e1) {
                  e1.printStackTrace();
               }
            }
         }
      );
      button.setForeground(new Color(105, 105, 105));
      button.setFont(new Font("Tahoma", 1, 18));
      button.setBackground(new Color(255, 127, 80));
      button.setBounds(175, 370, 133, 39);
      panel_1.add(button);
      textArea = new JTextArea();
      textArea.setForeground(Color.BLACK);
      textArea.setFont(new Font("Monospaced", 1, 17));
      textArea.setBackground(new Color(211, 211, 211));
      textArea.setBounds(244, 435, 198, 33);
      panel_1.add(textArea);
      JTextArea txtrRfidReadWrite = new JTextArea();
      txtrRfidReadWrite.setEditable(false);
      txtrRfidReadWrite.setWrapStyleWord(true);
      txtrRfidReadWrite.setText("\r\n RFID Read Write Application");
      txtrRfidReadWrite.setForeground(new Color(255, 0, 0));
      txtrRfidReadWrite.setFont(new Font("Arial Black", 1, 26));
      txtrRfidReadWrite.setBackground(new Color(211, 211, 211));
      txtrRfidReadWrite.setBounds(10, 33, 446, 139);
      panel_1.add(txtrRfidReadWrite);
      this.textField_1 = new JPasswordField();
      this.textField_1.setBounds(244, 283, 173, 39);
      panel_1.add(this.textField_1);
   }

   public static Image getImage(String pathAndFileName) {
      URL url = Thread.currentThread().getContextClassLoader().getResource(pathAndFileName);
      if (url != null) {
         return Toolkit.getDefaultToolkit().getImage(url);
      }

      try {
         File imageFile = new File(LoginPage.path != null ? LoginPage.path : System.getProperty("user.dir"), pathAndFileName);
         if (imageFile.exists()) {
            return Toolkit.getDefaultToolkit().getImage(imageFile.toURI().toURL());
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      return new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
   }
}

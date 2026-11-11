package com.rfid;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

public class Tcp_ip_Socket {
   public static String[] TagWrite(String address, int port, String Tagname) {
      String[] args = new String[2];
      Socket socket = null;
      String ret = "Not Written";
      args[0] = ret;
      args[1] = new Date() + " Failure";
      String host = address;

      try {
         socket = new Socket(host, port);
         String writecmd = "sMN TAwriteTagData 0 1 2 6 32 +24 ";
         String cmd = writecmd + asciiToHex(Tagname);
         byte[] byteArr = cmd.getBytes("UTF-8");
         byte[] bytefin = new byte[byteArr.length + 2];
         byte[] byteSTX = new byte[]{2};
         byte[] byteETX = new byte[]{3};
         System.arraycopy(byteSTX, 0, bytefin, 0, 1);
         System.arraycopy(byteArr, 0, bytefin, 1, byteArr.length);
         System.arraycopy(byteETX, 0, bytefin, byteArr.length + 1, 1);
         OutputStream out = socket.getOutputStream();
         out.write(bytefin, 0, bytefin.length);
         InputStream in = socket.getInputStream();
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         byte[] buf = new byte[4096];
         int n = 0;

         while ((n = in.read(buf)) != -1) {
            baos.write(buf, 0, n);
            if (baos.toString().contains("TAwriteTagData") || baos.toString().contains("sFA")) {
               break;
            }
         }

         System.out.println("Written result from RFU " + baos.toString());
         out.close();
         in.close();
         socket.close();
         ret = baos.toString();
         args[0] = ret;
         if (n > 10) {
            if (baos.toString() == "") {
               return args;
            }

            String[] arrSplit = new String[]{null, null, null, null};
            arrSplit = baos.toString().split(" ");
            if (arrSplit[0].contains("FA") || baos.toString() == null) {
               return args;
            }

            if (arrSplit[2] != null && arrSplit[2].contains("1") && arrSplit[1] != null && arrSplit[1].contains("TAwriteTagData")) {
               args[1] = new Date() + " Success";
            }
         }
      } catch (UnknownHostException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }

      return args;
   }

   public static String[] TagRead(String address, int port) {
      Socket socket = null;
      String ret = "Not Read";
      String host = address;
      String[] args = new String[]{ret, "No Command", new Date() + " Failure"};

      try {
         socket = new Socket(host, port);
         String writecmd = "sMN TAreadTagData 0 1 2 5 32";
         String str = null;
         String cmd = writecmd;
         byte[] byteArr = cmd.getBytes("UTF-8");
         byte[] bytefin = new byte[byteArr.length + 2];
         byte[] byteSTX = new byte[]{2};
         byte[] byteETX = new byte[]{3};
         byte[] data = new byte[12];
         System.arraycopy(byteSTX, 0, bytefin, 0, 1);
         System.arraycopy(byteArr, 0, bytefin, 1, byteArr.length);
         System.arraycopy(byteETX, 0, bytefin, byteArr.length + 1, 1);
         OutputStream out = socket.getOutputStream();
         out.write(bytefin, 0, bytefin.length);
         InputStream in = socket.getInputStream();
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         byte[] buf = new byte[4096];
         int n = 0;

         while ((n = in.read(buf)) != -1) {
            baos.write(buf, 0, n);
            if (baos.toString().contains("TAreadTagData") || baos.toString().contains("sFA")) {
               break;
            }
         }

         System.out.println("Read Result from RFU " + baos.toString());
         out.close();
         in.close();
         socket.close();
         if (n > 10) {
            String[] arrSplit = new String[]{null, null, null, null};
            arrSplit = baos.toString().split(" ");
            if (arrSplit[2] != null && arrSplit[2].contains("1") && arrSplit[1] != null) {
               if (arrSplit[1].contains("TAreadTagData")) {
                  str = arrSplit[4].substring(0, arrSplit[4].length() - 3);
               }

               try {
                  data = hexToBytes(str);
                  ret = new String(data, "UTF-8");
                  args[0] = ret;
                  args[1] = baos.toString();
                  args[2] = new Date() + " Success";
               } catch (Exception e) {
                  e.printStackTrace();
               }
            }
         }
      } catch (UnknownHostException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }

      return args;
   }

   public static String[] TagAccess(String address, int port, String password) {
      String[] args = new String[2];
      Socket socket = null;
      String ret = "Not Written";
      args[0] = ret;
      args[1] = new Date() + " Failure";
      String host = address;

      try {
         socket = new Socket(host, port);
         String writecmd = "sMN TAusePasswd 1 " + password;
         String cmd = writecmd;
         byte[] byteArr = cmd.getBytes("UTF-8");
         byte[] bytefin = new byte[byteArr.length + 2];
         byte[] byteSTX = new byte[]{2};
         byte[] byteETX = new byte[]{3};
         System.arraycopy(byteSTX, 0, bytefin, 0, 1);
         System.arraycopy(byteArr, 0, bytefin, 1, byteArr.length);
         System.arraycopy(byteETX, 0, bytefin, byteArr.length + 1, 1);
         OutputStream out = socket.getOutputStream();
         out.write(bytefin, 0, bytefin.length);
         InputStream in = socket.getInputStream();
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         byte[] buf = new byte[4096];
         int n = 0;

         while ((n = in.read(buf)) != -1) {
            baos.write(buf, 0, n);
            if (baos.toString().contains("TAusePasswd") || baos.toString().contains("sFA")) {
               break;
            }
         }

         System.out.println("Tag access result from RFU" + baos.toString());
         out.close();
         in.close();
         socket.close();
         ret = baos.toString();
         args[0] = ret;
         if (n > 10) {
            if (baos.toString() == "") {
               return args;
            }

            String[] arrSplit = new String[]{null, null, null, null};
            arrSplit = baos.toString().split(" ");
            if (arrSplit[0].contains("FA") || baos.toString() == null) {
               return args;
            }

            if (arrSplit[2] != null && arrSplit[2].contains("1") && arrSplit[1] != null && arrSplit[1].contains("TAusePasswd")) {
               args[1] = new Date() + " Success";
            }
         }
      } catch (UnknownHostException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }

      return args;
   }

   public static void main(String[] args) throws UnknownHostException, IOException {
      TagRead("192.168.0.2", 2112);
   }

   private static String asciiToHex(String asciiValue) {
      char[] chars = asciiValue.toCharArray();
      StringBuffer hex = new StringBuffer();

      for (int i = 0; i < chars.length; i++) {
         hex.append(Integer.toHexString(chars[i]));
      }

      String hex1 = hex.toString();
      return hex1 + "000000";
   }

   private static byte[] hexToBytes(String value) {
      if (value == null || value.length() % 2 != 0) {
         return new byte[0];
      }

      byte[] result = new byte[value.length() / 2];
      for (int i = 0; i < result.length; i++) {
         String hex = value.substring(i * 2, i * 2 + 2);
         result[i] = (byte)Integer.parseInt(hex, 16);
      }
      return result;
   }
}

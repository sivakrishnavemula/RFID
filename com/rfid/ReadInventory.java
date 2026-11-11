package com.rfid;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

public class ReadInventory {
   public static String[] TagRead(String address, int port) {
      Socket socket = null;
      String ret = "Not Read";
      String host = address;
      String[] args = new String[]{ret, "RFID Tag not found", new Date() + " Failure"};

      try {
         socket = new Socket(host, port);
         String writecmd = "sMN IVSingleInv F";
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
         int n = 0;
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         OutputStream out = null;
         InputStream in = null;

         for (int i = 0; i < 1; i++) {
            in = null;
            out = null;
            in = socket.getInputStream();
            byte[] buf = new byte[4096];
            int loop = 0;

            while ((n = in.read(buf)) != -1) {
               baos.write(buf, 0, n);
               System.out.println("Loop read from RFU" + baos.toString());
               if (baos.toString().contains("TAGFOUND") || baos.toString().contains("TagNotFound")) {
                  String[] arrSplit1 = new String[5];
                  arrSplit1 = baos.toString().split(",");
                  System.out.println(arrSplit1);
                  break;
               }

               System.out.println("Read Result from RFU" + baos.toString());
            }

            in.close();
            socket.close();
            if (n > 10) {
               String[] arrSplit = new String[]{null, null, null, null, null, null};
               arrSplit = baos.toString().split(",");
               System.out.println("read data " + arrSplit[0]);
               System.out.println("read data " + arrSplit[1]);
               System.out.println("read data " + arrSplit[2]);
               if (arrSplit[1].contains("0")) {
                  return args;
               }

               if (arrSplit[1] != null) {
                  if (Integer.parseInt(arrSplit[1]) == 1) {
                     String firstFour = arrSplit[2].substring(0, 4);
                     String mh = "4D48";
                     System.out.println(firstFour);
                     if (firstFour.contains(mh)) {
                        args[0] = "Tag already has id starting MH";
                        args[1] = new Date() + " Failure";
                        return args;
                     }

                     str = arrSplit[2];
                     System.out.println(str);
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
            } else {
               args[0] = "More than 1 transponder found";
               args[1] = baos.toString();
               args[2] = new Date() + " Failure";
            }
         }
      } catch (UnknownHostException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }

      return args;
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

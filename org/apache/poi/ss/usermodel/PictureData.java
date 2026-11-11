package org.apache.poi.ss.usermodel;

public interface PictureData {
   byte[] getData();

   String suggestFileExtension();

   String getMimeType();

   int getPictureType();
}

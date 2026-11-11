package org.apache.poi.poifs.common;

public interface POIFSConstants {
   int SMALLER_BIG_BLOCK_SIZE = 512;
   POIFSBigBlockSize SMALLER_BIG_BLOCK_SIZE_DETAILS = new POIFSBigBlockSize(512, (short)9);
   int LARGER_BIG_BLOCK_SIZE = 4096;
   POIFSBigBlockSize LARGER_BIG_BLOCK_SIZE_DETAILS = new POIFSBigBlockSize(4096, (short)12);
   int SMALL_BLOCK_SIZE = 64;
   int PROPERTY_SIZE = 128;
   int BIG_BLOCK_MINIMUM_DOCUMENT_SIZE = 4096;
   int LARGEST_REGULAR_SECTOR_NUMBER = -5;
   int DIFAT_SECTOR_BLOCK = -4;
   int FAT_SECTOR_BLOCK = -3;
   int END_OF_CHAIN = -2;
   int UNUSED_BLOCK = -1;
   byte[] OOXML_FILE_HEADER = new byte[]{80, 75, 3, 4};
   byte[] RAW_XML_FILE_HEADER = new byte[]{60, 63, 120, 109, 108};
}

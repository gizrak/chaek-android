package com.gizrak.ebook.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

public class FileUtil {

	/**
	 * Resizes ByteArray Image
	 * 
	 * @param byteArrayImg
	 * @param width
	 * @param height
	 * @return
	 */
	public static byte[] resizingByteArrayImage(byte[] byteArrayImg, int width, int height) {
		if (byteArrayImg == null)
			return null;

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPurgeable = true;
		Bitmap oriBitmap = BitmapFactory.decodeByteArray(byteArrayImg, 0, byteArrayImg.length,
				options);
		if (oriBitmap == null)
			return null;

		Bitmap resizedBitmap = makeThumbImg(oriBitmap, width, height);
		byte[] retBytes = convertBitmapToByteArray(resizedBitmap);
		;

		if (resizedBitmap != null)
			resizedBitmap.recycle();

		return retBytes;
	}

	/**
	 * Makes Thumbnail Image
	 * 
	 * @param ori
	 * @param dstWidth
	 * @param dstHeight
	 * @return
	 */
	public static Bitmap makeThumbImg(Bitmap ori, int dstWidth, int dstHeight) {
		int srcWidth = ori.getWidth();
		int srcHeight = ori.getHeight();
		float scaleWidth = ((float) dstWidth) / srcWidth;
		float scaleHeight = ((float) dstHeight) / srcHeight;

		if (scaleWidth >= 1 || scaleHeight >= 1)
			return ori;

		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);

		Bitmap resizedBitmap = Bitmap.createBitmap(ori, 0, 0, srcWidth, srcHeight, matrix, true);

		return resizedBitmap;
	}

	/**
	 * Converts Bitmap to ByteArray
	 * 
	 * @param bitmap
	 * @return
	 */
	public static byte[] convertBitmapToByteArray(Bitmap bitmap) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		// ignored for PNG
		boolean result = bitmap.compress(CompressFormat.PNG, 0, bos);
		byte[] retBytes = null;

		if (result)
			retBytes = bos.toByteArray();

		if (bos != null) {
			try {
				bos.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		return retBytes;
	}

}

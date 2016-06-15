package com.example.detectrectangle;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.ExifInterface;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class Utility {

	/** Check if this device has a camera */
	public static boolean checkCameraHardware(Context context) {
		if (context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA)) 
		{
			return true;
		}
		else if(context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA_FRONT))
		{
			return true;
		}
		else {
			return false;
		}
	}

	// release camera
	public static void releaseCamera(Camera mCamera) 
	{
		if(mCamera != null)
		{
			mCamera.setPreviewCallback(null);
			mCamera.release();
			mCamera = null;
		}
	}

	/** A safe way to get an instance of the Camera object. */
	public static CameraIdModel getCameraInstance(Context mContext) {
		int cameraId = -1;
		
		int numberOfCamera = Camera.getNumberOfCameras();
		for (int i = 0; i < numberOfCamera; i++) 
		{
			CameraInfo mCameraInfo = new CameraInfo();
			Camera.getCameraInfo(i, mCameraInfo);
			if (mCameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
				cameraId = i;
				break;
			}
			else if(mCameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT)
			{
				cameraId = i;
			}
		}
		
		Camera c = null;
		try 
		{
			c = Camera.open(cameraId); // attempt to get a Camera instance
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}

		
		
		return new CameraIdModel(c, cameraId); // returns null if camera is unavailable
	}

	public static void setCameraDisplayOrientation(Activity activity,
			int cameraId, android.hardware.Camera camera) {
		android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(cameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay()
				.getRotation();

		int degrees = 0;

		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) 
		{
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360; // compensate the mirror
		} else { // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		
		camera.setDisplayOrientation(result);
	}
	
	
	public static Bitmap rotateImage(Bitmap source, float angle) 
	{
		Bitmap retVal;

		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		retVal = Bitmap.createBitmap(source, 0, 0, source.getWidth(),
				source.getHeight(), matrix, true);

		return retVal;
	}

	public static Bitmap fixImageOrientation(Bitmap bitmap, String imgUri) 
	{
		Bitmap finalBitmap = null;

		ExifInterface ei = null;
		try 
		{
			ei = new ExifInterface(imgUri);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
				ExifInterface.ORIENTATION_UNDEFINED);

		switch (orientation) {
		case ExifInterface.ORIENTATION_NORMAL:
			finalBitmap = Utility.rotateImage(bitmap, 90);
			break;
		case ExifInterface.ORIENTATION_ROTATE_90:
			finalBitmap = Utility.rotateImage(bitmap, 90);
			break;
		case ExifInterface.ORIENTATION_ROTATE_180:
			finalBitmap = Utility.rotateImage(bitmap, 180);
		case ExifInterface.ORIENTATION_ROTATE_270:
			finalBitmap = Utility.rotateImage(bitmap, 270);
			break;
		default:
			finalBitmap = bitmap;
			break;
		}

		return finalBitmap;
	}
	
	
	public static Bitmap getBitmapFromMat(Mat tempMat) 
	{
		Bitmap bmp = null;
		Mat tmp = new Mat(tempMat.rows(), tempMat.cols(), CvType.CV_8U,
				new Scalar(4));
		try 
		{
			// Imgproc.cvtColor(seedsImage, tmp, Imgproc.COLOR_RGB2BGRA);
			Imgproc.cvtColor(tempMat, tmp, Imgproc.COLOR_GRAY2RGBA, 4);
			bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(),
					Bitmap.Config.ARGB_8888);
			Utils.matToBitmap(tmp, bmp);

		} 
		catch (CvException e) 
		{
			Log.d("Exception", e.getMessage());
		}

		return bmp;
	}
	

	/** Create a File for saving an image or video */
	public static File getOutputMediaFile(Context mContext) 
	{
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		File mediaStorageDir = new File(
				Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				mContext.getString(R.string.app_name));
		
		// This location works best if you want the created images to be shared
		// between applications and persist after your app has been uninstalled.

		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists()) 
		{
			if (!mediaStorageDir.mkdirs()) 
			{
				Log.d("MyCameraApp", "failed to create directory");
				return null;
			}
		}

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
		File mediaFile;
		mediaFile = new File(mediaStorageDir.getPath() + File.separator
				+ "IMG_" + timeStamp + ".jpg");

		return mediaFile;
	}

	
	
	
	public static void showToast(Context mContext , String msg)
	{
		Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
	}

	public static RectF getRatioRectF(RectF mRectF , double widthRatio , double heightRatio)
	{
		RectF tRectF = new RectF((float)(mRectF.left*widthRatio), (float)(mRectF.top*heightRatio), (float)(mRectF.right*widthRatio), (float)(mRectF.bottom*heightRatio));
		return tRectF;
	}
	
}

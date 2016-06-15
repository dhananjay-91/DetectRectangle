package com.example.detectrectangle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

@SuppressWarnings("deprecation")
public class CameraSurfaceView extends ViewGroup implements
		SurfaceHolder.Callback, Camera.PreviewCallback {

	private Size mPreviewSize;
	private List<Size> mSupportedPreviewSizes;
	private Context mContext;
	private SurfaceView mSurfaceView;
	private SurfaceHolder mHolder;
	private final static String TAG = "CameraSurfaceView";
	private Camera mCamera;
	private int imageFormat;

	private ImageView MyCameraPreview = null;
	private byte[] FrameData = null;
	private boolean bProcessing = false;

	// values
	private Handler mHandler = new Handler(Looper.getMainLooper());

	private Camera.Size mDefaultSize;

	
	public interface onPicTakenListener {
		public void onPicReceived(Bitmap bitmap);
	}

	public CameraSurfaceView(Activity context, Camera camera,
			ImageView CameraPreview) {
		super(context);
		mContext = context;
		this.mCamera = camera;
		setCamera(mCamera);
		MyCameraPreview = CameraPreview;

		mSurfaceView = new SurfaceView(context);
		addView(mSurfaceView, 0);
		mHolder = mSurfaceView.getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mHolder.setKeepScreenOn(true);

		mDefaultSize = mCamera.new Size(640, 480);
	}

	public CameraSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}

	public void setSupportedPreviewSizes(List<Size> supportedPreviewSizes) {
		mSupportedPreviewSizes = supportedPreviewSizes;
	}

	public Size getPreviewSize() {
		return mPreviewSize;
	}

	public void setCamera(Camera camera) {
		mCamera = camera;
		if (mCamera != null) {
			mSupportedPreviewSizes = mCamera.getParameters()
					.getSupportedPreviewSizes();
			setCamFocusMode(mCamera.getParameters());
		}
		requestLayout();

	}

	private Runnable DoImageProcessing = new Runnable() {
		public void run() {
			// Log.i("CameraPreview", "DoImageProcessing():");
			bProcessing = true;

			Bitmap bitmap = convertFrameDataToBitmapWithDetection(false);

			if (bitmap != null) {
				MyCameraPreview.setImageBitmap(bitmap);
			}

			bProcessing = false;
		}
	};

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// surface destroyed
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// Now that the size is known, set up the camera parameters and begin
		// the preview.

		if (mCamera != null) {
			// cameraSetup(mDefaultSize.width, mDefaultSize.height);
			Camera.Parameters parameters = mCamera.getParameters();
			Size previewSize = getPreviewSize();
			parameters.setPreviewSize(previewSize.width, previewSize.height);

			imageFormat = parameters.getPreviewFormat();

			if (parameters != null) {
				setFocusMode(parameters, Camera.Parameters.FLASH_MODE_AUTO);
			}

			mCamera.setParameters(parameters);
			try {
				mCamera.setPreviewDisplay(holder);
			} catch (IOException e) {
				e.printStackTrace();
			}
			mCamera.setPreviewCallback(this);
			mCamera.startPreview();
		}
	}


	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, acquire the camera and tell it where
		// to draw.
		try {
			if (mCamera != null) {
				mCamera.setPreviewDisplay(holder);
				setCamFocusMode(mCamera.getParameters());
			}
		} catch (IOException exception) {
			Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
		}

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int width = resolveSize(getSuggestedMinimumWidth(),
				widthMeasureSpec);
		final int height = resolveSize(getSuggestedMinimumHeight(),
				heightMeasureSpec);
		setMeasuredDimension(width, height);

		if (mSupportedPreviewSizes != null) {
			// mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes,
			// width, height);
			mPreviewSize = mDefaultSize;

			// set Preview size to application size
			((ApplicationClass) mContext.getApplicationContext())
					.setPreviewSize(mPreviewSize);
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		if (changed) {
			final View cameraView = getChildAt(0);

			final int width = right - left;
			final int height = bottom - top;

			int previewWidth = width;
			int previewHeight = height;
			if (mPreviewSize != null) {
				Display display = ((WindowManager) mContext
						.getSystemService(Context.WINDOW_SERVICE))
						.getDefaultDisplay();

				switch (display.getRotation()) {
				case Surface.ROTATION_0:
					previewWidth = mPreviewSize.height;
					previewHeight = mPreviewSize.width;
					mCamera.setDisplayOrientation(90);
					break;
				case Surface.ROTATION_90:
					previewWidth = mPreviewSize.width;
					previewHeight = mPreviewSize.height;
					break;
				case Surface.ROTATION_180:
					previewWidth = mPreviewSize.height;
					previewHeight = mPreviewSize.width;
					break;
				case Surface.ROTATION_270:
					previewWidth = mPreviewSize.width;
					previewHeight = mPreviewSize.height;
					mCamera.setDisplayOrientation(180);
					break;
				}
			}

			final int scaledChildHeight = previewHeight * width / previewWidth;
			cameraView.layout(0, height - scaledChildHeight, width, height);
		}
	}


	public void previewCamera() {
		try {
			mCamera.setPreviewDisplay(mHolder);
			mCamera.startPreview();
		} catch (Exception e) {
			Log.d(TAG, "Cannot start preview.", e);
		}
	}

	public void setFocusMode(Camera.Parameters cameraParams, String mode) {
		if (cameraParams == null) {
			return;
		}

		List<String> supported = cameraParams.getSupportedFocusModes();
		if (supported.contains(mode)) {
			cameraParams.setFocusMode(mode);
		}
	}

	private void setCamFocusMode(Camera.Parameters mParameters) {
		if (null == mCamera) {
			return;
		}

		/* Set Auto focus */
		List<String> focusModes = mParameters.getSupportedFocusModes();

		
		 if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
			mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		}

		mCamera.setParameters(mParameters);
	}

	public void removeSurfaceViewCallback() {
		mSurfaceView.getHolder().removeCallback(this);
	}

	private Bitmap convertFrameDataToBitmapWithDetection(
			boolean returnHeightAndWidth) {
		// Copy preview data to a new Mat element
		/*
		 * Mat mYuv = new Mat(frameHeight + frameHeight / 2, frameWidth,
		 * CvType.CV_8UC1); mYuv.put(0, 0, data);
		 */
		// height should be 1.5 of original height
		Mat mYuv = new Mat(mPreviewSize.height + mPreviewSize.height / 2,
				mPreviewSize.width, CvType.CV_8UC1);
		mYuv.put(0, 0, FrameData);

		// Convert preview frame to rgba color space
		final Mat mRgba = new Mat();
		Imgproc.cvtColor(mYuv, mRgba, Imgproc.COLOR_YUV2BGR_NV12, 4);

		// Converts the Mat to a bitmap.
		Bitmap bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(),
				Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(mRgba, bitmap);

		Bitmap rotatedBitmap = null;
		if (bitmap != null) {
			Matrix matrix = new Matrix();
			matrix.postRotate(90);
			rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
					bitmap.getWidth(), bitmap.getHeight(), matrix, true);
		}

		Bitmap result = null;
		try {
			result = findRectangle(rotatedBitmap);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (result != null) {
			return result;
		}

		return rotatedBitmap;
	}


	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		if (imageFormat == ImageFormat.NV21) {
			// We only accept the NV21(YUV420) format.
			if (!bProcessing) {
				FrameData = data;
				mHandler.post(DoImageProcessing);
			}
		}
	}

	public void takePicture(final onPicTakenListener onPicTakenListener) {
		if (onPicTakenListener == null) {
			return;
		}

		Bitmap result = convertFrameDataToBitmapWithDetection(true);

		// if (result != null && isCoinDetected && detectedCoinDiameter != -1)
		if (result != null) {
			// onPicTakenListener.onPicReceived(result, mDetectedWidth,
			// mDetectedHeight);
			onPicTakenListener.onPicReceived(result);
		} else {
			onPicTakenListener.onPicReceived(null);
		}
	}


	private static double angle(Point p1, Point p2, Point p0) {
		double dx1 = p1.x - p0.x;
		double dy1 = p1.y - p0.y;
		double dx2 = p2.x - p0.x;
		double dy2 = p2.y - p0.y;
		return (dx1 * dx2 + dy1 * dy2)
				/ Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2)
						+ 1e-10);
	}

	private static Bitmap findRectangle(Bitmap image) throws Exception {
		Mat tempor = new Mat();
		Mat src = new Mat();
		Utils.bitmapToMat(image, tempor);

		Imgproc.cvtColor(tempor, src, Imgproc.COLOR_BGR2RGB);

		Mat blurred = src.clone();
		Imgproc.medianBlur(src, blurred, 9);

		Mat gray0 = new Mat(blurred.size(), CvType.CV_8U), gray = new Mat();

		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

		List<Mat> blurredChannel = new ArrayList<Mat>();
		blurredChannel.add(blurred);
		List<Mat> gray0Channel = new ArrayList<Mat>();
		gray0Channel.add(gray0);

		MatOfPoint2f approxCurve;

		double maxArea = 0;
		int maxId = -1;

		for (int c = 0; c < 3; c++) {
			int ch[] = { c, 0 };
			Core.mixChannels(blurredChannel, gray0Channel, new MatOfInt(ch));

			int thresholdLevel = 1;
			for (int t = 0; t < thresholdLevel; t++) {
				if (t == 0) {
					Imgproc.Canny(gray0, gray, 10, 20, 3, true); // true ?
					Imgproc.dilate(gray, gray, new Mat(), new Point(-1, -1), 1); // 1
																					// ?
				} else {
					Imgproc.adaptiveThreshold(gray0, gray, thresholdLevel,
							Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
							Imgproc.THRESH_BINARY,
							(src.width() + src.height()) / 200, t);
				}

				Imgproc.findContours(gray, contours, new Mat(),
						Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

				for (MatOfPoint contour : contours) {
					MatOfPoint2f temp = new MatOfPoint2f(contour.toArray());

					double area = Imgproc.contourArea(contour);
					approxCurve = new MatOfPoint2f();
					Imgproc.approxPolyDP(temp, approxCurve,
							Imgproc.arcLength(temp, true) * 0.02, true);

					if (approxCurve.total() == 4 && area >= maxArea) {
						double maxCosine = 0;

						List<Point> curves = approxCurve.toList();
						for (int j = 2; j < 5; j++) {

							double cosine = Math.abs(angle(curves.get(j % 4),
									curves.get(j - 2), curves.get(j - 1)));
							maxCosine = Math.max(maxCosine, cosine);
						}

						if (maxCosine < 0.3) {
							maxArea = area;
							maxId = contours.indexOf(contour);
						}
					}
				}
			}
		}

		if (maxId >= 0) {
			Rect rect = Imgproc.boundingRect(contours.get(maxId));

			Imgproc.rectangle(src, rect.tl(), rect.br(), new Scalar(255, 0, 0,
					.8), 4);

			
			int mDetectedWidth = rect.width;
			int mDetectedHeight = rect.height;
			
			Log.d(TAG, "Rectangle width :"+mDetectedWidth+ " Rectangle height :"+mDetectedHeight);
 
		}

		Bitmap bmp;
		bmp = Bitmap.createBitmap(src.cols(), src.rows(),
				Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(src, bmp);


		return bmp;

	}
}
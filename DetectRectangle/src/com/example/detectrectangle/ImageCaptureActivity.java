package com.example.detectrectangle;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.detectrectangle.CameraSurfaceView.onPicTakenListener;

@SuppressWarnings("deprecation")
public class ImageCaptureActivity extends Activity 
{
	private static final String TAG = "ImageCaptureActivity";
	private boolean isDeviceHasCamera;
	private Camera mCamera;
	// private CameraPreview mPreview;
	private CameraSurfaceView mCameraPreview;
	private FrameLayout mFrameLayout;


	private String mFilePath;

	private ProgressDialog mDialog;

	private ImageView mImageView;

	private BaseLoaderCallback mLoaderCallabck = new BaseLoaderCallback(this)
	{
		public void onManagerConnected(int status)
		{
			switch (status)
			{
			case LoaderCallbackInterface.SUCCESS:

				if (mCameraPreview == null)
				{
					init();
					onClick();
					CameraIdModel mCameraIdModel = Utility.getCameraInstance(ImageCaptureActivity.this);
					mCamera = mCameraIdModel.getmCamera();
					int cameraId = mCameraIdModel.getId();
					Utility.setCameraDisplayOrientation(ImageCaptureActivity.this, cameraId, mCamera);

					mImageView = new ImageView(ImageCaptureActivity.this);
					mImageView.setScaleType(ImageView.ScaleType.FIT_XY);

					mCameraPreview = new CameraSurfaceView(ImageCaptureActivity.this, mCamera, mImageView);

					mFrameLayout.addView(mCameraPreview);
					mFrameLayout.addView(mImageView);

				}
				break;

			default:
				break;
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.priview_layout);

		// checking does device has camera
		isDeviceHasCamera = Utility.checkCameraHardware(ImageCaptureActivity.this);

		// if there is no camera return it
		if (!isDeviceHasCamera)
		{
			finish();
			Toast.makeText(ImageCaptureActivity.this, "There is no Camera!!", Toast.LENGTH_LONG).show();
			return;
		}

	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		if (!OpenCVLoader.initDebug())
		{
			Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallabck);
		}
		else
		{
			Log.d("OpenCV", "OpenCV library found inside package. Using it!");
			mLoaderCallabck.onManagerConnected(LoaderCallbackInterface.SUCCESS);
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		if (mCameraPreview != null)
		{
			mCameraPreview.removeSurfaceViewCallback();
			mCameraPreview = null;
		}
		Utility.releaseCamera(mCamera);

	}


	private void takePicture()
	{
		mCameraPreview.takePicture(new onPicTakenListener()
		{
			@Override
			public void onPicReceived(Bitmap bitmap)
			{
				if (bitmap != null)
				{
					Log.e(TAG, "Bitmap Widht :" + bitmap.getWidth() + " Bitmap Height :" + bitmap.getHeight());

					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
					byte[] byteArray = stream.toByteArray();

					dismissCurrentDialog();

					File pictureFile = Utility.getOutputMediaFile(ImageCaptureActivity.this);
					if (pictureFile == null)
					{
						Log.d(TAG, "Error creating media file, check storage permissions: ");
						return;
					}

					mFilePath = pictureFile.getAbsolutePath();
					try
					{
						FileOutputStream fos = new FileOutputStream(pictureFile);
						fos.write(byteArray);
						fos.close();

						Intent mIntent = new Intent(ImageCaptureActivity.this, ImageDisplayActivity.class);
						mIntent.putExtra("image_path", mFilePath);
						startActivity(mIntent);
					}
					catch (FileNotFoundException e)
					{
						Log.d(TAG, "File not found: " + e.getMessage());
					}
					catch (IOException e)
					{
						Log.d(TAG, "Error accessing file: " + e.getMessage());
					}
				}
				else
				{
					dismissCurrentDialog();
					Toast.makeText(ImageCaptureActivity.this, "Please try again!!",Toast.LENGTH_LONG).show();
				}
			}
		});

	}

	protected void init()
	{
		mFrameLayout = (FrameLayout) findViewById(R.id.camera_preview);
	}

	protected void onClick()
	{
		mFrameLayout.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				
				if ((mCamera != null) && (mCameraPreview != null))
					{
						createCurrentDialog();
						mCamera.autoFocus(new AutoFocusCallback()
						{
							@Override
							public void onAutoFocus(boolean success, Camera camera)
							{
								takePicture();
							}
						});
					}
			}
		});
	}


	private void dismissCurrentDialog()
	{
		if (mDialog != null && mDialog.isShowing())
			mDialog.dismiss();
	}

	private void createCurrentDialog()
	{
		mDialog = new ProgressDialog(ImageCaptureActivity.this);
		mDialog.setCancelable(false);
		mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
		mDialog.show();
		mDialog.setContentView(R.layout.dialog_layout);
	}
}
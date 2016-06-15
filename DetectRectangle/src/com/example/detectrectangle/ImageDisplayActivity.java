package com.example.detectrectangle;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Rect;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

@SuppressWarnings("deprecation")
@SuppressLint("NewApi")
public class ImageDisplayActivity extends Activity {
	private static final String TAG = "ImageDisplayActivity";

	private ImageView imgView;

	private String mFilePath;
	private Bitmap mImgBitmap;

	public static Rect mRect;

	private BaseLoaderCallback mCallback = new BaseLoaderCallback(this) {
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:

				Log.d(TAG, "OpenCV loaded succeffully!!");

				setContentView(R.layout.image_display);

				init();

				mFilePath = getIntent().getStringExtra("image_path");
				mImgBitmap = getPic(mFilePath);

				if (mImgBitmap != null) {
					imgView.setImageBitmap(mImgBitmap);
				}
			default:
				break;
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		try {
			init();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	/* Called whenever we call invalidateOptionsMenu() */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		return super.onOptionsItemSelected(item);
	}

	private void init() {
		imgView = (ImageView) findViewById(R.id.imgView);

	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	private Bitmap getPic(String filePath) {

		/* Get the size of the image */
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		bmOptions.inPreferredConfig = Config.RGB_565;
		bmOptions.inDither = true;

		BitmapFactory.decodeFile(filePath, bmOptions);

		/* Figure out which way needs to be reduced less */
		int scaleFactor = 1;

		/* Set bitmap options to scale the image decode target */
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap */
		Bitmap rawBitmap = BitmapFactory.decodeFile(filePath, bmOptions);

		Bitmap tempBitmap = Utility.fixImageOrientation(rawBitmap, filePath);

		Bitmap bitmap = getResizedBitmap(tempBitmap, 300);

		return bitmap;
	}

	public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
		int width = image.getWidth();
		int height = image.getHeight();

		float bitmapRatio = (float) width / (float) height;
		if (bitmapRatio > 1) {
			width = maxSize;
			height = (int) (width / bitmapRatio);
		} else {
			height = maxSize;
			width = (int) (height * bitmapRatio);
		}
		return Bitmap.createScaledBitmap(image, width, height, true);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!OpenCVLoader.initDebug()) {
			Log.d(TAG,
					"Internal OpenCV library not found. Using OpenCV Manager for initialization");
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this,
					mCallback);
		} else {
			Log.d(TAG, "OpenCV library found inside package. Using it!");
			mCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
		}
	}

}

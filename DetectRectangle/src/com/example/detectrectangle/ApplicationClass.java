package com.example.detectrectangle;

import android.app.Application;
import android.hardware.Camera;

@SuppressWarnings("deprecation")
public class ApplicationClass extends Application 
{
	private Camera.Size mPreviewSize;
	
	public Camera.Size getPreviewSize()
	{
		return mPreviewSize;
	}

	public void setPreviewSize(Camera.Size mSize)
	{
		this.mPreviewSize = mSize;
	}
	
}

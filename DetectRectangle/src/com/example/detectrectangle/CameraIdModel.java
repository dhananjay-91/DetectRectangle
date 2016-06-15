package com.example.detectrectangle;

import android.hardware.Camera;

@SuppressWarnings("deprecation")
public class CameraIdModel
{
	private Camera mCamera;
	private int id;
	
	public CameraIdModel(Camera mCamera , int id)
	{
		this.mCamera = mCamera;
		this.id = id;
	}
	
	public Camera getmCamera()
	{
		return mCamera;
	}
	public void setmCamera(Camera mCamera)
	{
		this.mCamera = mCamera;
	}
	public int getId()
	{
		return id;
	}
	public void setId(int id)
	{
		this.id = id;
	}
	
	
	
}

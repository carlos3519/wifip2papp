package com.example.wifip2papp;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class FilePathProvider {
	public static final String TAG = "FilePathProvider";
	public static final int MEDIA_TYPE_VIDEO = 0;
	public static final int MEDIA_TYPE_IMAGE = 1;
	
	File mediaStorageDir = null;
	
	public FilePathProvider(String appName){
		mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), appName);
		if(!mediaStorageDir.exists()){
			if (! mediaStorageDir.mkdirs()){
	            Log.d(TAG, "failed to create directory: "+mediaStorageDir);
	        }
		}
	}

	public String getOutputMediaFilePath(int type){
	    // Create a media file name
		
		String filePath = null;
		
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    
	    if (type == MEDIA_TYPE_IMAGE){
	        filePath = mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp + ".jpg";
	    } else if(type == MEDIA_TYPE_VIDEO) {
	    	filePath = mediaStorageDir.getPath() + File.separator +"VID_"+ timeStamp + ".mp4";
	    } else {
	        return null;
	    }
	    
	    File mediaFile = new File(filePath);
	    
	    if(!mediaFile.exists()){
	    	try{
	    		mediaFile.createNewFile();
	    	}catch(Exception ex){
	    		Log.d(TAG, "problem creating file:"+mediaFile+": "+ex.getMessage());
	    	}
	    }

	    return filePath;
	}
	
}

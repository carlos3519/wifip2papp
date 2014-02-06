package com.example.wifip2papp;


import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;


import android.graphics.Color;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;

public class WifiP2pActivity extends Activity {

	public static final String TAG = "WifiP2pActivity";
	public static final int PORT = 5678;
	
	private IntentFilter intentFilter = null;
	private WifiP2pManager p2pManager = null;
	private Channel channel = null;
	private BroadcastReceiver receiver = null;
	private CameraPreview preview = null;
	private Camera camera = null;
	private MediaRecorder recorder = null;
	private MediaPlayer player = null;
	private boolean recording = false;
	private Handler handler = null;
	private FilePathProvider fpp = null;
	
	public static final String LOCALSOCKETADDRESS = "WifiP2pActivity";
	private ServerSocket serverSocket = null;
	private Socket socket = null;
	private LocalServerSocket localServerSocket = null;
	private LocalSocket localSocket = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wifi_p2p);
		handler = new Handler(){
			
			@Override
			public void handleMessage(Message msg) {
				EditText output = (EditText) findViewById(R.id.outputEditText);
				output.append(msg.getData().getString("msg") + "\r\n");
				super.handleMessage(msg);
			}
			
		};
		fpp = new FilePathProvider(TAG);
		//setup camera
		FrameLayout previewSpot = (FrameLayout) findViewById(R.id.frameLayout);
		camera = getCamera();
		preview = new CameraPreview(this, camera);
		previewSpot.addView(preview);
		launchServer();
		
		p2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		channel = p2pManager.initialize(this, getMainLooper(), null);
		receiver = new WiFiDirectBroadcastReceiver(p2pManager, channel, this);
		intentFilter = new IntentFilter();
	    intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
	    intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
	    intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
	    intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		
		Thread serverThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try{
					localServerSocket = new LocalServerSocket(LOCALSOCKETADDRESS);
					while(true){
						LocalSocket client = localServerSocket.accept();
						InputStream is = null;
						OutputStream os = null;
						if(client != null && socket != null){
							try{
								is = client.getInputStream();
								os = socket.getOutputStream();
								byte[] buff = new byte[1024*30];
								int bc = 0;
								while((bc=is.read(buff)) >= 0){
									os.write(buff, 0, bc);
									os.flush();
								}
							}catch(IOException ex){
								ex.printStackTrace();
							}finally{
								try{
									if(is != null){
										is.close();
									}
									if(os != null){
										os.close();
									}
									if(socket != null){
										socket.close();
									}
									if(localSocket != null){
										localSocket.close();
									}
								}catch(IOException ex2){
									ex2.printStackTrace();
								}
							}
							
						}
					}
				}catch(Exception ex){
					ex.printStackTrace();
				}
				
			}
		});
		serverThread.start();
	}

	public void launchServer(){
		Thread serverThread = new Thread( new Runnable() {
			@Override
			public void run() {
				postMessage("Server started");
				try{
					serverSocket = new ServerSocket(PORT);
					while(true){
						Socket clientSocket = serverSocket.accept();
						postMessage("Client connected");
						LocalSocket localSocket = new LocalSocket();
						localSocket.connect( new LocalSocketAddress(LOCALSOCKETADDRESS));
						//ParcelFileDescriptor pfd =  ParcelFileDescriptor.fromSocket(clientSocket);
						FileDescriptor fd = localSocket.getFileDescriptor();// pfd.getFileDescriptor();
						socket = clientSocket;
						if(fd.valid()){
							startRecording(fd);
						}
					}
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}
		});
		serverThread.start();
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.wifi_p2p, menu);
		return true;
	}
	
	@Override
	protected void onPause() {
		stopRecording();
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		stopRecording();
		super.onDestroy();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(receiver, intentFilter);
	}
	
	public void onRecordClicked(View view){
		if(!isRecording()){
			//start recording
			handler.post(new Runnable() {
				
				@Override
				public void run() {
					//startRecording();					
				}
			});
			//change button color
			handler.post(new Runnable() {
				
				@Override
				public void run() {
					changeButtonColor(Color.RED);										
				}
			});
		}else{
			handler.post(new Runnable() {
				
				@Override
				public void run() {
					stopRecording();
				}
			});
			handler.post(new Runnable() {
				
				@Override
				public void run() {
					changeButtonColor(Color.BLUE);
				}
			});
		}
	}
	
	private void changeButtonColor(int color){
		Button button = (Button) findViewById(R.id.recordButton);
		button.setBackgroundColor(color);
	}
	
	public Camera getCamera(){
		if(camera == null){
			try {
				camera = Camera.open(0);
			} catch (Exception ex) {
				Log.e(TAG, "Problem accessing camera: " + ex.getMessage());
			}
		}
		return camera;
	}
	
	public MediaRecorder getMediaRecorder(){
		if(recorder == null){
			recorder = new MediaRecorder();
		}
		return recorder;
	}
	
	public MediaPlayer getMediaPlayer(){
		if(player == null){
			player = new MediaPlayer();
		}
		return player;
	}
	
	public boolean prepareVideoRecorder(FileDescriptor fd){
		boolean prepared = false;
		
		camera = getCamera();
		recorder = getMediaRecorder();
		// Step 1: Unlock and set camera to MediaRecorder

			//camera.lock();		
			camera.unlock();

		recorder.setCamera(camera);
		
		// Step 2: Set sources
		//recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		
        //recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);                 
        //recorder.setVideoFrameRate(20);
        //recorder.setVideoSize(176,144);
        //recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
		
		// Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
		//recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));
		
		// Step 4: Set output file		
		recorder.setOutputFile(fd);
		recorder.setOutputFormat(8);
		recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		//recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		
		// Step 5: Set the preview output
		recorder.setPreviewDisplay(preview.getHolder().getSurface());
		
		// Step 6: Prepare configured MediaRecorder
		try{
			recorder.prepare();
			prepared = true;
		}catch(IllegalStateException ex){
			Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + ex.getMessage());
	        releaseMediaRecorder();
	        releaseCamera();
		}catch(IOException ex){
	        Log.d(TAG, "IOException preparing MediaRecorder: " + ex.getMessage());
	        ex.printStackTrace();
	        
	        releaseMediaRecorder();
	        releaseCamera();
		}
		
		return prepared;

	}
	

	
	private void releaseMediaRecorder(){
		if(recorder != null){
			recorder.reset();
			recorder.release();
			recorder = null;
			camera.lock();
		}
	}
	
	private void releaseCamera(){
		if(camera != null){
			camera.release();
			camera = null;
		}
	}
	
	
	public boolean isRecording(){
		return recording;
	}
	
	public void startRecording(FileDescriptor fd){
		if(prepareVideoRecorder(fd)){
				Log.d(TAG, "Ready to start recording");
				(new Thread(){
					@Override
					public void run() {
						recording = true;
						recorder.start();
						
					}
				}).start();
				Log.d(TAG, "Recording started");
		}
	}
	
	public void stopRecording(){
		//stop recording
		Log.d(TAG, "Stoping camera");
		(new Thread(){
			@Override
			public void run() {
				try{
					if(recorder != null){
						recorder.stop();
					}
				}catch(Throwable ex){
					ex.printStackTrace();
				}finally{
					recording = false;
					releaseMediaRecorder();
					releaseCamera();
				}
			}
		}).start();
		Log.d(TAG, "Camara stoped");
	}
	
	public void requestData(final InetAddress serverAddress){
		
		postMessage("Group owner found");
		
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try{
					postMessage("Connecting to group owner");
					String addr = serverAddress.getHostAddress();
					socket = new Socket(addr, PORT);
					postMessage("Connected to group owner");
					useData(socket.getInputStream(), ParcelFileDescriptor.fromSocket(socket).getFileDescriptor());
		
				}catch(Throwable ex){
					ex.printStackTrace();
					postMessage("Error connecting to group owner:"+ex.getMessage());
				}
			}
		});
		t.start();
	}
	
	
	public void useData(InputStream is, FileDescriptor fd){
		String filePath = fpp.getOutputMediaFilePath(FilePathProvider.MEDIA_TYPE_VIDEO);
		writeVideo(is, filePath);
		//startPlayback(filePath, fd);
	}
	
	public void writeVideo(final InputStream is, final String filePath){
		
		
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				OutputStream os = null;
				try{
					os=  new FileOutputStream(new File(filePath), true);
					byte[] buff = new byte[1024];
					int bc = 0;
					int t = 0;
					boolean s = false;
					while((bc = is.read(buff)) > 0){
						os.write(buff, 0, bc);
						os.flush();
						t += bc;
						if(t> 1024*10 && !s){
							postMessage(t+"bytes written to file buffer");
							startPlayback(filePath, null);
							s=true;
						}
					}
					
				}catch(Exception ex){
					ex.printStackTrace();
					postMessage("Error writing video to file");
				}finally{
					if(os != null){
						try{
						os.flush();
						os.close();
						}catch(Exception ex){
							ex.printStackTrace();
						}
					}
				}
			}
		});
		
		t.start();
	}
	
	public void startPlayback(final String  filePath, final FileDescriptor fd){
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(3000);
					postMessage("Starting playback");
					
					preview.stopPreview();
					
					player = getMediaPlayer();
					player.setAudioStreamType(AudioManager.STREAM_MUSIC);
					
					//player.setDataSource(getApplicationContext(), Uri.parse("/storage/emulated/0/DCIM/Camera/Video.mp4") );
					
					File file = new File(filePath);
					postMessage("exist:"+file.exists()+" can read:"+file.canRead());
					
					
					player.setDataSource(getApplicationContext(), Uri.parse(filePath) );
					//player.setDataSource(fd);
					player.prepare();
					
					player.setDisplay(preview.getHolder());
					//player.prepareAsync();
					player.start();
					postMessage("Playing....");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					postMessage("Problem performing playback:"+e.getMessage());
				}
			}
		});
		t.start();
	}
	
	public void postMessage(String msg){
        Bundle messageBundle = new Bundle();
        messageBundle.putString("msg", msg);
        Message message = new Message();
        message.setData(messageBundle);
        handler.sendMessage(message);
	}

}

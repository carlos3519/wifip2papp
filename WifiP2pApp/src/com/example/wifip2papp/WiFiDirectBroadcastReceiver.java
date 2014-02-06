package com.example.wifip2papp;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Hashtable;
import java.util.Map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.util.Log;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver implements WifiP2pManager.PeerListListener{

	public static final String TAG = WiFiDirectBroadcastReceiver.class.getName();
	private WifiP2pManager manager = null;
	private Channel channel = null;
	private WifiP2pActivity activity = null;
	private Map<String, String> connDevMap = null;
	private InetAddress goAddress = null;
	

	public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel, WifiP2pActivity activity) {
		super();
		this.manager = manager;
		this.channel = channel;
		this.activity = activity;
		connDevMap = new Hashtable<String, String>();
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();


		if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
	        int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
	        if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
	        	Log.d(TAG, "wifi p2p state enabled");
	        } else {
	        	Log.d(TAG, "wifi p2p state not enabled");
	        }
		} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
			activity.postMessage("Peers found");
			manager.requestPeers(channel, this);
			
		} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
			WifiP2pInfo p2pInfo = (WifiP2pInfo) intent.getExtras().get("wifiP2pInfo");
			NetworkInfo netInfo = (NetworkInfo) intent.getExtras().get("networkInfo");
			WifiP2pGroup p2pGroup = (WifiP2pGroup) intent.getExtras().get("p2pGroupInfo");
			Object connDevAddr = intent.getExtras().get("connectedDevIntfAddress");
			
			
			Log.d(TAG, "Connection Changed: "+bundleToString(intent.getExtras()));
	
			if(netInfo.isConnected() && !p2pInfo.isGroupOwner && p2pInfo.groupOwnerAddress != null && goAddress == null){
				goAddress = p2pInfo.groupOwnerAddress;
				activity.requestData(goAddress);
			}else if(!netInfo.isConnected() && goAddress != null){
				//goAddress = null;
			}
			
		} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
			// Respond to this device's wifi state changing
		}
	}
	
	@Override
	public void onPeersAvailable(WifiP2pDeviceList p2pDevList) {
		for(WifiP2pDevice dev : p2pDevList.getDeviceList()){
			if(!connDevMap.containsKey(dev.deviceAddress)){
				activity.postMessage("Connecting to device "+dev.deviceName);
				connectToDevice(dev);
			}
			
		}
	}
	
	
	private void connectToDevice(final WifiP2pDevice device) {

		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = device.deviceAddress;

		manager.connect(channel, config, new WifiP2pManager.ActionListener() {

			@Override
			public void onSuccess() {
				if (!connDevMap.containsKey(device.deviceAddress)) {
					connDevMap.put(device.deviceAddress, device.deviceName);
				}
				Log.d(TAG, "connected to device " + device.deviceName);
				activity.postMessage("Connected to device "+device.deviceName);

			}

			@Override
			public void onFailure(int arg0) {
				Log.d(TAG, "Could not connect to device " + device.deviceName);
				activity.postMessage("Failed connecting to device "+device.deviceName);
			}
		});

	}

	private String bundleToString(Bundle b){
		StringBuilder  sb = new StringBuilder();
		for(String key : b.keySet()){
			Object obj = b.get(key);
			Class<?> c = obj!=null ? b.getClass() : null;
			sb.append(" {key=["+key+"] , type=["+c+"], content=["+obj+"] } \n");
		}
		return sb.toString();
	}

}

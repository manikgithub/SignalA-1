package com.zsoft.SignalA.transport.longpolling;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import android.util.Log;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.util.Constants;
import com.zsoft.SignalA.ConnectionBase;
import com.zsoft.SignalA.ConnectionState;
import com.zsoft.SignalA.SignalAUtils;
import com.zsoft.SignalA.SendCallback;
import com.zsoft.SignalA.Transport.ProcessResult;
import com.zsoft.SignalA.Transport.TransportHelper;

public class ConnectedState extends StopableStateWithCallback {
	protected static final String TAG = "ConnectedState";
	private Object mCallbackLock = new Object();
	@SuppressWarnings("unused")
	private AjaxCallback<JSONObject> mCurrentCallback = null;
	private boolean mUseConnect = true;
	
	public ConnectedState(ConnectionBase connection) {
		super(connection);
	}

	@Override
	public ConnectionState getState() {
		return ConnectionState.Connected;
	}

	@Override
	public void Start() {
	}

	@Override
	public void Stop() {
		mConnection.SetNewState(new DisconnectingState(mConnection));
		super.Stop();
	}

	@Override
	public void Send(final CharSequence text, final SendCallback sendCb) {
		if(DoStop())
		{
			sendCb.OnError(new Exception("Connection is about to close"));
			return; 
		}

		AQuery aq = new AQuery(mConnection.getContext());
	    String url = SignalAUtils.EnsureEndsWith(mConnection.getUrl(), "/");
	    url +=  "send?transport=" + TRANSPORT_NAME;
		try {
			url += "&connectionToken=" + URLEncoder.encode(mConnection.getConnectionToken(), "utf-8");
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "Unsupported message encoding error, when encoding connectionToken.");
		}

	    AjaxCallback<String> cb = new AjaxCallback<String>() {
			@Override
			public void callback(String url, String result, AjaxStatus status) {
				if(status.getCode() == 200){
					Log.v(TAG, "Message sent: " + text);
					sendCb.OnSent(text);
				}
				else
				{
					Exception ex = new Exception("Error sending message");
					mConnection.setError(ex);
					sendCb.OnError(ex);
				}
			}
		};
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("data", text);
		
		cb.url(url).type(String.class).expire(-1).params(params).method(Constants.METHOD_POST);
		aq.ajax(cb);
	}

	@Override
	protected void OnRun() {
		//AQUtility.setDebug(true);
		//AjaxCallback.setTimeout(90000);
		AQuery aq = new AQuery(mConnection.getContext());
		
		if(DoStop()) return; 

	    String url = SignalAUtils.EnsureEndsWith(mConnection.getUrl(), "/");

	    //if (mConnection.getMessageId() == null)
		if (mUseConnect)
		{
			url += "connect";
			mUseConnect = false;
		}
	    else
	    {
			url += "reconnect";
	    }
	    
	    url += TransportHelper.GetReceiveQueryString(mConnection, null, TRANSPORT_NAME);

		Map<String, Object> params = new HashMap<String, Object>();
		      
		AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>()
		{
			@Override
			public void callback(String url, JSONObject json, AjaxStatus status) {
				if(DoStop()) return; 

                try
                {
                    if (json!=null)
                    {
                		ProcessResult result = TransportHelper.ProcessResponse(mConnection, json);

                		if(result.processingFailed)
                		{
                    		mConnection.setError(new Exception("Error while proccessing response."));
                    		mConnection.SetNewState(new ReconnectingState(mConnection));
                		}
                		else if(result.disconnected)
                		{
      						mConnection.SetNewState(new DisconnectedState(mConnection));
    						return;
                		}
                    }
                    else
                    {
					    mConnection.setError(new Exception("Error when calling endpoint. Returncode: " + status.getCode()));
						mConnection.SetNewState(new ReconnectingState(mConnection));
                    }
                }
                finally
                {
					mIsRunning.set(false);
					
					// Loop if we are still connected
					if(mConnection.getCurrentState() == ConnectedState.this)
						Run();
                }
			}
		};

		
		synchronized (mCallbackLock) {
			mCurrentCallback = cb;
		}
		//aq.ajax(url, JSONObject.class, cb);
		AjaxCallback.setReuseHttpClient(false);	// To fix wierd timeout issue
		cb.url(url).type(JSONObject.class).expire(-1).params(params).method(Constants.METHOD_POST).timeout(115000);
		aq.ajax(cb);
	}

}

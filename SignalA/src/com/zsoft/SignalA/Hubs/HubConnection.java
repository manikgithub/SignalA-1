package com.zsoft.SignalA.Hubs;

import android.content.Context;

import com.zsoft.SignalA.Transport.ITransport;
import com.zsoft.SignalA.Transport.StateBase;

public class HubConnection extends com.zsoft.SignalA.ConnectionBase {

	public HubConnection(String url, Context context, ITransport transport) {
		super(url, context, transport);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void OnError(Exception exception) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnMessage(String message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnStateChanged(StateBase oldState, StateBase newState) {
		// TODO Auto-generated method stub
		
	}

}

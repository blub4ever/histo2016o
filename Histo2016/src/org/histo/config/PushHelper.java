package org.histo.config;

import javax.faces.application.FacesMessage;

import org.primefaces.push.EventBus;
import org.primefaces.push.RemoteEndpoint;
import org.primefaces.push.annotation.OnClose;
import org.primefaces.push.annotation.OnMessage;
import org.primefaces.push.annotation.OnOpen;
import org.primefaces.push.annotation.PushEndpoint;
import org.primefaces.push.impl.JSONEncoder;

@PushEndpoint("/chat")
public class PushHelper {

	@OnOpen
	public void onOpen(RemoteEndpoint r, EventBus eventBus) {
	}

	@OnClose
	public void onClose(RemoteEndpoint r, EventBus eventBus) {
	}

	@OnMessage(encoders = { JSONEncoder.class })
	public FacesMessage onMessage(FacesMessage message) {
		return message;
	}
}

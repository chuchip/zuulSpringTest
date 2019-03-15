package com.profesorp.zuulSpringTest.Requests;

public class GatewayRequest {
	URIRequest uri;
	String body;
	public URIRequest getUri() {
		return uri;
	}
	public void setUri(URIRequest uri) {
		this.uri = uri;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	
}

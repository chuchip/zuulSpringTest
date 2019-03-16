package com.profesorp.zuulSpringTest.Requests;

import java.util.HashMap;

public class URIRequest {
	String url;
	String path;

	byte[] body=null;
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	
	public byte[] getBody() {
		return body;
	}
	public void setBody(byte[] body) {
		this.body = body;
	}
	
}

/**
 *
 * Copyright (c) 2015
 * All rights reserved.
 *
 * @Title RoutingModel.java
 * @Package com.probestar.dynamicrouting
 * @author ProbeStar
 * @Email probestar@qq.com
 * @QQ 344137375
 * @date Aug 27, 2015 3:55:05 PM
 * @version V1.0
 * @Description 
 *
 */

package com.probestar.dynamicrouting;

public class RoutingModel {
	private String _key;
	private String _url;

	public void setKey(String key) {
		_key = key;
	}

	public String getKey() {
		return _key;
	}

	public void setUrl(String url) {
		_url = url;
	}

	public String getUrl() {
		return _url;
	}

	@Override
	public String toString() {
		return String.format("Key: %s; Url: %s", getKey(), getUrl());
	}
}

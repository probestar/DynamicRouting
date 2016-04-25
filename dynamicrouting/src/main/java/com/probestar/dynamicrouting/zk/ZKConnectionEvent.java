/**
 *
 * Copyright (c) 2015
 * All rights reserved.
 *
 * @Title ZKConnectionEvent.java
 * @Package com.probestar.configuration.zk
 * @author ProbeStar
 * @Email probestar@qq.com
 * @QQ 344137375
 * @date Jul 29, 2015 3:35:51 PM
 * @version V1.0
 * @Description 
 *
 */

package com.probestar.dynamicrouting.zk;

public interface ZKConnectionEvent {

	void onConnected(boolean newSession);

	void onDisconnected();

	void onDataReset();

	void onDataChanged(String key, byte[] data);

	void onDataRemoved(String key);

}
/**
 *
 * Copyright (c) 2015
 * All rights reserved.
 *
 * @Title DynamicRouting.java
 * @Package com.probestar.dynamicrouting
 * @author ProbeStar
 * @Email probestar@qq.com
 * @QQ 344137375
 * @date Aug 27, 2015 3:49:26 PM
 * @version V1.0
 * @Description 
 *
 */

package com.probestar.dynamicrouting;

import java.nio.charset.Charset;

import org.apache.zookeeper.KeeperException;

import com.google.gson.Gson;
import com.probestar.dynamicrouting.zk.ZKConnection;
import com.probestar.dynamicrouting.zk.ZKConnectionEvent;
import com.probestar.psutils.PSConvert;
import com.probestar.psutils.PSTracer;

public class DynamicRouting implements ZKConnectionEvent {
	private static PSTracer _tracer = PSTracer.getInstance(DynamicRouting.class);
	private static DynamicRouting _instance;

	private ZKConnection _zk;
	private Gson _gson;

	public static void initialize(String conn, String userName, String password) {
		try {
			_instance = new DynamicRouting(conn, userName, password);
		} catch (Throwable t) {
			_tracer.error("DynamicRouting.static error.", t);
		}
	}

	public static DynamicRouting getInstance() {
		if (_instance == null)
			throw new NullPointerException("Please call DynamicRouting.initialize first.");
		return _instance;
	}

	private DynamicRouting(String conn, String userName, String password) {
		_zk = new ZKConnection(conn, userName, password);
		_zk.register(this);
		_gson = new Gson();
	}

	public void register(RoutingModel model) throws InterruptedException {
		_zk.createTemp(model.getKey(), model2Bytes(model));
	}

	public void deregister(String key) throws InterruptedException, KeeperException {
		_zk.delete(key);
	}

	private byte[] model2Bytes(RoutingModel model) {
		String json = _gson.toJson(model, RoutingModel.class);
		return json.getBytes(Charset.forName("utf-8"));
	}

	@Override
	public void onNodeChanged(byte[] data) {
		_tracer.info("Got data:" + PSConvert.bytes2HexString(data));
	}
}

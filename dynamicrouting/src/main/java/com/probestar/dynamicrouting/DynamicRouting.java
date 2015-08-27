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
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.Gson;
import com.probestar.dynamicrouting.zk.ZKConnection;
import com.probestar.dynamicrouting.zk.ZKConnectionEvent;
import com.probestar.psutils.PSConvert;
import com.probestar.psutils.PSTracer;

public class DynamicRouting implements ZKConnectionEvent {
	private static PSTracer _tracer = PSTracer.getInstance(DynamicRouting.class);
	private static DynamicRouting _instance;

	private boolean _isConnected;
	private Gson _gson;
	private ArrayList<RoutingModel> _myModels;
	private HashMap<String, RoutingModel> _models;
	private ZKConnection _zk;

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
		_isConnected = false;
		_gson = new Gson();
		_myModels = new ArrayList<RoutingModel>();
		_models = new HashMap<>();
		_zk = new ZKConnection(conn, userName, password);
		_zk.register(this);
	}

	public synchronized void register(RoutingModel model) throws InterruptedException {
		if (_isConnected)
			innerRegister(model);
		else
			_myModels.add(model);
	}

	public RoutingModel getRouting(String key) {
		return _models.get(key);
	}

	private void innerRegister(RoutingModel model) {
		try {
			_zk.createTempSeq(model.getKey(), model2Bytes(model));
			_tracer.info("RouteModel has been registered.\r\n" + model.toString());
		} catch (Throwable t) {
			_tracer.error("DynamicRouting.onConnected error.", t);
		}
	}

	private byte[] model2Bytes(RoutingModel model) {
		String json = _gson.toJson(model, RoutingModel.class);
		return json.getBytes(Charset.forName("utf-8"));
	}

	private RoutingModel bytes2Model(byte[] data) {
		String json = new String(data, Charset.forName("utf-8"));
		return _gson.fromJson(json, RoutingModel.class);
	}

	@Override
	public synchronized void onConnected() {
		_isConnected = true;

		for (RoutingModel model : _myModels)
			innerRegister(model);
		if (_myModels.size() == 0) {
			try {
				_zk.fireDataChanged();
			} catch (Throwable t) {
				_tracer.error("DynamicRouting.onConnected error.", t);
			}
		}
	}

	@Override
	public synchronized void onDisconnected() {
		_isConnected = false;
	}

	@Override
	public void onDataReset() {
		synchronized (_models) {
			_models.clear();
		}
	}

	@Override
	public void onDataChanged(byte[] data) {
		_tracer.debug("Got data: " + PSConvert.bytes2HexString(data));
		RoutingModel model = bytes2Model(data);
		_tracer.info("Got RoutingModel: " + model.toString());
		synchronized (_models) {
			_models.put(model.getKey(), model);
		}
	}

}

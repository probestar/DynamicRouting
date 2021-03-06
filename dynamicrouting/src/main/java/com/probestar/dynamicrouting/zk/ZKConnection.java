/**
 *
 * Copyright (c) 2015
 * All rights reserved.
 *
 * @Title ZKConnection.java
 * @Package com.probestar.dynamicrouting.zk
 * @author ProbeStar
 * @Email probestar@qq.com
 * @QQ 344137375
 * @date Aug 27, 2015 3:31:49 PM
 * @version V1.0
 * @Description 
 *
 */

package com.probestar.dynamicrouting.zk;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;

import com.probestar.psutils.PSTracer;

public class ZKConnection implements Watcher {
	private static PSTracer _tracer = PSTracer.getInstance(ZKConnection.class);

	private String _conn;
	private String _userName;
	private String _password;
	private boolean _newSession;
	private ZooKeeper _zk;
	private List<ACL> _acl;
	private ArrayList<ZKConnectionEvent> _events;

	public ZKConnection(String conn, String userName, String password) {
		try {
			_conn = conn;
			_userName = userName;
			_password = password;
			_newSession = true;
			_zk = createZooKeeper(_conn, _userName, _password);
			_acl = createAclList(userName, password);
			_events = new ArrayList<ZKConnectionEvent>();
		} catch (Exception ex) {
			_tracer.error("ZKConnection.ZKConnection error.", ex);
		}
	}

	public synchronized void register(ZKConnectionEvent event) {
		if (_events.contains(event)) {
			_tracer.error("ZKConnectionEvent has been registered. " + event.toString());
			return;
		}
		_events.add(event);
	}

	public synchronized void deregister(ZKConnectionEvent event) {
		_events.remove(event);
	}

	public boolean exists(String key) {
		String path = getPath(key);
		try {
			return !(_zk.exists(path, false) == null);
		} catch (Throwable t) {
			_tracer.error("ZKConnection.exists", t);
			return false;
		}
	}

	public void create(String key, byte[] data) throws InterruptedException, KeeperException {
		create(key, data, CreateMode.PERSISTENT);
	}

	public void createTempSeq(String key, byte[] data) throws InterruptedException, KeeperException {
		create(key, data, CreateMode.EPHEMERAL);
	}

	private void create(String key, byte[] data, CreateMode mode) throws InterruptedException, KeeperException {
		String path = getPath(key);
		_zk.create(path, data, _acl, mode);
		_tracer.info("Path %s has been created.", path);
	}

	public void delete(String key) throws InterruptedException, KeeperException {
		_zk.delete(getPath(key), -1);
		_tracer.info("Path %s has been deleted.", key);
	}

	public List<String> list() throws KeeperException, InterruptedException {
		return _zk.getChildren("/", false);
	}

	public void set(String key, byte[] value) throws Throwable {
		_zk.setData(getPath(key), value, -1);
	}

	public byte[] get(String key) throws KeeperException, InterruptedException {
		byte[] b = null;
		b = _zk.getData(getPath(key), null, null);
		return b;
	}

	public void close() {
		try {
			_zk.close();
		} catch (InterruptedException t) {
			_tracer.error("ZKConnection.close error.", t);
		}
	}

	private String getPath(String key) {
		return key.startsWith("/") ? key : "/" + key;
	}

	private ZooKeeper createZooKeeper(String conn, String userName, String password) throws IOException {
		ZooKeeper zk = new ZooKeeper(conn, 5000, this);
		if (userName != null && password != null)
			zk.addAuthInfo("digest", String.format("%s:%s", userName, password).getBytes());
		return zk;
	}

	private List<ACL> createAclList(String userName, String password) throws NoSuchAlgorithmException {
		List<ACL> list = new ArrayList<ACL>();
		if (userName != null && password != null) {
			Id id = new Id("digest", DigestAuthenticationProvider.generateDigest(String.format("%s:%s", userName, password)));
			ACL acl = new ACL(ZooDefs.Perms.ALL, id);
			list.add(acl);
		}
		list.addAll(Ids.READ_ACL_UNSAFE);
		return list;
	}

	public void fireDataChanged() throws KeeperException, InterruptedException {
		fireDataReset();
		List<String> list = list();
		for (String path : list)
			fireDataChanged(path);
	}

	private void fireConnected(boolean newSession) {
		for (ZKConnectionEvent event : _events)
			event.onConnected(newSession);
	}

	private void fireDisconnected() {
		for (ZKConnectionEvent event : _events)
			event.onDisconnected();
	}

	private void fireDataReset() {
		for (ZKConnectionEvent event : _events)
			event.onDataReset();
	}

	private void fireDataChanged(String path) throws KeeperException, InterruptedException {
		byte[] data = get(path);
		for (ZKConnectionEvent event : _events)
			event.onDataChanged(path, data);
	}

	private void fireDataRemoved(String key) {
		for (ZKConnectionEvent event : _events)
			event.onDataRemoved(key);
	}

	private void continued(String path) throws KeeperException, InterruptedException {
		if (path == null)
			path = "/";
		List<String> children = _zk.getChildren(path, true);
		for (String child : children)
			_zk.exists(path + child, true);
	}

	@Override
	public void process(WatchedEvent event) {
		_tracer.info("Received Event: " + event.toString());
		try {
			switch (event.getState()) {
			case Expired:
				_newSession = true;
				_zk.close();
				_zk = createZooKeeper(_conn, _userName, _password);
				break;
			case SyncConnected:
				switch (event.getType()) {
				case None:
					continued("/");
					fireConnected(_newSession);
					_newSession = false;
					break;
				case NodeChildrenChanged:
					continued(event.getPath());
					fireDataChanged();
					break;
				case NodeDeleted:
					continued("/");
					fireDataRemoved(event.getPath());
					break;
				default:
					_tracer.error("Unhandled Event: " + event.toString());
					break;
				}
				break;
			case Disconnected:
				switch (event.getType()) {
				case None:
					fireDisconnected();
					break;
				default:
					_tracer.error("Unhandled Event: " + event.toString());
					break;
				}
				break;
			default:
				_tracer.error("Unhandled Event: " + event.toString());
				break;
			}

		} catch (Throwable t) {
			_tracer.error("ZKConnection.process error.", t);
		}
	}

	@Override
	public String toString() {
		return _zk == null ? super.toString() : _zk.toString();
	}
}
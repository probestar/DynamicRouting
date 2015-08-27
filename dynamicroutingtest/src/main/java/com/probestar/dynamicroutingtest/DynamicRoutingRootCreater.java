/**
 *
 * Copyright (c) 2015
 * All rights reserved.
 *
 * @Title DynamicRoutingRootCreater.java
 * @Package com.probestar.dynamicroutingtest
 * @author ProbeStar
 * @Email probestar@qq.com
 * @QQ 344137375
 * @date Aug 27, 2015 4:19:24 PM
 * @version V1.0
 * @Description 
 *
 */

package com.probestar.dynamicroutingtest;

import java.util.HashMap;

import com.probestar.dynamicrouting.zk.ZKConnection;
import com.probestar.psutils.PSTracer;
import com.probestar.psutils.PSUtils;

public class DynamicRoutingRootCreater {
	private static PSTracer _tracer = PSTracer.getInstance(DynamicRoutingRootCreater.class);

	public static void main(String[] args) {
		try {
			HashMap<String, String> map = PSUtils.loadProperties("ZKSettings.properties");
			String conn = map.get("Servers");
			String userName = map.get("UserName");
			String password = map.get("Password");

			int index = conn.lastIndexOf('/');
			ZKConnection zk = new ZKConnection(conn.substring(0, index), userName, password);
			String nodeName = conn.substring(index + 1);
			zk.create(nodeName, "probestar@qq.com".getBytes());
		} catch (Throwable t) {
			_tracer.error("DynamicRoutingRootCreater.main error.", t);
		}
	}

}

/**
 *
 * Copyright (c) 2015
 * All rights reserved.
 *
 * @Title DynamicRoutingTest.java
 * @Package com.probestar.dynamicroutingtest
 * @author ProbeStar
 * @Email probestar@qq.com
 * @QQ 344137375
 * @date Aug 27, 2015 3:29:25 PM
 * @version V1.0
 * @Description 
 *
 */

package com.probestar.dynamicroutingtest;

import java.util.HashMap;

import com.probestar.dynamicrouting.DynamicRouting;
import com.probestar.dynamicrouting.RoutingModel;
import com.probestar.psutils.PSTracer;
import com.probestar.psutils.PSUtils;

public class DynamicRoutingTest {
	private static PSTracer _tracer = PSTracer.getInstance(DynamicRoutingTest.class);

	public static void main(String[] args) {
		try {
			HashMap<String, String> map = PSUtils.loadProperties("ZKSettings.properties");
			String conn = map.get("Servers");
			String userName = map.get("UserName");
			String password = map.get("Password");

			RoutingModel model = new RoutingModel();
			model.setKey("probestar.2");
			model.setUrl("ps://www.probestar.com/1");

			DynamicRouting.initialize(conn, userName, password);
			Thread.sleep(1000);
			DynamicRouting.getInstance().register(model);

			synchronized (DynamicRoutingTest.class) {
				DynamicRoutingTest.class.wait();
			}
		} catch (Throwable t) {
			_tracer.error("DynamicRoutingTest.main error.", t);
		}
	}

}

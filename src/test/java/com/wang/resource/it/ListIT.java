package com.wang.resource.it;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tinybrick.utils.http.RestClient;

public class ListIT {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	RestClient restClient = new RestClient();
	final static String listUrl = "http://localhost:8080/resource-server/list";

	@Test
	public void testListFolder() throws KeyManagementException, ClientProtocolException, NoSuchAlgorithmException,
			IOException {
		StringBuffer sb = new StringBuffer();
		int status = restClient.get(sb, listUrl);
		logger.debug("Status: " + status);
		Assert.assertEquals(200, status);
		logger.debug(sb.toString());

		JSONArray folders = new JSONArray(sb.toString());
		Assert.assertTrue(folders.length() >= 2);
	}
}

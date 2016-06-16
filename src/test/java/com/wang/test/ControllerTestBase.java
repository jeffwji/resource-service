package com.wang.test;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@DirtiesContext
@ContextHierarchy({ @ContextConfiguration(locations = { "classpath:applicationContext-servlet.xml" }) })
@WebAppConfiguration
public abstract class ControllerTestBase {
	@Autowired protected WebApplicationContext webApplicationContext;
	@Autowired protected MockHttpSession session;

	protected MockMvc mockMvc;

	@Before
	public void setUp() throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}
}

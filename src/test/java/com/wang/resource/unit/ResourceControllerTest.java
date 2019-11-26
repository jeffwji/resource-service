package com.wang.resource.unit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.FileCopyUtils;

import com.wang.test.ControllerTestBase;

@RunWith(SpringJUnit4ClassRunner.class)
public class ResourceControllerTest extends ControllerTestBase {
	static final String testJpeg = "/测试.jpg";
	static final String testPdf = "/Readme.pdf";

	@Test
	public void testGetPreferences() throws Exception {
		ResultActions resultActions = mockMvc.perform(get("/preferences").accept(MediaType.APPLICATION_JSON_VALUE));
		resultActions.andDo(print()).andExpect(status().isOk());

		return;
	}

	@Test
	public void testUploadImage() throws Exception {
		InputStream fis = this.getClass().getResourceAsStream(testJpeg);
		MockMultipartFile multipartFile = new MockMultipartFile("file", testJpeg, null,
				FileCopyUtils.copyToByteArray(fis));

		ResultActions resultActions = mockMvc.perform(fileUpload("/file").file(multipartFile).session(session)
				.contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON));
		resultActions.andDo(print()).andExpect(status().isOk());

		return;
	}

	@Test
	public void testDownloadImage() throws Exception {
		ResultActions resultActions = mockMvc.perform(get("/file/7079D463082FA28E5904DB71E0CEC2B3.jpg")
				.session(session).accept(MediaType.IMAGE_JPEG_VALUE));
		resultActions.andExpect(status().isOk());

		resultActions = mockMvc.perform(get("/file/7079D463082FA28E5904DB71E0CEC2B3.jpg?width=100&height=80").session(
				session).accept(MediaType.IMAGE_JPEG_VALUE));
		resultActions.andExpect(status().isOk());

		resultActions = mockMvc.perform(get("/file/7079D463082FA28E5904DB71E0CEC2B3.jpg?width=200&height=180").session(
				session).accept(MediaType.IMAGE_JPEG_VALUE));
		resultActions.andExpect(status().isOk());

		return;
	}

	@Test
	public void testUploadPdfFile() throws Exception {
		InputStream fis = this.getClass().getResourceAsStream(testPdf);
		MockMultipartFile multipartFile = new MockMultipartFile("file", testPdf, null,
				FileCopyUtils.copyToByteArray(fis));

		ResultActions resultActions = mockMvc.perform(fileUpload("/file").file(multipartFile).session(session)
				.contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON));
		resultActions.andDo(print()).andExpect(status().isOk());

		return;
	}

	@Test
	public void testDownloadPdfFile() throws Exception {
		ResultActions resultActions = mockMvc.perform(get("/file/DC01E80DE9D0B64408664E95F879FD57.pdf")
				.session(session).accept(MediaType.ALL_VALUE));
		resultActions.andExpect(status().isOk());

		resultActions = mockMvc.perform(get("/file/DC01E80DE9D0B64408664E95F879FD57.pdf?width=100&height=80").session(
				session).accept(MediaType.ALL_VALUE));
		resultActions.andExpect(status().isOk());

		return;
	}

	@Test
	public void testGetList() throws Exception {
		ResultActions resultActions = mockMvc.perform(get("/list/").session(session)
				.contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON));
		resultActions.andDo(print()).andExpect(status().isOk());

		return;
	}
}

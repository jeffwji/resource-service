package com.wang.services;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;

import com.wang.utils.crypto.MD5;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ResizeService {
	final Logger logger = LoggerFactory.getLogger(this.getClass());

	LinkedList<ResizeDetail> resizeCandidate = new LinkedList<ResizeDetail>();

	public void addCandidate(ResizeDetail candidate) {
		synchronized (resizeCandidate) {
			resizeCandidate.add(candidate);
		}
	}

	public byte[] resize(ResizeDetail candidate) throws IOException {
		String extName = getExtentionName(candidate.getFilePath());
		String resizedFileName = candidate.getFilePath().replace("." + extName,
				"_" + (null == candidate.getWidth() ? "-" : candidate.getWidth()) + "_"
						+ (null == candidate.getHeight() ? "-" : candidate.getHeight()) + "." + extName);

		logger.info("Generate image file: " + resizedFileName);
		File resizedFile = new File(resizedFileName);
		if (!resizedFile.exists()) {
			File file = new File(candidate.getFilePath());

			// 如果文件不存在
			if (!file.exists()) {
				throw new IOException("File not found!");
			}

			InputStream inFile = new FileInputStream(file);
			byte[] bytes = IOUtils.toByteArray(inFile);
			inFile.close();

			bytes = resizeImage(bytes, extName, candidate.getWidth(), candidate.getHeight());

			saveFile(bytes, resizedFileName);

			return bytes;
		}

		return null;
	}

	/**
	 * 生成新尺寸图片数据
	 * 
	 * @param bytes
	 * @param format
	 * @param width
	 * @param height
	 * @return
	 * @throws IOException
	 */
	public byte[] resizeImage(byte[] bytes, String format, Integer width, Integer height) throws IOException {
		InputStream sourceImageFile = new ByteArrayInputStream(bytes);
		BufferedImage img = ImageIO.read(sourceImageFile);
		Integer orginalWidth = img.getWidth();
		Integer orginalHeight = img.getHeight();

		if (null == width) {
			width = Math.round(orginalWidth * (height.floatValue() / orginalHeight.floatValue()));
		}
		if (null == height) {
			height = Math.round((orginalHeight * (width.floatValue() / orginalWidth.floatValue())));
		}

		Image scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);

		BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		thumbnail.createGraphics().drawImage(scaledImg, 0, 0, null);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(thumbnail, format, out);

		byte[] imageBytes = out.toByteArray();

		return imageBytes;
	}

	/**
	 * 保存文件
	 * 
	 * @param bytes
	 * @param dest
	 * @return
	 * @throws IOException
	 */
	public boolean saveFile(byte[] bytes, String dest) throws IOException {
		return saveFile(bytes, dest, false);
	}

	public boolean saveFile(byte[] bytes, String dest, boolean overwrite) throws IOException {
		File destFile = new File(dest);
		if (overwrite || !destFile.exists()) {
			OutputStream outFile = new FileOutputStream(new File(dest));
			outFile.write(bytes);
			outFile.close();
			return true;
		}
		return false;
	}

	/**
	 * 获得文件扩展名
	 * 
	 * @param fullPath
	 * @return
	 */
	private String getExtentionName(final String fullPath) {
		String extName = "";
		int sep = fullPath.lastIndexOf(".") + 1;
		if (sep > 0) {
			extName = fullPath.substring(sep).toLowerCase();
		}

		return extName;
	}

	/**
	 * Start async thread for standard file generation
	 */
	@PostConstruct
	public void resizeDeamon() {
		Thread thread = new Thread() {
			@Override
			public void run() {
				while (true) {
					try {
						ResizeDetail resizeDetail = null;
						synchronized (resizeCandidate) {
							resizeDetail = resizeCandidate.poll();
						}

						if (null != resizeDetail) {
							resize(resizeDetail);
						}
						else {
							Thread.sleep(1000);
						}
					}
					catch (InterruptedException e) {
						logger.error(e.getMessage(), e);
					}
					catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
			}
		};

		thread.start();
	}
}

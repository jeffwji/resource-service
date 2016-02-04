package com.wang.controllers;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import com.wang.resource.exceptions.NotModifiedException;
import com.wang.services.ResizeDetail;
import com.wang.services.ResizeService;
import com.wang.utils.crypto.MD5;

/**
 * @author jeff
 */

@Controller
@RequestMapping("/")
public class ResourceController {
	final Logger logger = LoggerFactory.getLogger(this.getClass());

	String repository = "/data/uploaded";
	Tika tika = new Tika();

	boolean watermarkEnabled = true;
	String pressText = "www.htche.com";
	String fontName = "SimSun";
	int fontStyle = Font.PLAIN;
	Color color = Color.WHITE;
	int fontSize = 12;
	int marginX = 2;
	int marginY = 2;
	float alpha = 1f;
	int degree = 0;

	int poolSize = 25;
	ExecutorService executor = Executors.newFixedThreadPool(poolSize);

	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
		executor = Executors.newFixedThreadPool(poolSize);
	}

	boolean multipleThread = true;

	public void setMultipleThread(boolean multipleThread) {
		this.multipleThread = multipleThread;
	}

	public boolean isWatermarkEnabled() {
		return watermarkEnabled;
	}

	public void setWatermarkEnabled(boolean watermarkEnabled) {
		this.watermarkEnabled = watermarkEnabled;
	}

	public String getPressText() {
		return pressText;
	}

	public void setPressText(String pressText) {
		this.pressText = pressText;
	}

	public String getFontName() {
		return fontName;
	}

	public void setFontName(String fontName) {
		this.fontName = fontName;
	}

	public int getFontStyle() {
		return fontStyle;
	}

	public void setFontStyle(int fontStyle) {
		this.fontStyle = fontStyle;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public int getFontSize() {
		return fontSize;
	}

	public void setFontSize(int fontSize) {
		this.fontSize = fontSize;
	}

	public float getAlpha() {
		return alpha;
	}

	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}

	public int getDegree() {
		return degree;
	}

	public void setDegree(int degree) {
		this.degree = degree;
	}

	Map<String, Object> preferences = new HashMap<String, Object>();

	public static final String PREFERENCE_NAME_STANDARD_WIDTH = "STANDARD_WIDTH";
	public static final String PREFERENCE_NAME_STANDARD_HEIGHT = "STANDARD_HEIGHT";
	public static final String PREFERENCE_NAME_GENERATE_STANDARD_IMAGE = "GENERATE_STANDARD_IMAGE";

	// 是否生成标准图。图片的源文件尺寸有可能比较大，为避免每次都从较大文件中生成小文件，图片上载后可以生成一个适中的“标准”文件，较小图片可以由这个“标准”文件生成，以加快处理速度
	//boolean generateStandardImageFile = false;

	public Boolean isGenerateStandardImageFile() {
		return (Boolean) preferences.get(PREFERENCE_NAME_GENERATE_STANDARD_IMAGE);
	}

	public void setGenerateStandardImageFile(Boolean generateStandardImageFile) {
		preferences.put(PREFERENCE_NAME_GENERATE_STANDARD_IMAGE, generateStandardImageFile);
	}

	// 标准文件尺寸
	public Integer getStandardWidth() {
		return (Integer) preferences.get(PREFERENCE_NAME_STANDARD_WIDTH);
	}

	public void setStandardWidth(Integer standardWidth) {
		preferences.put(PREFERENCE_NAME_STANDARD_WIDTH, standardWidth);
	}

	public int getStandardHeight() {
		return (Integer) preferences.get(PREFERENCE_NAME_STANDARD_HEIGHT);
	}

	public void setStandardHeight(Integer standardHeight) {
		preferences.put(PREFERENCE_NAME_STANDARD_HEIGHT, standardHeight);
	}

	// 子目录长度。取文件名前两位，最多可能产生 36*36 （26个英文字母加上10个数字）个子目录
	static final int LENGTH_OF_SUB_FOLDER_NAME = 2;

	public String getRepository() {
		return repository;
	}

	public void setRepository(String repository) {
		this.repository = repository.startsWith("/") ? repository : ("/" + repository);
	}

	@RequestMapping(value = "preferences", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
	public @ResponseBody Map<String, Object> getPreferences() {
		return preferences;
	}

	@Autowired ResizeService resizeService;

	/**
	 * 上载文件
	 * 
	 * @param files
	 * @param response
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	@RequestMapping(value = "upload", method = RequestMethod.POST, consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
	public @ResponseBody Map<String, String> uploadFiles(@RequestParam("file") MultipartFile[] files)
			throws IllegalStateException, IOException {
		Map<String, String> fileMap = new HashMap<String, String>();

		List<Future<Map<String, String>>> futureList = new ArrayList<Future<Map<String, String>>>();

		if (multipleThread) {
			// Upload files parallel

			// Create future service
			for (final MultipartFile file : files) {
				Callable<Map<String, String>> callable = new Callable<Map<String, String>>() {
					@Override
					public Map<String, String> call() throws Exception {
						logger.debug("[Thread-" + Thread.currentThread().getId() + "] New thread has been creating for "
								+ file.getOriginalFilename() + " uploading");
						return saveSingleFile(file);
					}
				};

				Future<Map<String, String>> future = executor.submit(callable);
				futureList.add(future);
			}

			// Collect results
			for (Future<Map<String, String>> future : futureList) {
				try {
					fileMap.putAll(future.get());
					logger.debug("Thread is finished.");
				}
				catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
		else {
			// Upload files one by one
			for (final MultipartFile file : files) {
				fileMap.putAll(saveSingleFile(file));
			}
		}

		return fileMap;
	}

	/**
	 * 上载文件
	 * 
	 * @param files
	 * @param response
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	@RequestMapping(value = "file", method = RequestMethod.POST, consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
	public @ResponseBody List<String> fileUpload(@RequestParam("file") MultipartFile[] files)
			throws IllegalStateException, IOException {
		return new ArrayList<String>(uploadFiles(files).values());
	}

	/**
	 * @param file
	 * @return
	 * @throws IOException
	 */
	private Map<String, String> saveSingleFile(MultipartFile file) throws IOException {
		Map<String, String> uploadInfo = new HashMap<String, String>();
		if (file != null) {
			String originalFilename = file.getOriginalFilename();
			try {
				logger.debug(
						"[Thread-" + Thread.currentThread().getId() + "]: " + originalFilename + " is being uploaded.");

				// Generate filename
				String extName = getExtentionName(originalFilename);

				InputStream inFile = file.getInputStream();
				byte[] bytes = IOUtils.toByteArray(inFile);
				inFile.close();

				// 为避免文件重复上载，算出文件的MD5值作为文件名
				String fileName = MD5.hash(bytes);

				String repositoryPath = getFullFilePath(fileName);
				// Save file
				String realFileName = getRealPath(repositoryPath, fileName, extName);
				resizeService.saveFile(bytes, realFileName);

				// Generate standard image file
				if (isGenerateStandardImageFile() && getStandardWidth() > 0 && getStandardHeight() > 0
						&& getMediaType(bytes).startsWith("image/")) {
					Dimension dimension = getDimension(bytes);
					if (dimension.getWidth() > getStandardWidth() && dimension.getHeight() > getStandardHeight()) {
						/*String resizedFileName = realFileName.replace("." + extName, "_" + getStandardWidth() + "_"
								+ getStandardHeight() + "." + extName);
						logger.info("Generate standard image file: " + resizedFileName);
						File resizedFile = new File(resizedFileName);
						if (!resizedFile.exists()) {
							bytes = resizeService.resizeImage(bytes, extName, getStandardWidth(), getStandardHeight());
							resizeService.saveFile(bytes, resizedFileName);
						}*/

						// Add file to resize list
						ResizeDetail candidate = new ResizeDetail();
						candidate.setFilePath(realFileName);
						candidate.setWidth(getStandardWidth());
						candidate.setHeight(getStandardHeight());
						resizeService.addCandidate(candidate);
					}
				}

				logger.debug("[Thread-" + Thread.currentThread().getId() + "]: " + originalFilename + " is uploaded");
				uploadInfo.put(originalFilename, fileName + "." + extName);
			}
			catch (Exception e) {
				logger.error(e.getMessage(), e);
				uploadInfo.put(originalFilename, null);
			}

			return uploadInfo;
		}

		logger.debug("[Thread-" + Thread.currentThread().getId() + "]: None has been uploaded");
		return null;
	}

	/**
	 * 根据文件名获得完整路径
	 * 
	 * @param fileName
	 * @return
	 */
	private String getFullFilePath(String fileName) {
		String subFolder = getSubFolder(fileName);
		String repositoryPath = repository + "/" + subFolder;

		File localFile = new File(repositoryPath);
		if (!localFile.exists()) {
			localFile.mkdirs();
		}

		return repositoryPath;
	}

	/**
	 * 根据文件名生成子目录名
	 * 
	 * @param fileName
	 * @return
	 */
	private String getSubFolder(String fileName) {
		return fileName.substring(0, LENGTH_OF_SUB_FOLDER_NAME);
	}

	/**
	 * 打水印
	 * 
	 * @param id
	 * @param width
	 * @param height
	 * @param watermark
	 * @param watermarkFontSize
	 * @param watermarkFontStyle
	 * @param fontColor
	 * @param positionX
	 * @param positionY
	 * @param refresh
	 * @param ifModifiedSince
	 * @param response
	 * @throws IOException
	 */
	@RequestMapping(value = "file/{id:.+}", method = RequestMethod.GET)
	public void fileDownload(@PathVariable("id") String id,
			@RequestParam(value = "width", required = false) Integer width,
			@RequestParam(value = "height", required = false) Integer height,
			@RequestParam(value = "wm", required = false) String watermark,
			@RequestParam(value = "fs", required = false) Integer watermarkFontSize,
			@RequestParam(value = "ft", required = false) Integer watermarkFontStyle,
			@RequestParam(value = "fc", required = false) Integer fontColor,
			@RequestParam(value = "x", required = false) Double positionX,
			@RequestParam(value = "y", required = false) Double positionY,
			@RequestParam(value = "a", required = false) Float watermarkAlpha,
			@RequestParam(value = "refresh", required = false) Boolean refresh,
			@RequestParam(value = "fmt", required = false) String format,
			@RequestHeader(value = "If-Modified-Since", required = false) String ifModifiedSince,
			HttpServletResponse response) throws IOException {

		// when;whom;from;do(request);what(width|height)
		String subFolder = getSubFolder(id);
		String fullPath = repository + "/" + subFolder + "/" + id;
		String extName = getExtentionName(fullPath);

		SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss:SSS", Locale.ENGLISH);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date modifySince = null;
		if (null != ifModifiedSince) {
			try {
				modifySince = sdf.parse(ifModifiedSince);
			}
			catch (ParseException e) {
				logger.warn(e.getMessage(), e);
				modifySince = null;
			}
		}

		byte[] bytes = null;
		bytes = getResourceFile(fullPath, width, height, (null != refresh && refresh) ? null : modifySince, response);

		if (getMediaType(bytes).startsWith("image/")
				&& ((null != watermark && (watermark.toLowerCase().equals("false") || 0 == watermark.trim().length()))
						? false : watermarkEnabled)) {
			logger.info("Applying watermark.");
			// 如果是图片，尝试打水印
			bytes = applyTextWatermark(bytes, null == format ? extName : format, positionX, positionY,
					null == watermark ? pressText : watermark, fontName,
					null == watermarkFontStyle ? fontStyle : watermarkFontStyle,
					null == fontColor ? color : new Color(fontColor), null == watermarkAlpha ? alpha : watermarkAlpha,
					null == watermarkFontSize ? fontSize : watermarkFontSize);
		}

		response.setContentLength(bytes.length);
		response.setContentType(getMediaType(bytes));
		FileCopyUtils.copy(bytes, response.getOutputStream());
	}

	/**
	 * 打水印
	 * 
	 * @param bytes
	 * @param format
	 * @param positionX
	 * @param positionY
	 * @param pressText
	 * @param fontName
	 * @param fontStyle
	 * @param color
	 * @param fontSize
	 * @return
	 * @throws IOException
	 */
	protected byte[] applyTextWatermark(byte[] bytes, String format, Double positionX, Double positionY,
			String pressText, String fontName, int fontStyle, Color color, float fontAlpha, int fontSize)
					throws IOException {
		InputStream sourceImageFile = new ByteArrayInputStream(bytes);
		BufferedImage srcImg = ImageIO.read(sourceImageFile);
		Graphics2D g = (Graphics2D) srcImg.getGraphics();

		// 抗锯齿
		if (18 <= fontSize) {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}

		// 3、设置水印旋转
		if (0 != degree) {
			g.rotate(Math.toRadians(degree), (double) srcImg.getWidth() / 2, (double) srcImg.getHeight() / 2);
		}

		// 设置透明度
		if (1 > fontAlpha) {
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, fontAlpha));
		}

		Font font = new Font(fontName, fontStyle, fontSize);
		// Watermark position
		double x = 0;
		double y = 0;
		if (null != positionX && null != positionY) {
			x = positionX;
			y = positionY;
		}
		else {
			FontRenderContext context = g.getFontRenderContext();
			Rectangle2D bounds = font.getStringBounds(pressText, context);
			x = (srcImg.getWidth() - bounds.getWidth()) - marginX;
			y = (srcImg.getHeight() - (bounds.getHeight() + bounds.getY())) - marginY;
		}

		g.setColor(color);
		g.setFont(font);
		g.drawString(pressText, (int) (0 > x ? 0 : x), (int) (y));
		g.dispose();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(srcImg, format, out);
		byte[] imageBytes = out.toByteArray();

		return imageBytes;
	}

	private void setLastModified(Date lastModifed, HttpServletResponse response) {
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss:SSS", Locale.ENGLISH);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		response.setHeader("Last-Modified", sdf.format(lastModifed) + " GMT");
	}

	/**
	 * 获得资源文件并生成裁切图
	 * 
	 * @param fullPath
	 * @param width
	 * @param height
	 * @param modifiedSince
	 * @param response
	 * @return
	 * @throws IOException
	 */
	private byte[] getResourceFile(String fullPath, Integer width, Integer height, Date modifiedSince,
			HttpServletResponse response) throws IOException {
		byte[] bytes = null;

		String extName = getExtentionName(fullPath);
		String fileName = fullPath;

		File file = null;
		boolean resizedFileExists = true;
		if (width != null || height != null) {
			// 资源文件名
			fileName = fullPath.replace("." + extName,
					"_" + (null == width ? "-" : width) + "_" + (null == height ? "-" : height) + "." + extName);
			file = new File(fileName);
			resizedFileExists = file.exists();

			if (!resizedFileExists) {
				// 如果不能确定文件尺寸比标准文件小，则读取源文件
				if ((null == width || null == height) || (width > getStandardWidth() || height > getStandardHeight())) {
					file = new File(fullPath);
				}
				// 否则读取标准文件
				else {
					String standardFileName = fullPath.replace("." + extName,
							"_" + getStandardWidth() + "_" + getStandardHeight() + "." + extName);

					file = new File(standardFileName);
					// 如果标准文件不存在
					if (!file.exists()) {
						file = new File(fullPath);
					}
				}
			}
		}
		else {
			file = new File(fileName);
		}

		// 如果文件不存在
		if (!file.exists()) {
			throw new IOException("File not found!");
		}

		if (null != modifiedSince && 0 == modifiedSince.compareTo(new Date(file.lastModified()))) {
			throw new NotModifiedException();
		}

		InputStream inFile = new FileInputStream(file);
		bytes = IOUtils.toByteArray(inFile);
		inFile.close();

		if (!resizedFileExists) {
			bytes = generateResizedResource(bytes, width, height, fileName);
		}

		file = new File(fileName);
		Date lastModified = new Date(file.lastModified());
		setLastModified(lastModified, response);

		return bytes;
	}

	/**
	 * @param bytes
	 * @param width
	 * @param height
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	private byte[] generateResizedResource(byte[] bytes, Integer width, Integer height, String fileName)
			throws IOException {
		String extName = getExtentionName(fileName);

		if (getMediaType(bytes).startsWith("image/")) {
			// 是否图像文件，能否裁切？
			logger.info("Resize image to " + (null == width ? "-" : width) + " x " + (null == height ? "-" : height));
			bytes = resizeService.resizeImage(bytes, extName, width, height);
			resizeService.saveFile(bytes, fileName);
		}
		else {
			// 非图像文件，不可裁切，无法获得裁切文件数据
			logger.info("Not an image file, resizing is ignored.");
		}

		return bytes;
	}

	/**
	 * 列表指令
	 * 
	 * @param path
	 * @param extension
	 * @param from
	 * @param to
	 * @return
	 */
	@RequestMapping(value = "list", method = RequestMethod.GET)
	@ResponseStatus(value = HttpStatus.OK)
	public @ResponseBody String[] list(@RequestParam(required = false) String path,
			@RequestParam(required = false) final String extension, @RequestParam(required = false) final Date from,
			@RequestParam(required = false) final Date to) {

		FileFilter filter = new ResourceFiltre(extension, from, to);
		File repositoryFolder = new File(null == path ? repository : repository + "/" + path);
		File[] resources = repositoryFolder.listFiles(filter);

		String[] resourcePathes = new String[resources.length];
		for (int i = 0; i < resources.length; i++) {
			resourcePathes[i] = resources[i].getName();
		}

		return resourcePathes;
	}

	/**
	 * IO异常
	 * 
	 * @param e
	 */
	@ExceptionHandler(IOException.class)
	@ResponseStatus(value = HttpStatus.NOT_FOUND)
	public void onIOException(IOException e) {
		logger.error(e.getMessage(), e);
	}

	/**
	 * 其他异常
	 * 
	 * @param e
	 */
	@ExceptionHandler(RuntimeException.class)
	@ResponseStatus(value = HttpStatus.EXPECTATION_FAILED)
	public void onGeneralException(RuntimeException e) {
		logger.error(e.getMessage(), e);
	}

	/**
	 * 非异常处理，文件未成改动，返回 304 状态码
	 * 
	 * @param e
	 */
	@ExceptionHandler(NotModifiedException.class)
	@ResponseStatus(value = HttpStatus.NOT_MODIFIED)
	public void onExceptionHandler(NotModifiedException e) {
		logger.debug("Resource not modified");
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
	 * 获得文件本地目录
	 * 
	 * @param repositoryPath
	 * @param fileName
	 * @param extName
	 * @return
	 */
	protected String getRealPath(String repositoryPath, String fileName, String extName) {
		return repositoryPath + "/" + fileName + (extName.length() > 0 ? ("." + extName) : "");
	}

	/**
	 * 保存文件
	 * 
	 * @param bytes
	 * @param dest
	 * @return
	 * @throws IOException
	 */
	/*protected boolean saveFile(byte[] bytes, String dest) throws IOException {
		return saveFile(bytes, dest, false);
	}*/

	/*protected boolean saveFile(byte[] bytes, String dest, boolean overwrite) throws IOException {
		File destFile = new File(dest);
		if (overwrite || !destFile.exists()) {
			OutputStream outFile = new FileOutputStream(new File(dest));
			outFile.write(bytes);
			outFile.close();
			return true;
		}
		return false;
	}*/

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
	/*protected byte[] resizeImage(byte[] bytes, String format, Integer width, Integer height) throws IOException {
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
	}*/

	/**
	 * 获得文件类型
	 * 
	 * @param content
	 * @return
	 */
	private String getMediaType(byte[] content) {
		return tika.detect(content);
	}

	/**
	 * 获得图片尺寸
	 * 
	 * @param image
	 * @return
	 * @throws IOException
	 */
	private Dimension getDimension(byte[] image) throws IOException {
		Dimension dimension = new Dimension();
		BufferedImage img = ImageIO.read(new ByteArrayInputStream(image));
		dimension.setSize(img.getWidth(), img.getHeight());
		return dimension;
	}

	/**
	 * 文件过滤器
	 * 
	 * @author jeff
	 */
	static class ResourceFiltre implements FileFilter {
		String extension;
		Date from;
		Date to;

		public ResourceFiltre(String extension, Date from2, Date to2) {
			this.extension = extension;
			this.from = from2;
			this.to = to2;
		}

		@Override
		public boolean accept(File pathname) {
			// Check extension type
			if (null != extension && !pathname.getPath().endsWith("." + extension))
				return false;

			// Check time
			Date lastModified = new Date(pathname.lastModified());
			lastModified = DateUtils.truncate(lastModified, Calendar.DATE);
			Calendar calLastModified = Calendar.getInstance();
			calLastModified.setTime(lastModified);

			// Select from.. to end.
			if (null != from && null == to) {
				return isAfter(calLastModified);
			}
			// Select from .. to ..
			else if (null != from && null != to) {
				return isDuration(calLastModified);
			}
			// Select all untill 'to'
			else if (null == from && null != to) {
				return isBefore(calLastModified);
			}

			return true;
		}

		private boolean isAfter(Calendar calLastModified) {
			Calendar calFrom = Calendar.getInstance();
			calFrom.setTime(DateUtils.truncate(from, Calendar.DATE));

			return calLastModified.after(calFrom);
		}

		private boolean isDuration(Calendar calLastModified) {
			Calendar calFrom = Calendar.getInstance();
			calFrom.setTime(DateUtils.truncate(from, Calendar.DATE));

			Calendar calTo = Calendar.getInstance();
			calTo.setTime(DateUtils.truncate(to, Calendar.DATE));

			return calLastModified.after(calFrom) && calLastModified.before(calTo);
		}

		private boolean isBefore(Calendar calLastModified) {
			Calendar calTo = Calendar.getInstance();
			calTo.setTime(DateUtils.truncate(to, Calendar.DATE));

			return calLastModified.before(calTo);
		}
	}
}

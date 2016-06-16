<%@page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jstl/core_rt"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<%
	String basePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
	%>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>uploadify 文件上传</title>
<script type="text/javascript"
	src="<%=basePath%>/plugins/uploadify/jquery-1.6.4.js"></script>
<script type="text/javascript"
	src="<%=basePath%>/plugins/uploadify/uploadify_lang_cn.js"></script>
<script type="text/javascript"
	src="<%=basePath%>/plugins/uploadify/jquery.uploadify-3.2.js"></script>
<script type="text/javascript">

	$(window).load(function() {
		 $("#uploadify").uploadify({
		        height        : 30,
		        swf           : '<%=basePath%>/plugins/uploadify/uploadify.swf',
		        uploader      : '<%=basePath%>/file',
		        width         : 120,
		        fileObjName   : 'file',
		        fileTypeDesc  : '请选择图片文件',
		        fileExt 	  : '*.jpg',
		        onUploadSuccess: function(file, data, response){
		        	alert(data);
		        }
		    });
	});
	</script>
	
<link rel="stylesheet" type="text/css" href="<%=basePath%>/plugins/uploadify/uploadify.css">
</head>
<body>
	<input type="file" name="uploadify" id="uploadify" />
</body>
</html>
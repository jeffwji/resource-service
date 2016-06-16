<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jstl/core_rt"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>文件上传</title>
</head>
<body>
	<script type="text/javascript" src="<c:url value='http://sms.test.wang.com/js/ux/main/main.js'/>"></script>
	<script type="text/javascript" src="<c:url value='http://sms.test.wang.com/js/commons/jquery.min.js'/>"></script>
	<script type="text/javascript" src="<c:url value='http://sms.test.wang.com/js/commons/jquery.form.js'/>"></script>

	<form action="<c:url value='/file'/>" method="post" enctype="multipart/form-data">
		<input type="file" name="file" multiple="multiple" /> <input type="submit" value="上传" />
	</form>
	
	<form id="uploadForm" action="<c:url value='/file?width=100&height=80'/>" name="UploadForm"
		enctype="multipart/form-data" method="post" onsubmit="return chickfile();">
		<input type="file" size="25" name="file" id="uploadedFile" onchange="selectOnePicture();" style="display: none" multiple="multiple" />
	</form>
	<img id="img11" alt="" src="http://7u2nae.com1.z0.glb.clouddn.com/2.jpg" onclick="F_Open_dialog()" />

	<script type="text/javascript">
		var nowPicture;

		function F_Open_dialog()
		{
			document.getElementById("uploadedFile").click();
			nowPicture = $(this);
		}
		
		//单张图片上传的时候
		function selectOnePicture(){
			var id;
			alert("begin");
			var form=document.getElementById("uploadForm");
			var formData=new FormData(form);
			$.ajax({
				type:"post",
				url:"http://localhost:8080/resource-server/file?width=100&height=80",
				data:formData,
				dataType:"json",
				processData: false, //  告诉jQuery 不要去处理发送的数据
				contentType: false,
				success:function(data){
				alert(data);
				//id=data;
				//nowPicture.attr("src","#"+data[0]+"");
				}//success
			});
		}
		</script>
</body>
</html>
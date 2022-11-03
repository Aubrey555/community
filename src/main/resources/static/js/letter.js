$(function(){
	$("#sendBtn").click(send_letter);/*对发送私信界面的id="sendBtn"发送按钮进行响应,点击按钮后即可跳到该方法*/
	$(".close").click(delete_msg);
});

function send_letter() {
	$("#sendModal").modal("hide");//单击后即将发送私信框进行隐藏
	//向服务端发送数据,得到服务端响应的结果后再关闭提示框
		//1.从页面上获取私信发送的接受方toName以及内容content
	var toName = $("#recipient-name").val();//获得接受方name(与请求方法的参数一致)
	var content = $("#message-text").val();//获得私信内容
	$.post(				//异步发送post请求
		CONTEXT_PATH + "/letter/send",	//发送的请求路径
		{"toName":toName,"content":content},//传输的参数
		function(data) {		//处理服务端返回的结果,存储在data中
			data = $.parseJSON(data);//data为满足json格式的字符串,转化为js对象
			if(data.code == 0) {	//发送成功,提示到提示框id="#hintBody"属性
				$("#hintBody").text("发送成功!");
			} else {				//否则发送失败,显示失败原因
				$("#hintBody").text(data.msg);
			}
			//对页面进行异步刷新:即先弹出提示框,2s后就关闭
			$("#hintModal").modal("show");
			setTimeout(function(){
				$("#hintModal").modal("hide");
				location.reload();	//重载当前页面
			}, 2000);
		}
	);
}

function delete_msg() {
	// TODO 删除数据
	$(this).parents(".media").remove();
}
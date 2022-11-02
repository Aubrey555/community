$(function(){
	$("#publishBtn").click(publish);/*发布按钮绑定该函数,该函数又调用下面的Publis()函数*/
});

function publish() {
	$("#publishModal").modal("hide");/*1. 发布完成后隐藏弹出框*/
	/*2. 向服务器发送异步ajax请求*/
		//2.1 获取弹出框id="publishBtn"中的标题和内容
	var title = $("#recipient-name").val();//发布标签中的id=recipient-name子标签即对应标题内容
	var content = $("#message-text").val();//帖子内容(id = message-text)
		//2.2 发送异步请求(POST；表单提交数据)
	$.post(
		CONTEXT_PATH + "/discuss/add",// 访问路径
		{"title":title,"content":content},// 传入的数据
		//2.3 处理返回的数据(data即为服务器向浏览器相应的数据,为JSON格式的字符串)
		function(data) {
			data = $.parseJSON(data);//得到js对象
			// 在提示框中显示服务器返回的消息
			$("#hintBody").text(data.msg);
		/*3. 弹出框隐藏后,显示提示框*/
		$("#hintModal").modal("show");
		//4. 提示框显示后,2s后自动隐藏
		setTimeout(function () {
			$("#hintModal").modal("hide");
			//当服务器向浏览器响应的数据data中的属性code=0时,表示发布成功,则刷新页面
			if (data.code == 0) {
				window.location.reload();
			}
		}, 2000);
		}
	);
}

$(function(){
	$(".follow-btn").click(follow);
});

function follow() {
	var btn = this;
	if($(btn).hasClass("btn-info")) {//如果为一个蓝色的样式,则进行关注
		// 关注TA(此处关注的对象为人,即为ENTITY_TYPE_USER = 3)
		$.post(
			CONTEXT_PATH + "/follow",//异步请求的请求路径
			//传入参数为:当前实体类型	+  当前实体对象的id(即当前按钮的值)
			{"entityType":3,"entityId":$(btn).prev().val()},
			function(data) {//服务器向浏览器响应数据data
				data = $.parseJSON(data);//将data转化为json对象
				if(data.code == 0) {//如果data返回的code为0,表示关注成功,重新刷新界面
					window.location.reload();
				} else {
					//否则关注失败,弹出异常处理器handleException中添加的数据
					alert(data.msg);
				}
			}
		);
		// 关注TA,显示已关注信息
		//$(btn).text("已关注").removeClass("btn-info").addClass("btn-secondary");
	} else {
		// 取消关注(否则为一个灰色样式,取消关注)
		// 取消关注	与前面的处理逻辑一致
		$.post(
			CONTEXT_PATH + "/unfollow",
			{"entityType":3,"entityId":$(btn).prev().val()},
			function(data) {
				data = $.parseJSON(data);
				if(data.code == 0) {
					window.location.reload();
				} else {
					alert(data.msg);
				}
			}
		);
	//已经关注
		//$(btn).text("关注TA").removeClass("btn-secondary").addClass("btn-info");
	}
}
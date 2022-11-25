$(function(){       //该函数类似于window.load,即在页面加载完成后获取此按钮绑定事件
    $("#topBtn").click(setTop);     //即表示对于id="topBtn"(置顶)的单机框按钮绑定的单击事件为setTop
    $("#wonderfulBtn").click(setWonderful); //表示对于id="wonderfulBtn"(加精)的单机框按钮绑定的单击事件为setWonderful
    $("#deleteBtn").click(setDelete);   //表示对于id="deleteBtn"(删除)的单机框按钮绑定的单击事件为setDelete
});

function like(btn, entityType, entityId, entityUserId,postId) {    //定义like()方法,响应discuss-detail界面的单击事件
    //接收前端传入的参数(btn:对应的点赞位置按钮(一共三个) entityType:被点赞的实体类(帖子/评论/评论的回复) entityId:实体类对应的id)
    $.post( //发送异步请求
        CONTEXT_PATH + "/like",//请求路径
        {"entityType":entityType,"entityId":entityId,"entityUserId":entityUserId,"postId":postId},//异步请求携带参数
        function(data) {//服务器返回数据data
            data = $.parseJSON(data);//将服务器返回的数据转化为JS对象
            if(data.code == 0) {//data中的code为0表示处理成功
                //btn为当前点赞的按钮(discuss-detail页面共3个点赞的位置)
                $(btn).children("i").text(data.likeCount);//btn按钮下的子节点<i>标签的值为:data中的likeCount(已赞的总数)
                $(btn).children("b").text(data.likeStatus==1?'已赞':"赞");//btn按钮下的子节点<b>标签的值为，根据返回的状态决定
            } else {
                //此时对于异步请求报错,返回的是异常通知类ExceptionAdvice中对于错误信息抛出的异常
                alert(data.msg);//此时表示点赞失败,返回错误信息
            }
        }
    );
}

// 置顶
function setTop() {
    $.post( //异步的ajax请求,并传递相关参数
        CONTEXT_PATH + "/discuss/top",  //异步ajax请求路径
        {"id":$("#postId").val()},  //传递参数
        function(data) {    //data中封装服务器返回的JSON格式的字符串数据
            data = $.parseJSON(data);   //将此字符串数据转化为JSON对象
            if(data.code == 0) {    //如果此JSON对象中的code属性值为0,表示置顶成功
                $("#topBtn").attr("disabled", "disabled");//成功后则将此topBtn对应的button按钮的disabled属性设置为"disabled",表示按钮不可用
            } else {
                alert(data.msg);//表示指定失败,传入提示信息msg(ExceptionAdvice通知类中封装了服务器异常的提示信息)
            }
        }
    );
}

// 加精请求(类似于置顶)
function setWonderful() {
    $.post(
        CONTEXT_PATH + "/discuss/wonderful",
        {"id":$("#postId").val()},
        function(data) {
            data = $.parseJSON(data);
            if(data.code == 0) {
                $("#wonderfulBtn").attr("disabled", "disabled");
            } else {
                alert(data.msg);
            }
        }
    );
}

// 删除请求(类似于置顶)
function setDelete() {
    $.post(
        CONTEXT_PATH + "/discuss/delete",
        {"id":$("#postId").val()},
        function(data) {
            data = $.parseJSON(data);
            if(data.code == 0) {    //删除完成后,按钮就不用重新设置,帖子不存在,回到帖子首页即可
                location.href = CONTEXT_PATH + "/index";
            } else {
                alert(data.msg);
            }
        }
    );
}
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
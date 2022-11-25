$(function(){
    $("#uploadForm").submit(upload);//该函数完成:表单(id=uploadForm)点击提交后,触发upload()事件
});

function upload() { //upload事件发送异步请求,将相关头像文件上传到七牛云服务器
    $.ajax({        //ajax异步请求处理
        url: "http://upload-z1.qiniup.com",//请求提交路径(七牛云的华北区域,客户端上传文件的提交路径,不同区域不同)
        method: "post", //请求提交方式
        processData: false, //表示不要将表单内容转化为字符串(默认情况下会将表单内容转化为字符串提交给服务器)
        contentType: false, //表示不让Jquery设置上传文件的类型(浏览器会进行自动设置)
        data: new FormData($("#uploadForm")[0]),    //data即为需要上传的数据,当上传文件时使用new FormData进行特殊处理,将对应表单传入即可(id=uploadForm的表单)
        success: function(data) {   //成功后进行success处理,data中封装上传成功后的响应信息,七牛云服务器返回的data数据即为JSON对象
            if(data && data.code == 0) {//表示上传成功
                // 上传成功则使用异步方式更新头像的访问路径
                $.post(
                    CONTEXT_PATH + "/user/header/url",  //提交给当前应用,进行头像更新处理
                    {"fileName":$("input[name='key']").val()},  //得到表单中的name=key对应的属性,作为更新头像文件的文件名
                    function(data) {        //服务端做出的响应
                        data = $.parseJSON(data);   //解析为JSON格式的对象,当前应用返回JSON格式的字符串
                        if(data.code == 0) {    //更新成功
                            window.location.reload();
                        } else {                //更新失败,返回提示信息
                            alert(data.msg);
                        }
                    }
                );
            } else {//否则上传失败,返回提示信息
                alert("上传失败!");
            }
        }
    });
    return false;
}
package com.nowcoder.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
public class MessageController implements CommunityConstant {

    @Autowired
    private MessageService messageService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    //得到当前用户的私信列表
    @RequestMapping(path = "/letter/list", method = RequestMethod.GET)
    public String getLetterList(Model model, Page page) {
        User user = hostHolder.getUser();//当前请求的用户
        // 1.分页信息设置
        page.setLimit(5);//每页显示5条数据
        page.setPath("/letter/list");//导航页的访问路径
        page.setRows(messageService.findConversationCount(user.getId()));//设置数据总行数

        //2.得到当前用户的会话列表集合conversationList
        List<Message> conversationList = messageService.findConversations(
                user.getId(), page.getOffset(), page.getLimit());
        //3.页面显示的额外内容(显示当前用户的未读私信总数;当前用户每个会话的未读私信总数;当前会话的总私信数量)都封装在conversations中
        List<Map<String, Object>> conversations = new ArrayList<>();
        if (conversationList != null) {
                //3.遍历当前用户的会话列表,得到每个会话的最新消息message
            for (Message message : conversationList) {
                Map<String, Object> map = new HashMap<>();
                    //map中存储当前会话的最新消息message
                map.put("conversation", message);
                    //存储当前会话message.getConversationId()的私信总数
                map.put("letterCount", messageService.findLetterCount(message.getConversationId()));
                    //存储当前用户当前会话message.getConversationId()的未读私信总数
                map.put("unreadCount", messageService.findLetterUnreadCount(user.getId(), message.getConversationId()));
                    //在每个会话前显示与当前用户user.getId()私信的人的头像,如果用户为A,头像就显示B(FromId ToId一个为当前用户,一个为与当前用户聊天的人)
                int targetId = user.getId() == message.getFromId() ? message.getToId() : message.getFromId();
                    //得到的targetId即为与当前用户user.getId()聊天的用户的id,得到该目标用户User
                map.put("target", userService.findUserById(targetId));
                conversations.add(map);
            }
        }
        //4.conversations中存储了列表中每个会话消息需要单独显示的内容,保存到请求域中
        model.addAttribute("conversations", conversations);

        //5.查询未读消息总数,需要显示在私信页面上方
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        //6.查询所有未读通知的总数
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);
        //7.返回到私信详情界面letter
        return "/site/letter";
    }

    /**
     * 进入当前用户某个会话conversationId的详情界面
     * @param conversationId    该用户的某个会话
     * @param page
     * @param model
     * @return
     */
    @RequestMapping(path = "/letter/detail/{conversationId}", method = RequestMethod.GET)
    public String getLetterDetail(@PathVariable("conversationId") String conversationId, Page page, Model model) {
        //1.会话详情内的分页信息
        page.setLimit(5);
        page.setPath("/letter/detail/" + conversationId);
        page.setRows(messageService.findLetterCount(conversationId));

        //2.得到当前会话所包含的私信列表
        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        //3.该集合letters中保存每条私信及该私信所对应的发信人的头像和用户名
        List<Map<String, Object>> letters = new ArrayList<>();
        if (letterList != null) {
            //得到每条私信,并保存该私信所对应的用户fromUser
            for (Message message : letterList) {
                Map<String, Object> map = new HashMap<>();
                map.put("letter", message);
                map.put("fromUser", userService.findUserById(message.getFromId()));
                letters.add(map);
            }
        }
        //4.将letters保存到请求域中
        model.addAttribute("letters", letters);
        //5.私信目标
        model.addAttribute("target", getLetterTarget(conversationId));

        //6.只要进入私信详情界面,就将所有未读私信设置已读
        List<Integer> ids = getLetterIds(letterList);//得到未读私信集合
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }
        //7.进入私信详情界面
        return "/site/letter-detail";
    }

    /**
     * 得到该用户在当前私信中的对话用户
     * @param conversationId    传入会话id(一半为当前用户,一半为与当前用户聊天的目标用户target,即需要返回的用户User),例如112_113
     * @return      返回当前私信的对话用户
     */
    private User getLetterTarget(String conversationId) {
        String[] ids = conversationId.split("_");//对会话id进行拆分
        int id0 = Integer.parseInt(ids[0]);//得到用户1
        int id1 = Integer.parseInt(ids[1]);//得到用户2
        if (hostHolder.getUser().getId() == id0) {//如果当前登录用户==id0
            return userService.findUserById(id1);//返回id1对应的用户
        } else {
            return userService.findUserById(id0);
        }
    }

    /**
     * 得到所有未读私信集合
     * @param letterList    所有的私信列表
     * @return
     */
    private List<Integer> getLetterIds(List<Message> letterList) {
        List<Integer> ids = new ArrayList<>();

        if (letterList != null) {
            for (Message message : letterList) {
                if (hostHolder.getUser().getId() == message.getToId() && message.getStatus() == 0) {
                    ids.add(message.getId());
                }
            }
        }
        return ids;
    }

    /**
     * 发送私信处理(表单提交界面)
     * @param toName    私信发送的用户名
     * @param content   发送私信的内容
     * @return
     */
    @RequestMapping(path = "/letter/send", method = RequestMethod.POST)
    @ResponseBody
    public String sendLetter(String toName, String content) {
        Integer.valueOf("abc");
        User target = userService.findUserByName(toName);
        if (target == null) {
            return CommunityUtil.getJSONString(1, "目标用户不存在!");//code=1,表示错误提示代码
        }
        Message message = new Message();
        message.setFromId(hostHolder.getUser().getId());//得到当前用户的id(即为消息发送方)
        message.setToId(target.getId());//即为消息接收方
        if (message.getFromId() < message.getToId()) {//拼接ConversationId(较小的用户id在ConversationId开始)
            message.setConversationId(message.getFromId() + "_" + message.getToId());
        } else {
            message.setConversationId(message.getToId() + "_" + message.getFromId());
        }
        message.setContent(content);
        message.setCreateTime(new Date());
        messageService.addMessage(message);//插入当前信息

        return CommunityUtil.getJSONString(0);//业务逻辑吗code=0表示发送成功
    }

    /**
     * 处理系统通知的展示界面(展示三种主题下的最新通知)点赞/评论/关注
     * @param model     向模板返回数据
     * @return
     */
    @RequestMapping(path = "/notice/list", method = RequestMethod.GET)
    public String getNoticeList(Model model) {
        //1.获取当前用户(查询当前用户的系统通知)
        User user = hostHolder.getUser();
            //系统通知界面显示三种主题的最新通知,因此需要查询三次
        //2.查询评论类通知
        Message message = messageService.findLatestNotice(user.getId(), TOPIC_COMMENT);//查询评论主题的通知: TOPIC_COMMENT
        if (message != null) {
            Map<String, Object> messageVO = new HashMap<>();    //messageVO封装相关数据发送到界面:比如评论的用户信息,时间等,界面进行使用的对象
            messageVO.put("message", message);
             //得到系统通知Message的具体内容content(存储的为转义字符,此处进行反转义(即正常显示),content即存储了通知事件Event的具体属性信息)
            String content = HtmlUtils.htmlUnescape(message.getContent());
            //系统通知content为JSON格式的字符串,此处还原为对象(data中存储)
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            //从data对象中得到触发通知事件的用户Auser
            messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
            //触发通知事件的事件类型(即该事件是作用于帖子/评论/用户?)
            messageVO.put("entityType", data.get("entityType"));
            //事件的具体id
            messageVO.put("entityId", data.get("entityId"));
            //此时为评论通知,即得到帖子的id
            messageVO.put("postId", data.get("postId"));
            //得到评论类通知的总数量
            int count = messageService.findNoticeCount(user.getId(), TOPIC_COMMENT);
            messageVO.put("count", count);
            //得到评论类通知的未读总数
            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_COMMENT);
            messageVO.put("unread", unread);
            //存储评论类通知的信息到model中
            model.addAttribute("commentNotice", messageVO);
        }


        //3.查询点赞类通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_LIKE);//查询点赞主题的通知: TOPIC_LIKE
        if (message != null) {
            Map<String, Object> messageVO = new HashMap<>();//messageVO封装相关数据发送到界面:比如评论的用户信息,时间等,界面进行使用的对象
            messageVO.put("message", message);
            //得到系统通知Message的具体内容content(存储的为转义字符,此处进行反转义(即正常显示),content即存储了通知事件Event的具体属性信息)
            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));
            //点赞的具体帖子
            messageVO.put("postId", data.get("postId"));
            //所有的点赞通知的总数
            int count = messageService.findNoticeCount(user.getId(), TOPIC_LIKE);
            messageVO.put("count", count);
            //所有的未读点赞通知的总数
            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_LIKE);
            messageVO.put("unread", unread);
            model.addAttribute("likeNotice", messageVO);
        }


        //4.查询关注类通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_FOLLOW);//查询关注主题的通知: TOPIC_FOLLOW

        if (message != null) {
            Map<String, Object> messageVO = new HashMap<>();
            messageVO.put("message", message);

            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            //触发关注事件的用户A,用户A关注了当前用户
            messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));
            //关注类通知没有帖子id,只是关注信息,点击此通知连接到关注当前用户的user的主页

            int count = messageService.findNoticeCount(user.getId(), TOPIC_FOLLOW);
            messageVO.put("count", count);

            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_FOLLOW);
            messageVO.put("unread", unread);
            model.addAttribute("followNotice", messageVO);
        }

        //5.查询所有未读私信的总数
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        //6.查询所有未读通知的总数
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);
        //7.跳转到系统通知notice界面,在模板内得到当前请求的信息
        return "/site/notice";
    }

    /**
     * 通过请求路径传入的主题topic访问该主题下的所有通知信息,即跳转到notice-detail界面
     * @param topic     请求路径中传输的主题参数
     * @param page
     * @param model
     * @return
     */
    @RequestMapping(path = "/notice/detail/{topic}", method = RequestMethod.GET)
    public String getNoticeDetail(@PathVariable("topic") String topic, Page page, Model model) {
        //1.得到当前请求的请求用户
        User user = hostHolder.getUser();
        //2.设置分页数据
        page.setLimit(5);//每页显示5条
        page.setPath("/notice/detail/" + topic);//分页导航跳转的路径
        page.setRows(messageService.findNoticeCount(user.getId(), topic));//帖子总数
        //查询topic主题下的所有通知列表集合
        List<Message> noticeList = messageService.findNotices(user.getId(), topic, page.getOffset(), page.getLimit());
        List<Map<String, Object>> noticeVoList = new ArrayList<>();//存储每个通知对应的信息map(比如触发通知事件的用户信息等)
        if (noticeList != null) {
            //遍历每个通知,对noticeVoList进行赋值
            for (Message notice : noticeList) {
                Map<String, Object> map = new HashMap<>();
                // 存储通知信息notice(即为Message)
                map.put("notice", notice);
                // 得到系统通知Message的具体内容content(存储的为转义字符,此处进行反转义(即正常显示),content即存储了通知事件Event的具体属性信息)
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                // 系统通知content为JSON格式的字符串,此处还原为对象(data中存储)
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
                // 存储触发此通知的用户
                map.put("user", userService.findUserById((Integer) data.get("userId")));
                // 触发通知对应的实体类型
                map.put("entityType", data.get("entityType"));
                // 实体类型对应的id
                map.put("entityId", data.get("entityId"));
                // 帖子id(只有评论和点赞有帖子id,关注没有)
                map.put("postId", data.get("postId"));
                // 发送此通知的系统用户(即form_id = 1的用户)
                map.put("fromUser", userService.findUserById(notice.getFromId()));
                //存储到list集合中
                noticeVoList.add(map);
            }
        }
        model.addAttribute("notices", noticeVoList);

        // 访问此页,则所有的通知都设置已读
        List<Integer> ids = getLetterIds(noticeList);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }

        // 访问通知具体内容界面
        return "/site/notice-detail";
    }
}

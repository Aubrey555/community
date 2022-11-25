package com.nowcoder.community.config;

import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration  //该配置类实现Security的授权配置:对当前系统内包含的所有的请求，分配访问权限（普通用户、版主、管理员）。
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {
//继承WebSecurityConfigurerAdapter类,实现如下3个方法:
    //该方法一般用户配置:忽略掉对静态资源的拦截
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/resources/**");//所有静态资源都可访问
    }

    //对于configure(AuthenticationManagerBuilder auth)方法:用于实现登录认证功能(当前项目使用账号密码进行登录/退出认证)
        //当前项目已经实现了登录/退出功能,因此不需要实现此configure()方法,绕过认证功能

    //该方法 实现授权处理逻辑:即对于不同类型用户,给定普通权限/管理权限等
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 授权
        http.authorizeRequests()
                .antMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/discuss/add",
                        "/comment/add/**",
                        "/letter/**",   //私信功能
                        "/notice/**",   //通知功能
                        "/like",        //点赞
                        "/follow",      //关注
                        "/unfollow"     //取消关注
                )
                .hasAnyAuthority(
                        AUTHORITY_USER,
                        AUTHORITY_ADMIN,
                        AUTHORITY_MODERATOR     //对于如上请求路径,登录用户拥有此三个权限都可以进行访问
                )
                .antMatchers(
                        "/discuss/top", //置顶请求
                        "/discuss/wonderful"    //加精请求
                )
                .hasAnyAuthority(
                        AUTHORITY_MODERATOR //只有版主可以使用置顶和加精请求功能
                )
                .antMatchers(
                        "/discuss/delete",  //删除请求
                        "/data/**"      //数据统计请求,只有管理员用户可以访问
                )
                .hasAnyAuthority(
                        AUTHORITY_ADMIN     //只有管理员用户可以使用删除帖子功能
                )
                .anyRequest().permitAll()  //除以上请求外的所有请求对任意用户都可以访问
                .and().csrf().disable();    //不启用CSRF配置,即不会向页面生成token凭证,服务器也不会检查表单/异步请求是否包含了此token

        // 权限不够时的处理
        http.exceptionHandling()
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    // authenticationEntryPoint()配置没有登录时的处理:传入AuthenticationEntryPoint接口
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                        //当前项目存在普通请求/异步请求。当为普通请求时,希望返回html界面;而对于异步请求希望返回JSON格式的字符串
                            //通过得到当前请求的头信息"x-requested-with"值,判断是否为异步请求
                        String xRequestedWith = request.getHeader("x-requested-with");//异常处理时使用过
                        if ("XMLHttpRequest".equals(xRequestedWith)) {//此时表示请求为异步请求(在处理异常时用过)
                            response.setContentType("application/plain;charset=utf-8");//声明返回的数据类型(普通的中文字符)
                            PrintWriter writer = response.getWriter();//获取字符流
                            //想浏览器响应JSON字符串,响应代码为403
                            writer.write(CommunityUtil.getJSONString(403, "你还没有登录哦!"));
                        } else {
                            //当前为普通请求,则重定向到登陆界面
                            response.sendRedirect(request.getContextPath() + "/login");//访问/login请求
                        }
                    }
                })
                .accessDeniedHandler(new AccessDeniedHandler() {
                    // accessDeniedHandler()配置登陆后权限不足的处理:传入AccessDeniedHandler接口
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                        //也是根据请求的类型进行返回。普通请求重定向；异步请求返回JSON格式字符串
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) {
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403, "你没有访问此功能的权限!"));
                        } else {
                            //此时已经登陆,但是权限不足,返回到权限不足的界面。(该路径在homecontroller中进行处理)
                            response.sendRedirect(request.getContextPath() + "/denied");
                        }
                    }
                });
        // Security使用Filter进行权限管理,因此一定会在dispatcherservlet之前进行拦截,处置结束后,不会再进入controller中
        // 并且Security底层默认会拦截名为 /logout请求(即为退出请求),进行退出处理.
        // 覆盖它默认的逻辑,才能执行我们自己的退出代码.
        http.logout().logoutUrl("/securitylogout");//即Security底层默认的拦截请求名为:/logout(也是当前项目的推出请求),对其进行修改即可
    }
}

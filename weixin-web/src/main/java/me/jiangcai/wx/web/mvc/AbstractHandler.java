package me.jiangcai.wx.web.mvc;

import me.jiangcai.wx.WeixinUserService;
import me.jiangcai.wx.couple.WeixinRequestHandlerMapping;
import me.jiangcai.wx.model.PublicAccount;
import me.jiangcai.wx.protocol.Protocol;
import me.jiangcai.wx.protocol.exception.BadAuthAccessException;
import me.jiangcai.wx.web.exception.NoWeixinClientException;
import me.jiangcai.wx.web.flow.RedirectException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author CJ
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractHandler {

    private static final Log log = LogFactory.getLog(AbstractHandler.class);

    protected final String SK_Prefix_OpenID = "_weixin_openId_";
    @Autowired
    protected WeixinUserService weixinUserService;
    @Autowired
    private WeixinRequestHandlerMapping mapping;

    protected <T> T webAuth(NativeWebRequest webRequest, Function<PublicAccount, T> currentAuth
            , Class<T> clazz) {
        // 微信内

        PublicAccount account = mapping.currentPublicAccount();
        if (account == null)
            throw new NoWeixinClientException();

        try {
            HttpSession session = webRequest.getNativeRequest(HttpServletRequest.class).getSession();
            // 如果收到了 code
            // 是否已获得code
            String code = webRequest.getParameter("code");

//            StringBuilder stringBuilder = new StringBuilder();
//            stringBuilder.append("[WXDEBUG]");
//            HttpServletRequest _request = webRequest.getNativeRequest(HttpServletRequest.class);
//            stringBuilder.append(_request.getRequestURL());
//            stringBuilder.append("\n\r").append(code);
//            Enumeration<String> stringEnumeration = _request.getParameterNames();
//            while (stringEnumeration.hasMoreElements()){
//                String name = stringEnumeration.nextElement();
//                stringBuilder.append("\n\r").append(name).append(":").append(_request.getParameter(name));
//            }
//            log.error(stringBuilder.toString());


            if (code != null) {
                log.debug("get  web-auth success for code:" + code);
                String openId = Protocol.forAccount(account).userToken(code, weixinUserService, webRequest);
                if (session != null) {
                    session.setAttribute(SK_Prefix_OpenID + account.getAppID(), openId);
                    // 将code 去掉 再度重定向
                    HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
                    if (request.getMethod().equalsIgnoreCase("get")) {
                        Pattern pattern = Pattern.compile("&code=[0-9a-z]+");
                        String url = getUrl(request);
                        Matcher matcher = pattern.matcher(url);
                        String newUrl = matcher.replaceFirst("");
                        log.debug("got code,store openId in to session, redirect from " + url + " to " + newUrl);
                        throw new RedirectException(newUrl);
                    }
                }
                return weixinUserService.userInfo(account, openId, clazz, webRequest);
            }
            // 先看下是否可以直接完成
            T endValue = currentAuth.apply(account);
            if (endValue != null)
                return endValue;

            if (session != null) {
                String openId = (String) session.getAttribute(SK_Prefix_OpenID + account.getAppID());
                // 可能会丢出TOKEN 无效，这个时候收到的code可能已更新，必须确保给予机会让他获取更新
                if (!StringUtils.isEmpty(openId) && clazz == String.class) {
                    endValue = weixinUserService.userInfo(account, openId, clazz, webRequest);
                    if (endValue != null)
                        return endValue;
                }
            }

        } catch (BadAuthAccessException ex) {
            //
            log.debug("User's Auth Token is offline or bad scope", ex);
        }

        // 这个请求必须为一个get请求
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        if (!request.getMethod().equalsIgnoreCase("get"))
            throw new IllegalArgumentException("can not get OpenId in no-get http.");

        //记录我们的url
        String url = getUrl(request);
        String newUrl = Protocol.forAccount(account).redirectUrl(url, clazz);

        throw new RedirectException(newUrl);
    }

    private String getUrl(HttpServletRequest request) {
        final String url = request.getRequestURL().toString();

        if (log.isTraceEnabled()) {
            final Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String header = headerNames.nextElement();
                log.trace("[" + header + "]:" + request.getHeader(header));
            }
        }

        if (request.isSecure() ||
                // https://www.w3.org/TR/upgrade-insecure-requests/
                request.getHeader("Upgrade-Insecure-Requests") != null) {
            if (url.startsWith("https://"))
                return url;
            if (url.startsWith("http://")) {
                String newUrl = url.substring(4);
                return "https" + newUrl;
            }
        }
        return url;
    }

    protected String currentOpenId(NativeWebRequest webRequest, PublicAccount account) {
        HttpSession session = webRequest.getNativeRequest(HttpServletRequest.class).getSession();
        if (session != null)
            return (String) session.getAttribute(SK_Prefix_OpenID + account.getAppID());
        return null;
    }
}

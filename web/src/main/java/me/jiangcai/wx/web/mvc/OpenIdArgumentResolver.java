package me.jiangcai.wx.web.mvc;

import me.jiangcai.wx.OpenId;
import me.jiangcai.wx.WeixinUserService;
import me.jiangcai.wx.couple.WeixinRequestHandlerMapping;
import me.jiangcai.wx.model.PublicAccount;
import me.jiangcai.wx.protocol.Protocol;
import me.jiangcai.wx.web.flow.RedirectException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author CJ
 */
@Component
public class OpenIdArgumentResolver implements HandlerMethodArgumentResolver {

    private static final Log log = LogFactory.getLog(OpenIdArgumentResolver.class);

    @Autowired
    private WeixinRequestHandlerMapping mapping;
    private WeixinUserService weixinUserService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(OpenId.class) != null;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer
            , NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        // 微信内

        PublicAccount account = mapping.currentPublicAccount();
        if (account == null)
            throw new IllegalArgumentException("OpenId only work in weixin.");

        // 先看下是否可以直接完成
        HttpSession session = webRequest.getNativeRequest(HttpServletRequest.class).getSession();
        final String SK_Prefix_OpenID = "_weixin_openId_";
        if (session != null) {
            String openId = (String) session.getAttribute(SK_Prefix_OpenID + account.getAppID());
            if (!StringUtils.isEmpty(openId))
                return openId;
        }

        // 是否已获得code
        String code = webRequest.getParameter("code");
        if (code != null) {
            log.debug("get  web-auth success for code:" + code);
            String openId = Protocol.forAccount(account).userToken(code, weixinUserService);
            if (session != null) {
                session.setAttribute(SK_Prefix_OpenID + account.getAppID(), openId);
            }
            return openId;
        }

        // 这个请求必须为一个get请求
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        if (!request.getMethod().equalsIgnoreCase("get"))
            throw new IllegalArgumentException("can not get OpenId in no-get http.");

        //记录我们的url
        String url = request.getRequestURL().toString();
        String newUrl = Protocol.forAccount(account).baseRedirectUrl(url);

        throw new RedirectException(newUrl);
    }
}

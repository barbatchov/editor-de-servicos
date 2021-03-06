package br.gov.servicos.editor.security;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @NonFinal
    RequestCache requestCache;

    String defaultRedirectUrl;
    String loginUrl;

    public CustomLoginSuccessHandler(String defaultRedirectUrl, String loginUrl) {
        this.defaultRedirectUrl = defaultRedirectUrl;
        this.loginUrl = loginUrl;
        requestCache = new HttpSessionRequestCache();
        super.setRequestCache(requestCache);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        SavedRequest savedRequest = requestCache.getRequest(request, response);

        if (savedRequest == null || savedRequest.getRedirectUrl().contains(loginUrl)) {
            getRedirectStrategy().sendRedirect(request, response, defaultRedirectUrl);
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }

    @Override
    public void setRequestCache(RequestCache requestCache) {
        this.requestCache = requestCache;
        super.setRequestCache(requestCache);
    }
}

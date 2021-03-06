/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingsphere.shardingui.security;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import io.shardingsphere.shardingui.web.response.ResponseResultUtil;
import lombok.Setter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication filter.
 *
 * @author chenqingyang
 */
public final class AuthenticationFilter implements Filter {
    
    private static final String LOGIN_URI = "/api/login";
    
    @Setter
    private UserAuthenticationService userAuthenticationService;
    
    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
    }
    
    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        if (LOGIN_URI.equals(httpRequest.getRequestURI())) {
            handleLogin(httpRequest, httpResponse);
        } else {
            String accessToken = httpRequest.getHeader("Access-Token");
            if (!Strings.isNullOrEmpty(accessToken) && accessToken.equals(userAuthenticationService.getToken())) {
                filterChain.doFilter(httpRequest, httpResponse);
            } else {
                respondWithUnauthorized(httpResponse);
            }
        }
    }
    
    @Override
    public void destroy() {
    }
    
    private void handleLogin(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse) {
        try {
            UserAccount user = new Gson().fromJson(httpRequest.getReader(), UserAccount.class);
            if (userAuthenticationService.checkUser(user)) {
                httpResponse.setContentType("application/json");
                httpResponse.setCharacterEncoding("UTF-8");
                Map<String, Object> result = new HashMap<>();
                result.put("username", userAuthenticationService.getUsername());
                result.put("accessToken", userAuthenticationService.getToken());
                httpResponse.getWriter().write(new Gson().toJson(ResponseResultUtil.build(result)));
            } else {
                respondWithUnauthorized(httpResponse);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
    private void respondWithUnauthorized(final HttpServletResponse httpResponse) throws IOException {
        httpResponse.setContentType("application/json");
        httpResponse.setCharacterEncoding("UTF-8");
        httpResponse.getWriter().write(new Gson().toJson(ResponseResultUtil.handleUnauthorizedException("Unauthorized.")));
    }
}

package com.pnk.record_management.configuration;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


/*
 * Authorization among microservices
 * */
@Slf4j
public class AuthenticationRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        ServletRequestAttributes servletRequestAttributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        var authorizationTokenHeader = servletRequestAttributes.getRequest().getHeader("Authorization");

        log.info(">> apply::authorizationTokenHeader: {}", authorizationTokenHeader);

        if (StringUtils.hasText(authorizationTokenHeader))
            requestTemplate.header("Authorization", authorizationTokenHeader);
    }
}

package com.aicontent.marketing.auth.security;

import com.aicontent.marketing.auth.util.JwtUtil;
import com.aicontent.marketing.user.entity.SysUser;
import com.aicontent.marketing.user.service.SysUserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final SysUserService sysUserService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, SysUserService sysUserService) {
        this.jwtUtil = jwtUtil;
        this.sysUserService = sysUserService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(token);
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void authenticate(String token) {
        try {
            Claims claims = jwtUtil.parseToken(token);
            Long userId = jwtUtil.getUserId(claims);
            SysUser user = sysUserService.getById(userId);
            if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
                return;
            }
            LoginUser loginUser = new LoginUser(user);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    loginUser,
                    null,
                    loginUser.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ignored) {
            SecurityContextHolder.clearContext();
        }
    }
}

package com.aicontent.marketing.auth.dto;

import com.aicontent.marketing.user.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;

    private UserVO user;
}

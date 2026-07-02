package com.aicontent.marketing.user.vo;

import com.aicontent.marketing.user.entity.SysUser;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private String role;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static UserVO from(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        return vo;
    }
}

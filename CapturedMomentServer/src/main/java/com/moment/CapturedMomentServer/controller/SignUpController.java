package com.moment.CapturedMomentServer.controller;


import com.moment.CapturedMomentServer.config.security.JwtTokenProvider;
import com.moment.CapturedMomentServer.domain.User;
import com.moment.CapturedMomentServer.domain.UserRequestDto;
import com.moment.CapturedMomentServer.repository.UserRepository;
import com.moment.CapturedMomentServer.response.JSONResponse;
import com.moment.CapturedMomentServer.service.SignUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/* 김예진 2021-08-08, 장현애 08-15 */

@Controller
@RestController
@RequiredArgsConstructor
public class SignUpController {
    private final SignUpService signUpService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/user/signup")
    public JSONResponse<User> create(@RequestBody UserRequestDto userDto){
        userDto.setRoles(Collections.singletonList("ROLE_USER")); // 최초 가입시 USER로 설정
        User user = signUpService.signUp(userDto);                // 서비스로 user 정보 전달

        JSONResponse<User> response = new JSONResponse<>();
        response.setStatusCode(200);
        response.setMessage("회원가입 성공");
        response.setData(user);
        return response;
    }

    // 로그인
    @PostMapping("/user/signin")
    public JSONResponse<String> login(@RequestBody UserRequestDto.LoginDto requestDto) {
        User user = signUpService.loginCheck(requestDto);

        JSONResponse<String> response = new JSONResponse<>();
        if (user != null){
            response.setStatusCode(200);
            response.setMessage("로그인 성공");
            response.setData(jwtTokenProvider.createToken(user.getEmail(), user.getRoles()));
            return response;
        }
        else {
            response.setStatusCode(401);
            response.setMessage("로그인 실패");
            response.setData(null);
            return response;
        }
    }
}

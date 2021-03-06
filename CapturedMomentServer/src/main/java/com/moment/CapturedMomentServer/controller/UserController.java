package com.moment.CapturedMomentServer.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.moment.CapturedMomentServer.domain.VerifyCode;
import com.moment.CapturedMomentServer.domain.VerifyCodeDto;
import com.moment.CapturedMomentServer.service.MailService;
import com.moment.CapturedMomentServer.service.VerifyCodeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import com.moment.CapturedMomentServer.config.security.JwtTokenProvider;
import com.moment.CapturedMomentServer.domain.User;
import com.moment.CapturedMomentServer.domain.UserRequestDto;
import com.moment.CapturedMomentServer.repository.UserRepository;
import com.moment.CapturedMomentServer.service.UserService;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.HashMap;

/* 김예진 2021-08-08, 장현애 08-15 */

@CrossOrigin
@Controller
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final MailService mailService;
    private final VerifyCodeService verifyCodeService;

    @ApiOperation(value = "회원가입",
            httpMethod = "POST",
            response = ResponseEntity.class,
            notes = "회원 정보를 받아와 회원을 등록한다."
    )
    @ApiResponses({
            @ApiResponse(code = 200, message = "회원가입 성공"),
            @ApiResponse(code = 409, message = "중복된 이메일")
    })
    @PostMapping(value = "/user/signup",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<HashMap> create(@RequestPart(value = "user") UserRequestDto userDto, @RequestPart(value = "image", required = false) MultipartFile image){

        userDto.setRoles(Collections.singletonList("ROLE_USER"));// 최초 가입시 USER로 설정
        User user = userService.signUp(userDto, image);                  // 서비스로 user 정보 전달

        HashMap<String, Object> responseMap = new HashMap<>();
        if(user != null){
            responseMap.put("status", 200);
            responseMap.put("message", "회원가입 성공");
            return new ResponseEntity<HashMap> (responseMap, HttpStatus.OK);
        }
        else{
            responseMap.put("status", 409);
            responseMap.put("message", "중복된 이메일");
            return new ResponseEntity<HashMap> (responseMap, HttpStatus.CONFLICT);
        }

    }

    //메일 인증 번호 전송
    @PostMapping(value = "/user/certification")
    public ResponseEntity<HashMap> mailCheck(@RequestBody VerifyCodeDto codeDto){

        VerifyCode verifyCode = verifyCodeService.saveCode(codeDto);

        boolean success = mailService.checkEmail(verifyCode);

        HashMap<String, Object> responseMap = new HashMap<>();
        if(success) {
            responseMap.put("status", 200);
            responseMap.put("message", "메일 발송 성공");
            responseMap.put("code" , verifyCode.getCode());
            return new ResponseEntity<HashMap> (responseMap, HttpStatus.OK);
        }
        else{
            responseMap.put("status", 500);
            responseMap.put("message", "메일 발송 실패");
            return new ResponseEntity<HashMap> (responseMap, HttpStatus.CONFLICT);
        }
    }

    //이메일 인증 코드 맞는지 확인
    @GetMapping(value = "/user/check")
    public ResponseEntity<HashMap> mailCheck(@RequestBody VerifyCode verifyCode){

        boolean check = verifyCodeService.check(verifyCode);
        HashMap<String, Object> responseMap = new HashMap<>();

        if(check){
            verifyCodeService.deleteByEmail(verifyCode.getEmail());
            responseMap.put("status", 200);
            responseMap.put("message", "인증 성공");
            return new ResponseEntity<HashMap> (responseMap, HttpStatus.OK);
        }
        else{
            responseMap.put("status", 401);
            responseMap.put("message", "잘못된 인증 코드");
            return new ResponseEntity<HashMap> (responseMap, HttpStatus.CONFLICT);
        }
    }

    @ApiOperation(value = "로그인",
            httpMethod = "POST",
            response = ResponseEntity.class,
            notes = "회원의 이메일과 패스워드로 로그인을 시도한 후, 성공 시 JWT 토큰을 반환한다."
    )
    @ApiResponses({
            @ApiResponse(code = 200, message = "로그인 성공"),
            @ApiResponse(code = 401, message = "이메일 또는 비밀번호 오류")
    })
    @PostMapping("/user/signin")
    public ResponseEntity<HashMap> login(@RequestBody UserRequestDto.LoginDto requestDto) {

        User user = userService.signIn(requestDto);

        HashMap<String, Object> responseMap = new HashMap<>();
        if (user != null){
            responseMap.put("status", 200);
            responseMap.put("message", "로그인 성공");
            responseMap.put("token", jwtTokenProvider.createToken(user.getEmail(), user.getRoles()));
            return new ResponseEntity<HashMap> (responseMap, HttpStatus.OK);
        }
        else{
            responseMap.put("status", 401);
            responseMap.put("message", "이메일 또는 비밀번호 오류");
            return new ResponseEntity<HashMap> (responseMap, HttpStatus.UNAUTHORIZED);
        }
    }

    //비밀번호 재설정 링크 전송
    @GetMapping(value = "/user/password/find")
    public ResponseEntity<HashMap> findPasswordUrl(@RequestBody VerifyCodeDto codeDto) {

        VerifyCode verifyCode = verifyCodeService.saveCode(codeDto);

        boolean success = mailService.changePassword(verifyCode);

        HashMap<String, Object> responseMap = new HashMap<>();
        if (success) {
            responseMap.put("status", 200);
            responseMap.put("message", "메일 발송 성공");
            responseMap.put("code", verifyCode.getCode());
            return new ResponseEntity<HashMap>(responseMap, HttpStatus.OK);
        } else {
            responseMap.put("status", 500);
            responseMap.put("message", "메일 발송 실패");
            return new ResponseEntity<HashMap>(responseMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //비밀번호 바꾸기
    @PostMapping(value = "/user/password/{code}")
    public ResponseEntity<HashMap> changePassword(@RequestBody UserRequestDto.PasswordDto requestDto, @PathVariable String code){

        System.out.println(code);
        VerifyCode verifyCode = verifyCodeService.findByCode(code);

        HashMap<String, Object> responseMap = new HashMap<>();

        if(verifyCode == null){
            responseMap.put("status", 401);
            responseMap.put("message", "만료되었거나 잘못된 링크");
            return new ResponseEntity<HashMap>(responseMap, HttpStatus.CONFLICT);
        }


        userService.updatePassword(verifyCode.getEmail(), requestDto);

        if(true) {
            verifyCodeService.deleteByEmail(verifyCode.getEmail());
            responseMap.put("status", 200);
            responseMap.put("message", "비밀번호 변경 성공");
            return new ResponseEntity<HashMap> (responseMap, HttpStatus.OK);
        }
        else{
            responseMap.put("status", 500);
            responseMap.put("message", "비밀번호 변경 실패");
            return new ResponseEntity<HashMap> (responseMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "마이페이지 프로필 정보 조회",
            httpMethod = "GET",
            response = ResponseEntity.class,
            notes = "헤더의 JWT Token으로 사용자를 검색하고 프로필 정보를 조회한다."
    )
    @ApiResponses({
            @ApiResponse(code = 200, message = "프로필 조회 성공")
    })
    @ApiImplicitParam(name = "X-AUTH-TOKEN", value = "JWT 토큰", required = true, dataType = "String", paramType = "header")
    @GetMapping("/user/mypage")
    public ResponseEntity<HashMap> readProfile(@RequestHeader("X-AUTH-TOKEN") String token) { // "X-AUTH-TOKEN" 헤더에서 토큰 받아오기

        String email = jwtTokenProvider.getUserEmail(token);                                    // 토큰에서 유저 이메일 추출

        HashMap<String, Object> responseMap = new HashMap<>();

        User user = userRepository.findByEmail(email).orElseGet(()->null);  // 이메일로 유저 검색

        //서버 오류를 할 필요가 있을까?
        if(user == null){

            responseMap.put("status", 500);
            responseMap.put("message", "해당 아이디의 사용자를 찾을 수 없음");
            return new ResponseEntity<HashMap>(responseMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }
                /*orElseThrow(
                () -> new IllegalArgumentException("아이디가 존재하지 않습니다.")
        );*/
        else {
            UserRequestDto.ProfileDto profile = new UserRequestDto.ProfileDto(user);                // 프로필 정보 전달
            ObjectMapper mapper = new ObjectMapper();
            try {
                responseMap.put("profile", profile);
            }
            catch (Exception e){
                responseMap.put("status", "500");
            }
            responseMap.put("status", 200);
            responseMap.put("message", "프로필 조회 성공");

            return new ResponseEntity<HashMap>(responseMap, HttpStatus.OK);
        }
    }

    @ApiOperation(value = "마이페이지 프로필 정보 수정",
            httpMethod = "PUT",
            response = ResponseEntity.class,
            notes = "JWT Token에 저장된 유저의 프로필 정보를 수정한다."
    )
    @ApiResponses({
            @ApiResponse(code = 200, message = "프로필 수정 성공")
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-AUTH-TOKEN", value = "JWT 토큰", required = true, dataType = "String", paramType = "header"),
    })
    @PutMapping(value = "/user/mypage", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<HashMap> updateProfile(@RequestHeader("X-AUTH-TOKEN") String token,
                                                 @RequestPart(value="profile") UserRequestDto.ProfileDto requestDto,
                                                 @RequestPart(value = "image") MultipartFile image) {
        HashMap<String, Object> responseMap = new HashMap<>();

        responseMap.put("status", 200);
        responseMap.put("message", "프로필 업데이트 성공");
        responseMap.put("data", userService.updateProfile(jwtTokenProvider.getUserEmail(token), requestDto, image));

        return new ResponseEntity<HashMap>(responseMap, HttpStatus.OK);
    }
}

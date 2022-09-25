package com.dayone.web;

import com.dayone.model.Auth;
import com.dayone.persist.entity.MemberEntity;
import com.dayone.security.TokenProvider;
import com.dayone.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final TokenProvider tokenProvider;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Auth.SignUp request) {

        MemberEntity result = this.memberService.register(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody Auth.SignIn request) {

        MemberEntity member = this.memberService.authenticate(request);
        String token = tokenProvider.generateToken(member.getUsername(), member.getRoles());

        log.info("user login -> " + request.getUsername());

        return ResponseEntity.ok(token);
    }
}

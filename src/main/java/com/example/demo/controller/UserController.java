package com.example.demo.controller;

import com.example.demo.model.dto.CreateUserReq;
import com.example.demo.model.dto.CreateUserRes;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/create")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserReq req) {
        var ua = userService.createUser(req.getUserId(), BigDecimal.valueOf(req.getInitialBalance()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateUserRes(ua.getUserId(), ua.getBalance()));
    }

    @GetMapping("/{userId}/balance")
    public CreateUserRes getBalance(@PathVariable String userId) {
        return new CreateUserRes(userId, userService.getBalance(userId));
    }
}

package com.example.demo.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.ActivityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class ActivityController {
  private final ActivityService activityService;

  @GetMapping
  public Page<com.example.demo.entity.ActivityLog> list(
      @AuthenticationPrincipal com.example.demo.entity.User me,
      @RequestParam(required=false) String action,
      @RequestParam(defaultValue="0") int page,
      @RequestParam(defaultValue="20") int size) {
    return activityService.listMine(me.getUserId(), action, PageRequest.of(page, size));
  }
}


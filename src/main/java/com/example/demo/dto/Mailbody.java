package com.example.demo.dto;

import lombok.Builder;

@Builder

public record Mailbody(String to, String subject, String text) {

}

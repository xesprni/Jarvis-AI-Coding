package com.qihoo.finance.lowcode.aiquestion.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class Token {
    private final TokenType type;
    private final String text;
}

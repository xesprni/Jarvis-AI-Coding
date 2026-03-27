package com.qihoo.finance.lowcode.settings.ui;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class Option<T> {

    private final T value;
    private final String title;

    @Override
    public String toString() {
        return title;
    }
}

package com.qihoo.finance.lowcode.aiquestion.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

@Getter
@Setter
public class StreamData<T> {

    private static final int MAX_WAIT_CYCLE = 600;

    private volatile int index = -1;
    private volatile boolean done;
    private volatile T data;

    public T getData() {
        if (index == -1) {
            waitData();
        }
        return data;
    }

    public synchronized void setData(T data) {
        this.data = data;
        this.index++;
    }

    @SneakyThrows
    public T waitData() {
        if (done) {
            return data;
        }
        int curIndex = index;
        int waitCycle = 0;
        do {
            waitCycle++;
            Thread.sleep(100L);
        } while (curIndex == index && waitCycle < MAX_WAIT_CYCLE && !done);
        return data;
    }
}

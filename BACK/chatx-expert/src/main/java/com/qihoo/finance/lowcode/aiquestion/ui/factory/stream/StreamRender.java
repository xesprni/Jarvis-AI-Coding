package com.qihoo.finance.lowcode.aiquestion.ui.factory.stream;

public interface StreamRender {

    /**
     * 渲染组件
     */
    void render();

    /**
     * 刷新渲染
     * @param question
     */
    void flushRender(String question);

    /**
     * 停止渲染
     */
    default void stopRender() {};

    /**
     * 恢复渲染
     */
    default void resumeRender() {};

    /**
     * 重绘
     */
    void repaint();
}

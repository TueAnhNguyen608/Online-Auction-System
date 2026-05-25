package com.auction.ui.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Xử lý thông báo chung cho toàn hệ thống.
 * Tất cả các nhóm đều dùng class này để gửi/nhận thông báo.
 */
public class NotificationManager {

    private static final List<Consumer<String>> listeners = new ArrayList<>();

    /** Đăng ký nhận thông báo */
    public static void addListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    /** Hủy đăng ký nhận thông báo */
    public static void removeListener(Consumer<String> listener) {
        listeners.remove(listener);
    }

    /** Gửi thông báo đến tất cả listener đang đăng ký */
    public static void notify(String message) {
        for (Consumer<String> listener : listeners) {
            listener.accept(message);
        }
    }
}
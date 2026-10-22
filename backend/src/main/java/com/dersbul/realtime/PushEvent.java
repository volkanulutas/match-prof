package com.dersbul.realtime;

import java.util.Map;

/** Bir kullanıcıya WebSocket üzerinden iletilecek olay. Transaction commit edildikten sonra gönderilir. */
public record PushEvent(Long userId, Map<String, Object> payload) {}

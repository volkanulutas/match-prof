package com.dersbul.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.Map;
import java.util.Optional;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final ChatSocketHandler handler;
    private final WsTicketService tickets;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws")
            .addInterceptors(new HandshakeInterceptor() {
                @Override
                public boolean beforeHandshake(ServerHttpRequest req, ServerHttpResponse res,
                                               WebSocketHandler wsHandler, Map<String, Object> attrs) {
                    if (req instanceof ServletServerHttpRequest s) {
                        Optional<Long> uid = tickets.consume(s.getServletRequest().getParameter("ticket"));
                        if (uid.isPresent()) { attrs.put("userId", uid.get()); return true; }
                    }
                    res.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return false;
                }
                @Override
                public void afterHandshake(ServerHttpRequest req, ServerHttpResponse res, WebSocketHandler h, Exception ex) {}
            })
            // Kimlik doğrulaması tek kullanımlık biletle yapıldığı için (bilet yalnızca giriş yapmış
            // JavaScript tarafından alınabilir) Origin kısıtı gerekmiyor; Render gibi proxy arkasında da sorunsuz çalışır.
            .setAllowedOriginPatterns("*");
    }
}

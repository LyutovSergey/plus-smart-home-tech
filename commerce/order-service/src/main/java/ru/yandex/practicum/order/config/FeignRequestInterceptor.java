package ru.yandex.practicum.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FeignRequestInterceptor implements RequestInterceptor {

	@Override
	public void apply(RequestTemplate template) {
		String requestId = MDC.get("requestId");
		if (requestId == null) {
			requestId = UUID.randomUUID().toString();
		}
		template.header("X-Request-Id", requestId);
		template.header("X-Source-Service", "order-service");
	}
}
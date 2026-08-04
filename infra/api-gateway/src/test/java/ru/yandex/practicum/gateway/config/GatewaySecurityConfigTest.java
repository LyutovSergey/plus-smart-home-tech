package ru.yandex.practicum.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@WebFluxTest
@TestPropertySource(properties = {
		"spring.cloud.config.enabled=false",
		"spring.config.import=optional:configserver:"
})
class GatewaySecurityConfigTest {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void catalogGet_isPublic() {
		webTestClient.get().uri("/api/products").exchange().expectStatus().isOk();
	}

	@Test
	void orderCreate_withoutCredentials_isUnauthorized() {
		webTestClient.post().uri("/api/orders").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void orderCreate_withUserCredentials_passes() {
		webTestClient.post().uri("/api/orders")
				.header("Authorization", basic("ivan", "ivan"))
				.exchange().expectStatus().isOk();
	}

	@Test
	void productWrite_withUserCredentials_isForbidden() {
		webTestClient.patch().uri("/api/products/10")
				.header("Authorization", basic("ivan", "ivan"))
				.exchange().expectStatus().isForbidden();
	}

	@Test
	void productWrite_withAdminCredentials_passes() {
		webTestClient.patch().uri("/api/products/10")
				.header("Authorization", basic("anna", "anna"))
				.exchange().expectStatus().isOk();
	}

	@Test
	void allOrders_withUserCredentials_isForbidden() {
		webTestClient.get().uri("/api/orders")
				.header("Authorization", basic("ivan", "ivan"))
				.exchange().expectStatus().isForbidden();
	}

	@Test
	void allOrders_withAdminCredentials_passes() {
		webTestClient.get().uri("/api/orders")
				.header("Authorization", basic("anna", "anna"))
				.exchange().expectStatus().isOk();
	}

	@Test
	void unknownRoute_withAdminCredentials_isForbidden() {
		webTestClient.get().uri("/api/unknown")
				.header("Authorization", basic("anna", "anna"))
				.exchange().expectStatus().isForbidden();
	}

	@Test
	void preflight_isPublic() {
		webTestClient.options()
				.uri("/api/orders")
				.exchange()
				.expectStatus().isOk();
	}

	private String basic(String username, String password) {
		return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
	}

	@Configuration
	@Import(SecurityConfig.class)
	static class TestConfig {
		@Bean
		RouterFunction<ServerResponse> testRoutes() {
			return route()
					.GET("/api/products", request -> ServerResponse.ok().build())
					.GET("/api/products/10", request -> ServerResponse.ok().build())
					.PATCH("/api/products/10", request -> ServerResponse.ok().build())
					.POST("/api/orders", request -> ServerResponse.ok().build())
					.GET("/api/orders", request -> ServerResponse.ok().build())
					.OPTIONS("/api/orders", request -> ServerResponse.ok().build())
					.build();
		}
	}
}
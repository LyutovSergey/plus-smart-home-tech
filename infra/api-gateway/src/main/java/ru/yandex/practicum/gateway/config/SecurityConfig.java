package ru.yandex.practicum.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(SecurityConfig.SecurityProperties.class)
@Slf4j
public class SecurityConfig {

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		return http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.authorizeExchange(exchanges -> exchanges
						.pathMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
						.pathMatchers(org.springframework.http.HttpMethod.GET, "/api/products/**").permitAll()
						.pathMatchers(org.springframework.http.HttpMethod.GET, "/api/categories/**").permitAll()
						.pathMatchers(org.springframework.http.HttpMethod.GET, "/api/inventory/**").permitAll()
						.pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

						// GET /api/orders — список всех заказов (только ADMIN)
						.pathMatchers(org.springframework.http.HttpMethod.GET, "/api/orders").hasRole("ADMIN")

						// POST /api/orders — создание заказа (USER или ADMIN)
						.pathMatchers(org.springframework.http.HttpMethod.POST, "/api/orders").hasAnyRole("USER", "ADMIN")

						// GET /api/orders/by-email — поиск
						.pathMatchers(org.springframework.http.HttpMethod.GET, "/api/orders/by-email").hasAnyRole("USER", "ADMIN")

						// GET /api/orders/{id} — получить заказ
						.pathMatchers(org.springframework.http.HttpMethod.GET, "/api/orders/{id}").hasAnyRole("USER", "ADMIN")

						// Административные маршруты
						.pathMatchers(org.springframework.http.HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
						.pathMatchers(org.springframework.http.HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
						.pathMatchers(org.springframework.http.HttpMethod.PATCH, "/api/products/**").hasRole("ADMIN")
						.pathMatchers(org.springframework.http.HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
						.pathMatchers(org.springframework.http.HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
						.pathMatchers(org.springframework.http.HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
						.pathMatchers(org.springframework.http.HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")
						.pathMatchers(org.springframework.http.HttpMethod.POST, "/api/inventory/**").hasRole("ADMIN")
						.pathMatchers(org.springframework.http.HttpMethod.PUT, "/api/inventory/**").hasRole("ADMIN")
						.pathMatchers(org.springframework.http.HttpMethod.DELETE, "/api/inventory/**").hasRole("ADMIN")
						.pathMatchers("/api/inventory/reserve").hasAnyRole("USER", "ADMIN")
						.pathMatchers("/api/inventory/release").hasAnyRole("USER", "ADMIN")

						.anyExchange().denyAll()
				)
				.httpBasic(withDefaults())
				.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public MapReactiveUserDetailsService userDetailsService(PasswordEncoder passwordEncoder,
															SecurityProperties securityProperties) {
		List<UserDetails> users = securityProperties.getUsers().stream()
				.map(u -> {
					log.info("Создаём пользователя: username={}, roles={}", u.getUsername(), u.getRoles());
					return User.builder()
							.username(u.getUsername())
							.password(passwordEncoder.encode(u.getPassword()))
							.roles(u.getRoles().toArray(new String[0]))
							.build();
				})
				.toList();
		return new MapReactiveUserDetailsService(users);
	}

	@ConfigurationProperties(prefix = "app.security")
	public static class SecurityProperties {
		private List<UserConfig> users;

		public List<UserConfig> getUsers() { return users; }
		public void setUsers(List<UserConfig> users) { this.users = users; }

		public static class UserConfig {
			private String username;
			private String password;
			private List<String> roles;

			public String getUsername() { return username; }
			public void setUsername(String username) { this.username = username; }
			public String getPassword() { return password; }
			public void setPassword(String password) { this.password = password; }
			public List<String> getRoles() { return roles; }
			public void setRoles(List<String> roles) { this.roles = roles; }
		}
	}
}
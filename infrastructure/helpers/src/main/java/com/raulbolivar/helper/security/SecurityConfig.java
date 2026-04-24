package com.raulbolivar.helper.security;

import com.raulbolivar.helper.filters.RequiredRequestHeadersWebFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableReactiveMethodSecurity
@EnableWebFluxSecurity
public class SecurityConfig {

    private final RequiredRequestHeadersWebFilter requiredRequestHeadersWebFilter;

    @Value("${app-security.mock-bearer-token:mock-token}")
    private String mockBearerToken;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(mockAuthenticationManager());
        authenticationWebFilter.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance());
        authenticationWebFilter.setRequiresAuthenticationMatcher(ServerWebExchangeMatchers.anyExchange());

        authenticationWebFilter.setAuthenticationFailureHandler((webFilterExchange, exception) ->
                requiredRequestHeadersWebFilter.writeError(
                        webFilterExchange.getExchange(),
                        HttpStatus.UNAUTHORIZED,
                        exception.getMessage()
                ));

        authenticationWebFilter.setServerAuthenticationConverter(exchange -> {

            String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authorization == null || authorization.isBlank()) {
                return Mono.error(new BadCredentialsException("Missing Authorization header"));
            }

            if (!authorization.startsWith("Bearer ")) {
                return Mono.error(new BadCredentialsException("Authorization header must use Bearer token"));
            }

            String token = authorization.substring("Bearer ".length()).trim();
            if (token.isBlank()) {
                return Mono.error(new BadCredentialsException("Bearer token is required"));
            }

            return Mono.just(new UsernamePasswordAuthenticationToken("mock-user", token));
        });

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .addFilterAt(requiredRequestHeadersWebFilter, SecurityWebFiltersOrder.FIRST)
                .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((exchange, ex) ->
                        requiredRequestHeadersWebFilter.writeError(exchange, HttpStatus.UNAUTHORIZED, ex.getMessage())))
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().authenticated()
                ).build();
    }

    @Bean
    public ReactiveAuthenticationManager mockAuthenticationManager() {
        return authentication -> {

            String token = authentication.getCredentials() != null ? authentication.getCredentials().toString() : "";
            if (!mockBearerToken.equals(token)) {
                return Mono.error(new BadCredentialsException("Invalid bearer token"));
            }

            return Mono.just(new UsernamePasswordAuthenticationToken(
                    "mock-user",
                    token,
                    List.of(new SimpleGrantedAuthority("ROLE_API"))
            ));
        };
    }
}
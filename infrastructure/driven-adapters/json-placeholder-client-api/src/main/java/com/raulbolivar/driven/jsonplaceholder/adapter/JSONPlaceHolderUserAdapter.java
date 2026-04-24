package com.raulbolivar.driven.jsonplaceholder.adapter;

import com.raulbolivar.driven.jsonplaceholder.dto.UserDto;
import com.raulbolivar.driven.jsonplaceholder.mapper.JSONPlaceHolderMapper;
import com.raulbolivar.model.user.User;
import com.raulbolivar.ports.IJSONPlaceHolderUserGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class JSONPlaceHolderUserAdapter implements IJSONPlaceHolderUserGateway {

    private final WebClient webClient;
    private final JSONPlaceHolderMapper mapper;

    public JSONPlaceHolderUserAdapter(
            @Qualifier("jsonPlaceHolderUserWebClient") WebClient webClient,
            JSONPlaceHolderMapper mapper
    ) {
        this.webClient = webClient;
        this.mapper = mapper;
    }

    // =========================================================
    // 📌 GET ALL POSTS
    // =========================================================
    @Override
    public Flux<User> getUsers() {
        return webClient.get()
                .uri("/users")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(UserDto.class)
                .map(mapper::toDomainUser)
                .doOnSubscribe(s -> log.info("Calling JSONPlaceholder: GET /posts"))
                .doOnNext(p -> log.debug("User received: {}", p.id()))
                .doOnError(e -> log.error("Error getting Users", e));
    }

    // =========================================================
    // 📌 GET User BY ID
    // =========================================================
    @Override
    public Mono<User> getUserById(Long id) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{id}")
                        .build(id))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(UserDto.class)
                .map(mapper::toDomainUser)
                .doOnSubscribe(s -> log.info("Calling JSONPlaceholder: GET /Users/{}", id))
                .doOnSuccess(p -> log.debug("User found: {}", p))
                .doOnError(e -> log.error("Error getting User by id {}", id, e))
                .onErrorResume(e -> {
                    log.warn("Fallback activated for User id {}", id);
                    return Mono.empty();
                });
    }

    // =========================================================
    // 📌 CREATE User
    // =========================================================
    @Override
    public Mono<User> createUser(User user) {
        return webClient.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .retrieve()
                .bodyToMono(UserDto.class)
                .map(mapper::toDomainUser)
                .doOnSubscribe(s -> log.info("Calling JSONPlaceholder: POST /Users"))
                .doOnSuccess(p -> log.info("User created with id {}", p.id()))
                .doOnError(e -> log.error("Error creating User", e));
    }
}

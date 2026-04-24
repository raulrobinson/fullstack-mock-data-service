package com.raulbolivar.driven.jsonplaceholder.adapter;

import com.raulbolivar.driven.jsonplaceholder.dto.PostDto;
import com.raulbolivar.driven.jsonplaceholder.mapper.JSONPlaceHolderMapper;
import com.raulbolivar.model.Post;
import com.raulbolivar.ports.IJSONPlaceHolderPostGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class JSONPlaceHolderPostAdapter implements IJSONPlaceHolderPostGateway {

    private final WebClient webClient;
    private final JSONPlaceHolderMapper mapper;

    public JSONPlaceHolderPostAdapter(
            @Qualifier("jsonPlaceHolderPostWebClient") WebClient webClient,
            JSONPlaceHolderMapper mapper
    ) {
        this.webClient = webClient;
        this.mapper = mapper;
    }

    // =========================================================
    // 📌 GET ALL POSTS
    // =========================================================
    @Override
    public Flux<Post> getPosts() {
        return webClient.get()
                .uri("/posts")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(PostDto.class)
                .map(mapper::toDomainPost)
                .doOnSubscribe(s -> log.info("Calling JSONPlaceholder: GET /posts"))
                .doOnNext(p -> log.debug("Post received: {}", p.id()))
                .doOnError(e -> log.error("Error getting posts", e));
    }

    // =========================================================
    // 📌 GET POST BY ID
    // =========================================================
    @Override
    public Mono<Post> getPostById(Long id) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/posts/{id}")
                        .build(id))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(PostDto.class)
                .map(mapper::toDomainPost)
                .doOnSubscribe(s -> log.info("Calling JSONPlaceholder: GET /posts/{}", id))
                .doOnSuccess(p -> log.debug("Post found: {}", p))
                .doOnError(e -> log.error("Error getting post by id {}", id, e))
                .onErrorResume(e -> {
                    log.warn("Fallback activated for post id {}", id);
                    return Mono.empty();
                });
    }

    // =========================================================
    // 📌 GET POSTS BY USER
    // =========================================================
    @Override
    public Flux<Post> getPostsByUser(Long userId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/posts")
                        .queryParam("userId", userId)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(PostDto.class)
                .map(mapper::toDomainPost)
                .doOnSubscribe(s -> log.info("Calling JSONPlaceholder: GET /posts?userId={}", userId))
                .doOnError(e -> log.error("Error getting posts by user {}", userId, e));
    }

    // =========================================================
    // 📌 CREATE POST
    // =========================================================
    @Override
    public Mono<Post> createPost(Post post) {
        return webClient.post()
                .uri("/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(post)
                .retrieve()
                .bodyToMono(PostDto.class)
                .map(mapper::toDomainPost)
                .doOnSubscribe(s -> log.info("Calling JSONPlaceholder: POST /posts"))
                .doOnSuccess(p -> log.info("Post created with id {}", p.id()))
                .doOnError(e -> log.error("Error creating post", e));
    }
}

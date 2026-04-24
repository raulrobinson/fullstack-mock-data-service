package com.raulbolivar.ports;

import com.raulbolivar.model.Post;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IJSONPlaceHolderPostGateway {

    Flux<Post> getPosts();

    Mono<Post> getPostById(Long id);

    Flux<Post> getPostsByUser(Long userId);

    Mono<Post> createPost(Post post);
}

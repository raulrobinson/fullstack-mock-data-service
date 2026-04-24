package com.raulbolivar.ports;

import com.raulbolivar.model.user.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IJSONPlaceHolderUserGateway {

    Flux<User> getUsers();

    Mono<User> getUserById(Long id);

    //Flux<User> getPostsByUser(Long userId);

    Mono<User> createUser(User post);
}

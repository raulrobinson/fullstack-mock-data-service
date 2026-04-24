package com.raulbolivar.fs.domain.exceptions;

public class PostNotFoundException extends BusinessException {

    public PostNotFoundException(String id) {
        super("posts", "POST_NOT_FOUND", "Post with id " + id + " not found");
    }
}

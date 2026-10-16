package com.hertt.moviesallieapi.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(

    @Size(max = 2000, message = "O comentário deve ter no máximo 2000 caracteres")
    String comment

) {}

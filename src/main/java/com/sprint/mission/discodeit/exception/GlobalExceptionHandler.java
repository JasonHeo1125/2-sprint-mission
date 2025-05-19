package com.sprint.mission.discodeit.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;


public class GlobalExceptionHandler {

  @ExceptionHandler(Exception.class)
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
  }
}

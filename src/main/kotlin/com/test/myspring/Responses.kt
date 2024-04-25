package com.test.myspring

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ResponseStatusException

data class DataResponse(
    val data: Any?,
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class PageResponse(
    val data: Data,
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class Data(
        val items: List<*>,
        val page: Int,
        val perPage: Int,
        val totalItems: Long,
        val hasPrev: Boolean,
        val hasNext: Boolean,
    )
}

data class ErrorResponse(
    val code: String,
    val message: String?,
)

fun Any?.ok(): ResponseEntity<*> {
    return ResponseEntity.ok(DataResponse(this))
}

fun Page<*>.ok(): ResponseEntity<*> {
    return ResponseEntity.ok(
        PageResponse(
            data = PageResponse.Data(
                items = content,
                page = number,
                perPage = size,
                totalItems = totalElements,
                hasPrev = hasPrevious(),
                hasNext = hasNext(),
            ),
        ),
    )
}

@ControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(value = [ResponseStatusException::class])
    fun handleApiException(e: ResponseStatusException): ResponseEntity<Any> {
        return ResponseEntity(
            ErrorResponse(
                code = e.statusCode.value().toString(),
                message = e.reason,
            ),
            e.statusCode,
        )
    }

    @ExceptionHandler(value = [IllegalStateException::class])
    fun handleIllegalStateException(e: IllegalStateException): ResponseEntity<Any> {
        return ResponseEntity(
            ErrorResponse(
                code = HttpStatus.BAD_REQUEST.value().toString(),
                message = e.message,
            ),
            HttpStatus.BAD_REQUEST,
        )
    }
}
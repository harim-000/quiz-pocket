package com.quizpocket;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
@RestControllerAdvice
class ApiErrors {
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"요청을 처리할 수 없어요.":e.getReason()));}
 @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class}) ResponseEntity<?> invalid(Exception e){return ResponseEntity.badRequest().body(Map.of("message","입력값의 형식과 길이를 확인해 주세요."));}
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> duplicate(Exception e){return ResponseEntity.status(409).body(Map.of("message","중복되거나 유효하지 않은 데이터예요. 새로고침 후 확인해 주세요."));}
}

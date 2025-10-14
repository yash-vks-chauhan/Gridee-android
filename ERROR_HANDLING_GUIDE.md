# Error Handling Guide - Industry Standards Implementation

## Overview

The `GlobalExceptionControllerAdvice` class provides enterprise-grade error handling following industry best practices and international standards.

## Standards Compliance

### ✅ RFC 7807 - Problem Details for HTTP APIs
- Standardized error response format
- Machine-readable error codes
- Human-readable messages
- Proper HTTP status codes

### ✅ REST API Best Practices
- Consistent error response structure
- Detailed validation errors
- Security-aware messaging (no sensitive data exposure)

### ✅ OpenAPI 3.0 Compatible
- Well-structured error responses for API documentation
- Consistent schema across all endpoints

### ✅ Cloud-Native Ready
- Distributed tracing integration (trace IDs in responses)
- MDC context for log correlation
- Compatible with: Datadog, New Relic, AWS CloudWatch, ELK Stack

---

## Error Response Format

### Standard Response Structure

```json
{
  "timestamp": "2025-10-14T10:30:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "errorCode": "VALIDATION_ERROR",
  "message": "Validation failed for 2 field(s)",
  "path": "/api/v1/bookings",
  "traceId": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "requestId": "req-12ab",
  "validationErrors": {
    "email": "Email must be valid",
    "phoneNumber": "Phone number is required"
  }
}
```

### Fields Description

| Field | Type | Description |
|-------|------|-------------|
| `timestamp` | ISO 8601 | When the error occurred |
| `status` | integer | HTTP status code (400, 404, 500, etc.) |
| `error` | string | HTTP status reason phrase |
| `errorCode` | string | Application-specific error code for categorization |
| `message` | string | Human-readable error description |
| `path` | string | Request URI that caused the error |
| `traceId` | string | Distributed tracing ID (for log correlation) |
| `requestId` | string | Unique request identifier |
| `validationErrors` | object | Field-level validation errors (when applicable) |

---

## Exception Handling Coverage

### 1️⃣ Business Logic Exceptions (4xx)

| Exception | Status | Error Code | Description |
|-----------|--------|------------|-------------|
| `NotFoundException` | 404 | RESOURCE_NOT_FOUND | Resource not found in database |
| `ConflictException` | 409 | RESOURCE_CONFLICT | Resource already exists or state conflict |
| `IllegalStateException` | 400 | ILLEGAL_STATE | Operation not allowed in current state |
| `InsufficientFundsException` | 402 | INSUFFICIENT_FUNDS | Wallet balance too low |

### 2️⃣ Validation Exceptions (400)

| Exception | Error Code | Description |
|-----------|------------|-------------|
| `MethodArgumentNotValidException` | VALIDATION_ERROR | @Valid annotation validation failed |
| `ConstraintViolationException` | CONSTRAINT_VIOLATION | Bean validation constraint violated |
| `MissingServletRequestParameterException` | MISSING_PARAMETER | Required parameter missing |
| `MethodArgumentTypeMismatchException` | TYPE_MISMATCH | Parameter type conversion failed |
| `HttpMessageNotReadableException` | MALFORMED_REQUEST | Request body parsing failed |

### 3️⃣ Security Exceptions (401, 403)

| Exception | Status | Error Code | Description |
|-----------|--------|------------|-------------|
| `AuthenticationException` | 401 | AUTHENTICATION_FAILED | Invalid credentials |
| `BadCredentialsException` | 401 | AUTHENTICATION_FAILED | Username/password incorrect |
| `AccessDeniedException` | 403 | ACCESS_DENIED | Insufficient permissions |

### 4️⃣ HTTP Protocol Exceptions

| Exception | Status | Error Code | Description |
|-----------|--------|------------|-------------|
| `HttpRequestMethodNotSupportedException` | 405 | METHOD_NOT_ALLOWED | Wrong HTTP method used |
| `HttpMediaTypeNotSupportedException` | 415 | UNSUPPORTED_MEDIA_TYPE | Content-Type not supported |
| `NoHandlerFoundException` | 404 | ENDPOINT_NOT_FOUND | Endpoint doesn't exist |

### 5️⃣ Generic Exception (500)

| Exception | Status | Error Code | Description |
|-----------|--------|------------|-------------|
| `Exception` (catch-all) | 500 | INTERNAL_SERVER_ERROR | Unexpected server error |

---

## Integration with Distributed Tracing

### Automatic Trace ID Injection

Every error response includes the **trace ID** from your MDC context, enabling:

✅ **End-to-end request tracking** across microservices  
✅ **Log correlation** in cloud monitoring tools  
✅ **Debugging** by searching logs with trace ID  
✅ **APM integration** with Datadog, New Relic, etc.

### Example: Correlating Errors with Logs

**Error Response:**
```json
{
  "traceId": "a1b2c3d4e5f6g7h8",
  "errorCode": "INSUFFICIENT_FUNDS",
  "message": "Insufficient wallet balance"
}
```

**Log Query (in CloudWatch/Datadog):**
```
traceId:a1b2c3d4e5f6g7h8
```

This will show you the complete request flow, including all service calls.

---

## Security Features

### 🔒 Sensitive Data Protection

1. **Generic Error Messages for 500 Errors**
   - Internal errors don't expose stack traces to clients
   - Full details logged server-side only

2. **Security Event Auditing**
   - Authentication failures add `securityEvent` to MDC
   - Source IP captured for security auditing
   - Integration with Security Audit Log

3. **No Stack Trace Leakage**
   - Stack traces never sent to clients
   - Available only in server logs

---

## Example API Responses

### ✅ Successful Request
```bash
curl -X POST /api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{"spotId": "spot123", "duration": 2}'

# Response: 201 Created
{
  "id": "booking456",
  "status": "CONFIRMED"
}
```

### ❌ Validation Error (400)
```bash
curl -X POST /api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{"spotId": "", "duration": -1}'

# Response: 400 Bad Request
{
  "timestamp": "2025-10-14T10:30:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "errorCode": "VALIDATION_ERROR",
  "message": "Validation failed for 2 field(s)",
  "path": "/api/v1/bookings",
  "traceId": "abc123",
  "validationErrors": {
    "spotId": "Spot ID cannot be empty",
    "duration": "Duration must be positive"
  }
}
```

### ❌ Resource Not Found (404)
```bash
curl -X GET /api/v1/bookings/nonexistent

# Response: 404 Not Found
{
  "timestamp": "2025-10-14T10:30:45.123Z",
  "status": 404,
  "error": "Not Found",
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "Booking with ID 'nonexistent' not found",
  "path": "/api/v1/bookings/nonexistent",
  "traceId": "xyz789"
}
```

### ❌ Authentication Failed (401)
```bash
curl -X GET /api/v1/wallet \
  -H "Authorization: Bearer invalid_token"

# Response: 401 Unauthorized
{
  "timestamp": "2025-10-14T10:30:45.123Z",
  "status": 401,
  "error": "Unauthorized",
  "errorCode": "AUTHENTICATION_FAILED",
  "message": "Authentication failed. Please provide valid credentials.",
  "path": "/api/v1/wallet",
  "traceId": "def456"
}
```

### ❌ Insufficient Funds (402)
```bash
curl -X POST /api/v1/wallet/withdraw \
  -H "Authorization: Bearer valid_token" \
  -d '{"amount": 10000}'

# Response: 402 Payment Required
{
  "timestamp": "2025-10-14T10:30:45.123Z",
  "status": 402,
  "error": "Payment Required",
  "errorCode": "INSUFFICIENT_FUNDS",
  "message": "Insufficient wallet balance for this transaction",
  "path": "/api/v1/wallet/withdraw",
  "traceId": "ghi789"
}
```

---

## Usage in Your Services

### Throwing Custom Exceptions

```java
@Service
public class BookingService {
    
    public Booking getBooking(String id) {
        return bookingRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(
                "Booking with ID '" + id + "' not found"
            ));
    }
    
    public void cancelBooking(String id) {
        Booking booking = getBooking(id);
        
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException(
                "Cannot cancel a completed booking"
            );
        }
        
        // Cancel logic...
    }
}
```

### The handler automatically:
1. ✅ Catches the exception
2. ✅ Maps it to proper HTTP status
3. ✅ Creates standardized response
4. ✅ Adds trace ID for debugging
5. ✅ Logs the error with context

---

## Testing Error Responses

### Unit Test Example

```java
@WebMvcTest(BookingController.class)
class BookingControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldReturn404WhenBookingNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/nonexistent"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.traceId").exists());
    }
}
```

---

## Monitoring & Alerting

### Key Metrics to Track

1. **Error Rate by Status Code**
   - 4xx errors (client issues)
   - 5xx errors (server issues)

2. **Error Rate by Error Code**
   - Which business errors occur most frequently
   - Validation error patterns

3. **Response Time During Errors**
   - Are errors handled quickly?

4. **Error Correlation**
   - Use trace IDs to track error propagation across services

### CloudWatch/Datadog Queries

```
# Count 5xx errors
status:500 service.name:gridee-parking

# Track specific error type
errorCode:INSUFFICIENT_FUNDS

# Find all errors for a user
userId:user123 log.level:ERROR

# Trace complete request flow
traceId:abc123
```

---

## Benefits of This Implementation

✅ **Consistency** - All errors follow the same format  
✅ **Debuggability** - Trace IDs enable quick problem resolution  
✅ **Security** - No sensitive data leakage  
✅ **Monitoring** - Easy integration with APM tools  
✅ **Client-Friendly** - Clear, actionable error messages  
✅ **Standards-Compliant** - Follows RFC 7807 and REST best practices  
✅ **Production-Ready** - Comprehensive coverage of all error scenarios  

---

## Additional Resources

- [RFC 7807: Problem Details for HTTP APIs](https://tools.ietf.org/html/rfc7807)
- [REST API Best Practices](https://restfulapi.net/http-status-codes/)
- [OpenAPI 3.0 Error Responses](https://swagger.io/docs/specification/describing-responses/)
- [Spring Boot Error Handling](https://spring.io/blog/2013/11/01/exception-handling-in-spring-mvc)


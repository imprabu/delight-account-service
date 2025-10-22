# Session Management Manual Testing with curl

This guide provides step-by-step curl commands to test the session management functionality.

## Prerequisites
- Application running on `http://localhost:8080`
- curl installed

## Test Steps

### 1. Create Account and User (Sign Up)

```bash
curl -X POST "http://localhost:8080/account/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "domain": "testcompany.com",
    "companyName": "Test Company",
    "email": "admin@testcompany.com",
    "userName": "Admin User",
    "password": "SecurePassword123!"
  }'
```

**Expected:** HTTP 200/201 with account creation confirmation

### 2. Sign In with Session Creation

```bash
curl -X POST "http://localhost:8080/account/signin" \
  -H "Content-Type: application/json" \
  -H "User-Agent: curl/test-client" \
  -c cookies.txt \
  -d '{
    "email": "admin@testcompany.com",
    "password": "SecurePassword123!"
  }'
```

**Expected:** 
- HTTP 200 
- Response with JWT token and session ID
- `hrsession` cookie saved to cookies.txt

### 3. Validate Session

```bash
curl -X GET "http://localhost:8080/accounts/session" \
  -b cookies.txt
```

**Expected:** 
- HTTP 200
- Session details (user info, session ID, last accessed time)

### 4. Test Protected Endpoint (Optional)

```bash
curl -X GET "http://localhost:8080/some-protected-endpoint" \
  -b cookies.txt
```

**Expected:** Access granted with session authentication

### 5. Logout Single Session

```bash
curl -X POST "http://localhost:8080/accounts/logout" \
  -b cookies.txt
```

**Expected:** 
- HTTP 200
- Success message
- Session invalidated

### 6. Validate Session After Logout

```bash
curl -X GET "http://localhost:8080/accounts/session" \
  -b cookies.txt
```

**Expected:** 
- HTTP 401 Unauthorized
- Error message about invalid/expired session

### 7. Sign In Again (for testing logout all)

```bash
curl -X POST "http://localhost:8080/account/signin" \
  -H "Content-Type: application/json" \
  -H "User-Agent: curl/test-client-2" \
  -c cookies.txt \
  -d '{
    "email": "admin@testcompany.com",
    "password": "SecurePassword123!"
  }'
```

### 8. Logout All Sessions

```bash
curl -X POST "http://localhost:8080/accounts/logout-all" \
  -b cookies.txt
```

**Expected:** 
- HTTP 200
- Success message
- All user sessions invalidated

### 9. Validate Session After Logout All

```bash
curl -X GET "http://localhost:8080/accounts/session" \
  -b cookies.txt
```

**Expected:** 
- HTTP 401 Unauthorized
- Error message about invalid/expired session

## Additional Tests

### Test with Invalid Cookie

```bash
curl -X GET "http://localhost:8080/accounts/session" \
  -H "Cookie: hrsession=invalid-jwt-token"
```

**Expected:** HTTP 401 Unauthorized

### Test without Cookie

```bash
curl -X GET "http://localhost:8080/accounts/session"
```

**Expected:** HTTP 401 Unauthorized

### Check Cookie Details

```bash
cat cookies.txt
```

This will show the saved cookies including the `hrsession` cookie with HttpOnly flag.

## Troubleshooting

1. **Application not starting:** Check if port 8080 is available
2. **Database errors:** Ensure database is running and migrations applied
3. **Cookie not saved:** Check if `-c cookies.txt` flag is used in signin
4. **Session validation fails:** Ensure `-b cookies.txt` flag is used

## Expected Session Flow

1. **Sign In** → Creates session in database + sets HTTP-only cookie
2. **API Calls** → JWT extracted from cookie → Session validated
3. **Logout** → Session invalidated in database + cookie cleared
4. **Subsequent calls** → Return 401 Unauthorized

## Security Features Tested

- ✅ HTTP-only cookies (prevents XSS)
- ✅ Session validation on each request
- ✅ Proper session cleanup on logout
- ✅ JWT token validation
- ✅ User agent and IP tracking
- ✅ Session expiration handling
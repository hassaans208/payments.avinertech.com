@echo off
echo Testing Updated Signature-Based Payment Gateway API...

echo.
echo 1. Testing Health Check...
curl -s http://localhost:8080/v1/health/ready

echo.
echo 2. Testing Payment Creation with Signature (JSON Payload)...
curl -X POST http://localhost:8080/v1/payments ^
  -H "Content-Type: application/json" ^
  -H "X-APP-SIGNATURE: test-signature" ^
  -H "X-ENC-SUB: test-subdomain" ^
  -d "{\"signature\":\"encrypted-signature-containing-json-payload\"}"

echo.
echo Test completed!
pause

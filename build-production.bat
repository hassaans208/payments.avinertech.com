@echo off
echo Building Payment Gateway for Production...

echo.
echo Step 1: Cleaning previous build...
call gradlew clean

echo.
echo Step 2: Running tests...
call gradlew test

echo.
echo Step 3: Building production JAR...
call gradlew build -x test

echo.
echo Step 4: Creating production directory structure...
if not exist "production" mkdir production
if not exist "production\logs" mkdir production\logs
if not exist "production\config" mkdir production\config

echo.
echo Step 5: Copying files...
copy "build\libs\*.jar" "production\payment-gateway.jar"
copy "production.env" "production\config\"
copy "src\main\resources\application-prod.yaml" "production\config\"

echo.
echo Step 6: Creating startup script...
echo @echo off > production\start.bat
echo echo Starting Payment Gateway... >> production\start.bat
echo set JAVA_HOME=C:\Program Files\Java\jdk-21 >> production\start.bat
echo java %JAVA_OPTS% -jar payment-gateway.jar --spring.config.location=config/application-prod.yaml >> production\start.bat

echo.
echo Step 7: Creating stop script...
echo @echo off > production\stop.bat
echo echo Stopping Payment Gateway... >> production\stop.bat
echo for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080') do taskkill /f /pid %%a >> production\stop.bat

echo.
echo Production build completed!
echo Files are ready in the 'production' directory.
echo.
echo To deploy:
echo 1. Copy the 'production' folder to your server
echo 2. Update production.env with your configuration
echo 3. Run start.bat to start the application
echo.
pause

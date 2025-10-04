#!/bin/bash
# Production Build Script for Linux/Unix

echo "Building Payment Gateway for Production..."

echo ""
echo "Step 1: Cleaning previous build..."
./gradlew clean

echo ""
echo "Step 2: Running tests..."
./gradlew test

echo ""
echo "Step 3: Building production JAR..."
./gradlew build -x test

echo ""
echo "Step 4: Creating production directory structure..."
mkdir -p production/logs
mkdir -p production/config

echo ""
echo "Step 5: Copying files..."
cp build/libs/*.jar production/payment-gateway.jar
cp production.env production/config/
cp src/main/resources/application-prod.yaml production/config/

echo ""
echo "Step 6: Creating startup script..."
cat > production/start.sh << 'EOF'
#!/bin/bash
echo "Starting Payment Gateway..."

# Load environment variables
if [ -f config/production.env ]; then
    source config/production.env
fi

# Set Java options
export JAVA_OPTS="${JAVA_OPTS:-"-Xmx1024m -Xms512m -XX:+UseG1GC"}"

# Start the application
java $JAVA_OPTS -jar payment-gateway.jar --spring.config.location=config/application-prod.yaml
EOF

echo ""
echo "Step 7: Creating stop script..."
cat > production/stop.sh << 'EOF'
#!/bin/bash
echo "Stopping Payment Gateway..."
pkill -f payment-gateway.jar
EOF

echo ""
echo "Step 8: Creating systemd service script..."
cat > production/payment-gateway.service << 'EOF'
[Unit]
Description=Payment Gateway Service
After=network.target

[Service]
Type=simple
User=payment-gateway
Group=payment-gateway
WorkingDirectory=/opt/payment-gateway
ExecStart=/usr/bin/java -Xmx1024m -Xms512m -XX:+UseG1GC -jar payment-gateway.jar --spring.config.location=config/application-prod.yaml
ExecStop=/bin/kill -15 $MAINPID
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

echo ""
echo "Step 9: Making scripts executable..."
chmod +x production/start.sh
chmod +x production/stop.sh

echo ""
echo "Production build completed!"
echo "Files are ready in the 'production' directory."
echo ""
echo "To deploy:"
echo "1. Copy the 'production' folder to your server"
echo "2. Update production.env with your configuration"
echo "3. Run ./start.sh to start the application"
echo "4. Or install as systemd service: sudo cp payment-gateway.service /etc/systemd/system/"
echo ""

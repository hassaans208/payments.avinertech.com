#!/bin/bash
# Quick CORS Fix Deployment Script
# This script updates the production server with CORS fixes

echo "🔧 Deploying CORS fixes to production server..."

# Configuration
APP_NAME="payment-gateway"
APP_DIR="/opt/$APP_NAME"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if running as root
if [[ $EUID -eq 0 ]]; then
   print_error "This script should not be run as root for security reasons"
   exit 1
fi

print_status "Starting CORS fix deployment..."

# Step 1: Stop the service
print_status "Stopping payment gateway service..."
sudo systemctl stop $APP_NAME

# Step 2: Backup current JAR
print_status "Creating backup of current JAR..."
sudo cp $APP_DIR/payment-gateway.jar $APP_DIR/payment-gateway.jar.backup.$(date +%Y%m%d_%H%M%S)

# Step 3: Build new JAR with CORS fixes
print_status "Building new JAR with CORS fixes..."
if [ -f "build-production.sh" ]; then
    chmod +x build-production.sh
    ./build-production.sh
else
    print_error "build-production.sh not found!"
    exit 1
fi

# Step 4: Copy new JAR to production
print_status "Copying new JAR to production..."
sudo cp production/payment-gateway.jar $APP_DIR/

# Step 5: Update environment configuration
print_status "Updating CORS configuration..."
sudo sed -i 's|export CORS_ALLOWED_ORIGINS=.*|export CORS_ALLOWED_ORIGINS=https://onboard.avinertech.com,https://www.onboard.avinertech.com,https://avinertech.com|g' $APP_DIR/config/production.env

# Step 6: Set ownership
print_status "Setting proper ownership..."
sudo chown payment-gateway:payment-gateway $APP_DIR/payment-gateway.jar

# Step 7: Start the service
print_status "Starting payment gateway service..."
sudo systemctl start $APP_NAME

# Step 8: Wait for service to start
print_status "Waiting for service to start..."
sleep 10

# Step 9: Check service status
if sudo systemctl is-active --quiet $APP_NAME; then
    print_status "Service started successfully!"
    
    # Test CORS endpoint
    print_status "Testing CORS endpoint..."
    if curl -f http://localhost:8080/v1/cors/test > /dev/null 2>&1; then
        print_status "CORS test endpoint is working!"
    else
        print_warning "CORS test endpoint failed - check logs: sudo journalctl -u $APP_NAME -f"
    fi
else
    print_error "Service failed to start - check logs: sudo journalctl -u $APP_NAME -f"
    exit 1
fi

# Step 10: Display final information
echo ""
echo "🎉 CORS fixes deployed successfully!"
echo "=================================="
echo ""
echo "CORS Configuration:"
echo "- Allowed Origins: https://onboard.avinertech.com, https://www.onboard.avinertech.com, https://avinertech.com"
echo "- Methods: GET, POST, PUT, DELETE, OPTIONS"
echo "- Headers: All headers allowed"
echo "- Credentials: Enabled"
echo ""
echo "Test Endpoints:"
echo "- CORS Test: http://your-server:8080/v1/cors/test"
echo "- Health Check: http://your-server:8080/v1/health/ready"
echo ""
echo "To test CORS from onboard.avinertech.com:"
echo "1. Open browser console on onboard.avinertech.com"
echo "2. Run: fetch('http://your-server:8080/v1/cors/test')"
echo "3. Check for CORS errors"
echo ""
print_warning "If you still get CORS errors, check:"
print_warning "1. Firewall settings"
print_warning "2. Nginx configuration (if using reverse proxy)"
print_warning "3. Browser cache (try incognito mode)"
echo ""
echo "CORS fix deployment completed! 🚀"

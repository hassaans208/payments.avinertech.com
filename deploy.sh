#!/bin/bash
# Quick Deployment Script for Payment Gateway
# This script automates the entire deployment process

set -e  # Exit on any error

echo "🚀 Payment Gateway Deployment Script"
echo "====================================="

# Configuration
APP_NAME="payment-gateway"
APP_DIR="/opt/$APP_NAME"
SERVICE_USER="payment-gateway"
DB_NAME="payments"
DB_USER="payment_user"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
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

print_status "Starting deployment process..."

# Step 1: Build the application
print_status "Building application..."
if [ -f "build-production.sh" ]; then
    chmod +x build-production.sh
    ./build-production.sh
else
    print_error "build-production.sh not found!"
    exit 1
fi

# Step 2: Check if production directory exists
if [ ! -d "production" ]; then
    print_error "Production build failed - production directory not found"
    exit 1
fi

print_status "Production build completed successfully!"

# Step 3: Database setup
print_status "Setting up database..."
read -p "Enter MariaDB root password: " -s DB_ROOT_PASSWORD
echo

# Create database and user
mysql -u root -p$DB_ROOT_PASSWORD << EOF
CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY 'secure_password_123';
GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USER'@'localhost';
FLUSH PRIVILEGES;
EOF

print_status "Database setup completed!"

# Step 4: Create system user
print_status "Creating system user..."
sudo useradd -r -s /bin/false $SERVICE_USER 2>/dev/null || print_warning "User $SERVICE_USER already exists"

# Step 5: Create application directory
print_status "Setting up application directory..."
sudo mkdir -p $APP_DIR
sudo chown $SERVICE_USER:$SERVICE_USER $APP_DIR

# Step 6: Copy application files
print_status "Copying application files..."
sudo cp -r production/* $APP_DIR/
sudo chown -R $SERVICE_USER:$SERVICE_USER $APP_DIR

# Step 7: Update configuration
print_status "Updating configuration..."
sudo sed -i "s/your_secure_password_here/secure_password_123/g" $APP_DIR/config/production.env
sudo sed -i "s/your_admin_password_here/admin_password_123/g" $APP_DIR/config/production.env

# Step 8: Install systemd service
print_status "Installing systemd service..."
sudo cp $APP_DIR/payment-gateway.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable $APP_NAME

# Step 9: Start the service
print_status "Starting service..."
sudo systemctl start $APP_NAME

# Step 10: Wait for service to start
print_status "Waiting for service to start..."
sleep 10

# Step 11: Check service status
if sudo systemctl is-active --quiet $APP_NAME; then
    print_status "Service started successfully!"
    
    # Test health endpoint
    print_status "Testing health endpoint..."
    if curl -f http://localhost:8080/v1/health/ready > /dev/null 2>&1; then
        print_status "Health check passed!"
    else
        print_warning "Health check failed - check logs: sudo journalctl -u $APP_NAME -f"
    fi
else
    print_error "Service failed to start - check logs: sudo journalctl -u $APP_NAME -f"
    exit 1
fi

# Step 12: Display final information
echo ""
echo "🎉 Deployment completed successfully!"
echo "====================================="
echo ""
echo "Application Details:"
echo "- Service Name: $APP_NAME"
echo "- Installation Directory: $APP_DIR"
echo "- Service User: $SERVICE_USER"
echo "- Database: $DB_NAME"
echo "- Database User: $DB_USER"
echo ""
echo "Service Commands:"
echo "- Start: sudo systemctl start $APP_NAME"
echo "- Stop: sudo systemctl stop $APP_NAME"
echo "- Restart: sudo systemctl restart $APP_NAME"
echo "- Status: sudo systemctl status $APP_NAME"
echo "- Logs: sudo journalctl -u $APP_NAME -f"
echo ""
echo "Application URLs:"
echo "- Health Check: http://localhost:8080/v1/health/ready"
echo "- API Documentation: http://localhost:8080/swagger-ui.html"
echo "- Checkout Form: http://localhost:8080/forms/payment/checkout"
echo ""
echo "Configuration Files:"
echo "- Environment: $APP_DIR/config/production.env"
echo "- Application: $APP_DIR/config/application-prod.yaml"
echo ""
print_warning "Remember to update passwords in $APP_DIR/config/production.env"
print_warning "Configure firewall to allow access to port 8080"
echo ""
echo "Deployment completed! 🚀"

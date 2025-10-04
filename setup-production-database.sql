-- Production Database Setup Script
-- Run this script on your MariaDB server before deploying the application

-- Create database
CREATE DATABASE IF NOT EXISTS payments CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create user for the application
CREATE USER IF NOT EXISTS 'payment_user'@'localhost' IDENTIFIED BY 'your_secure_password_here';
CREATE USER IF NOT EXISTS 'payment_user'@'%' IDENTIFIED BY 'your_secure_password_here';

-- Grant permissions
GRANT ALL PRIVILEGES ON payments.* TO 'payment_user'@'localhost';
GRANT ALL PRIVILEGES ON payments.* TO 'payment_user'@'%';

-- Flush privileges
FLUSH PRIVILEGES;

-- Use the database
USE payments;

-- Create tables (these will be created by Hibernate, but you can create them manually if needed)
-- The application will create tables automatically with hibernate.ddl-auto=validate

-- Create indexes for better performance
-- These will be created by Hibernate annotations, but you can add custom ones here

-- Example: Create index on tenant_id for faster queries
-- CREATE INDEX idx_payment_tenant_id ON payment(tenant_id);
-- CREATE INDEX idx_payment_status ON payment(status);
-- CREATE INDEX idx_payment_created_at ON payment(created_at);

-- Verify setup
SELECT 'Database setup completed successfully!' as status;
SHOW DATABASES LIKE 'payments';
SELECT User, Host FROM mysql.user WHERE User = 'payment_user';

# Payment Gateway - Production Deployment Guide

## Overview
This guide covers deploying the Signature-Based Payment Gateway application to a traditional server environment.

## Prerequisites

### Server Requirements
- **OS**: Linux (Ubuntu 20.04+ recommended) or Windows Server 2019+
- **Java**: OpenJDK 21 or Oracle JDK 21
- **Database**: MariaDB 10.6+ or MySQL 8.0+
- **Memory**: Minimum 2GB RAM, Recommended 4GB+
- **Storage**: Minimum 10GB free space
- **Network**: Port 8080 accessible (or custom port)

### Software Installation
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-21-jdk mariadb-server

# CentOS/RHEL
sudo yum install java-21-openjdk-devel mariadb-server

# Windows
# Download and install OpenJDK 21 from https://adoptium.net/
# Download and install MariaDB from https://mariadb.org/download/
```

## Deployment Steps

### 1. Build the Application

#### Windows:
```cmd
build-production.bat
```

#### Linux/Unix:
```bash
chmod +x build-production.sh
./build-production.sh
```

### 2. Database Setup

1. **Start MariaDB service:**
   ```bash
   sudo systemctl start mariadb
   sudo systemctl enable mariadb
   ```

2. **Run database setup script:**
   ```bash
   mysql -u root -p < setup-production-database.sql
   ```

3. **Update database credentials in `production.env`**

### 3. Server Configuration

1. **Create application user (Linux):**
   ```bash
   sudo useradd -r -s /bin/false payment-gateway
   sudo mkdir -p /opt/payment-gateway
   sudo chown payment-gateway:payment-gateway /opt/payment-gateway
   ```

2. **Copy application files:**
   ```bash
   sudo cp -r production/* /opt/payment-gateway/
   sudo chown -R payment-gateway:payment-gateway /opt/payment-gateway
   ```

### 4. Environment Configuration

1. **Update `production.env` with your settings:**
   ```bash
   # Database
   export DB_HOST=localhost
   export DB_NAME=payments
   export DB_USERNAME=payment_user
   export DB_PASSWORD=your_secure_password

   # Security
   export ADMIN_USERNAME=admin
   export ADMIN_PASSWORD=your_admin_password

   # External Services
   export SIGNATURE_DECRYPT_URL=https://signal.avinertech.com/api/decrypt
   ```

### 5. Start the Application

#### Option A: Manual Start
```bash
cd /opt/payment-gateway
./start.sh
```

#### Option B: Systemd Service (Linux)
```bash
sudo cp payment-gateway.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable payment-gateway
sudo systemctl start payment-gateway
```

#### Option C: Windows Service
Use tools like NSSM (Non-Sucking Service Manager) to install as Windows service.

### 6. Verify Deployment

1. **Check application status:**
   ```bash
   curl http://localhost:8080/v1/health/ready
   ```

2. **Check logs:**
   ```bash
   tail -f logs/payment-gateway.log
   ```

3. **Test API endpoints:**
   ```bash
   curl -X POST http://localhost:8080/v1/payments \
     -H "Content-Type: application/json" \
     -d '{"signature":"test-signature"}'
   ```

## Configuration Options

### Application Properties
- **Port**: Change `SERVER_PORT` in `production.env`
- **Context Path**: Modify `server.servlet.context-path` in `application-prod.yaml`
- **Database**: Update datasource configuration
- **Logging**: Configure log levels and file locations

### Security Configuration
- **Admin Credentials**: Set `ADMIN_USERNAME` and `ADMIN_PASSWORD`
- **CORS**: Configure `CORS_ALLOWED_ORIGINS` for your domains
- **SSL/TLS**: Configure HTTPS in production

### Performance Tuning
- **JVM Options**: Adjust `JAVA_OPTS` for your server specs
- **Database Pool**: Configure HikariCP settings
- **Connection Timeouts**: Adjust timeout values

## Monitoring and Maintenance

### Health Checks
- **Application Health**: `GET /actuator/health`
- **Database Health**: Check database connectivity
- **External Services**: Monitor signature decryption service

### Log Management
- **Log Rotation**: Configure logrotate for log files
- **Log Levels**: Adjust logging levels in production
- **Monitoring**: Set up log monitoring and alerting

### Backup Strategy
- **Database Backups**: Regular MariaDB backups
- **Application Backups**: Backup configuration files
- **Log Backups**: Archive old log files

## Troubleshooting

### Common Issues

1. **Port Already in Use:**
   ```bash
   sudo netstat -tlnp | grep :8080
   sudo kill -9 <PID>
   ```

2. **Database Connection Issues:**
   - Check MariaDB service status
   - Verify credentials in `production.env`
   - Check firewall settings

3. **Memory Issues:**
   - Increase `-Xmx` value in `JAVA_OPTS`
   - Monitor memory usage with `htop` or `top`

4. **Permission Issues:**
   ```bash
   sudo chown -R payment-gateway:payment-gateway /opt/payment-gateway
   ```

### Log Locations
- **Application Logs**: `logs/payment-gateway.log`
- **System Logs**: `/var/log/syslog` (Linux)
- **MariaDB Logs**: `/var/log/mysql/error.log`

## Security Considerations

1. **Database Security:**
   - Use strong passwords
   - Limit database user permissions
   - Enable SSL connections

2. **Application Security:**
   - Change default admin credentials
   - Configure proper CORS settings
   - Use HTTPS in production

3. **Server Security:**
   - Keep system updated
   - Configure firewall rules
   - Use non-root user for application

## Scaling Considerations

### Horizontal Scaling
- Use load balancer (nginx, HAProxy)
- Multiple application instances
- Session affinity considerations

### Vertical Scaling
- Increase server resources
- Optimize JVM settings
- Database connection pooling

## Support and Maintenance

### Regular Tasks
- Monitor application logs
- Check database performance
- Update security patches
- Backup data regularly

### Emergency Procedures
- Application restart procedures
- Database recovery procedures
- Rollback procedures
- Contact information for support

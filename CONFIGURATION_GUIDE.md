# Configuration Management Guide

## Overview

This guide explains the proper configuration structure following Spring Boot best practices and industry standards.

## Configuration File Structure

### ✅ **Correct Structure (Now Implemented)**

```
src/main/resources/
├── application.properties          ← Common configurations (all environments)
├── application-local.properties    ← Local development overrides
└── application-prod.properties     ← Production overrides
```

## Configuration Principles

### 1️⃣ **application.properties** (Base Configuration)
**Purpose:** Contains ALL common configurations that apply across all environments.

**What Goes Here:**
- ✅ Distributed tracing configuration (Micrometer, OpenTelemetry)
- ✅ Metrics and monitoring setup (Prometheus, actuator endpoints)
- ✅ Logging configuration (patterns, levels, structured logging)
- ✅ Application metadata (name, version)
- ✅ Common Spring Boot settings
- ✅ Default values that work across environments

**What Does NOT Go Here:**
- ❌ Environment-specific URLs (database, APIs)
- ❌ Secrets/credentials (passwords, API keys)
- ❌ Environment-specific overrides

### 2️⃣ **application-local.properties** (Local Development)
**Purpose:** Contains ONLY local development environment-specific values.

**What Goes Here:**
- ✅ Local MongoDB connection string
- ✅ Local SSL certificates
- ✅ Test API keys (Razorpay test mode)
- ✅ Local tracing endpoints (localhost:9411)
- ✅ Development OAuth2 credentials
- ✅ Local environment tags

### 3️⃣ **application-prod.properties** (Production)
**Purpose:** Contains ONLY production environment-specific values.

**What Goes Here:**
- ✅ Production database URIs (from environment variables)
- ✅ Production SSL configuration
- ✅ Production API keys (from secrets manager)
- ✅ Production tracing endpoints (Datadog, New Relic, AWS X-Ray)
- ✅ Production OAuth2 credentials
- ✅ Production logging overrides (reduced verbosity)
- ✅ Production security settings (sampling rates)

---

## What Changed

### ✅ **Moved to application.properties** (Common)

```properties
# Distributed Tracing
management.tracing.enabled=true
management.tracing.sampling.probability=1.0
management.tracing.baggage.enabled=true
management.tracing.propagation.type=w3c,b3

# Metrics & Monitoring
management.endpoints.web.exposure.include=health,info,metrics,prometheus,...
management.metrics.export.prometheus.enabled=true
management.metrics.tags.application=${spring.application.name}

# Logging Configuration
logging.level.com.parking.app=DEBUG
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]...
logging.file.name=logs/application.log
```

### ✅ **Kept in application-local.properties** (Environment-Specific)

```properties
# Local tracing endpoints
management.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans
management.otlp.tracing.endpoint=http://localhost:4318/v1/traces

# Local environment tags
management.metrics.tags.environment=local
management.metrics.tags.region=us-east-1
app.environment=local
```

### ✅ **Updated in application-prod.properties** (Environment-Specific)

```properties
# Production tracing endpoints (from environment variables)
management.zipkin.tracing.endpoint=${ZIPKIN_ENDPOINT:https://...}
management.otlp.tracing.endpoint=${OTLP_ENDPOINT:https://...}

# Production environment tags
management.metrics.tags.environment=production
management.metrics.tags.region=${AWS_REGION:us-east-1}

# Production overrides
logging.level.com.parking.app=INFO
management.tracing.sampling.probability=0.1
```

---

## Why This Structure is Better

### ✅ **1. Single Source of Truth**
- All common configurations are in one place
- No duplication across environment files
- Easier to maintain and update

### ✅ **2. Environment Isolation**
- Environment-specific values are clearly separated
- Reduces risk of using wrong credentials
- Easy to see what differs per environment

### ✅ **3. DRY Principle (Don't Repeat Yourself)**
- Tracing/logging config defined once
- Only endpoints/credentials differ per environment
- Reduces configuration drift

### ✅ **4. Production Safety**
- Production file uses environment variables (12-factor app)
- Secrets never hardcoded
- Can be integrated with secrets managers (AWS Secrets Manager, HashiCorp Vault)

### ✅ **5. Scalability**
- Easy to add new environments (dev, staging, UAT)
- Just create `application-{env}.properties` with overrides
- Common config automatically applies

---

## Configuration Hierarchy

Spring Boot loads properties in this order (later overrides earlier):

```
1. application.properties              ← Base configuration
2. application-{profile}.properties    ← Environment-specific overrides
3. Environment variables               ← Runtime overrides
4. Command-line arguments              ← Deployment overrides
```

### Example Flow for Production:

```
application.properties
├── management.tracing.enabled=true           (from base)
├── logging.level.com.parking.app=DEBUG       (from base)
│
└── application-prod.properties (overrides)
    ├── logging.level.com.parking.app=INFO    (overrides to INFO)
    ├── management.zipkin.tracing.endpoint=${ZIPKIN_ENDPOINT}
    └── management.tracing.sampling.probability=0.1
```

**Result in Production:**
- Tracing enabled: ✅ (from base)
- Logging level: INFO (overridden)
- Zipkin endpoint: From environment variable
- Sampling: 10% (reduced for performance)

---

## Best Practices

### ✅ **DO:**

1. **Keep common configs in base file**
   ```properties
   # application.properties
   management.tracing.enabled=true
   logging.pattern.console=%d{...}
   ```

2. **Use environment variables for secrets**
   ```properties
   # application-prod.properties
   spring.data.mongodb.uri=${MONGODB_URI}
   razorpay.secret=${RAZORPAY_SECRET}
   ```

3. **Provide sensible defaults**
   ```properties
   # application-prod.properties
   management.zipkin.tracing.endpoint=${ZIPKIN_ENDPOINT:https://default-zipkin.com}
   ```

4. **Document environment variables**
   ```bash
   # Required environment variables for production:
   # - MONGODB_URI
   # - RAZORPAY_KEY
   # - RAZORPAY_SECRET
   # - GOOGLE_CLIENT_ID
   # - GOOGLE_CLIENT_SECRET
   ```

### ❌ **DON'T:**

1. **Don't duplicate common configs**
   ```properties
   # ❌ BAD: Don't repeat in application-local.properties
   management.tracing.enabled=true
   ```

2. **Don't hardcode production secrets**
   ```properties
   # ❌ BAD: Never do this
   razorpay.secret=live_secret_key_here
   ```

3. **Don't mix concerns**
   ```properties
   # ❌ BAD: Don't put environment-specific values in base file
   # application.properties
   management.zipkin.tracing.endpoint=http://localhost:9411
   ```

---

## Activating Profiles

### Local Development
```bash
# Automatically uses 'local' profile (set in application.properties)
./gradlew bootRun
```

### Production Deployment
```bash
# Set active profile via environment variable
export SPRING_PROFILES_ACTIVE=prod

# Or via command-line argument
java -jar app.jar --spring.profiles.active=prod

# Or via system property
java -Dspring.profiles.active=prod -jar app.jar
```

### Docker Deployment
```dockerfile
# Dockerfile
ENV SPRING_PROFILES_ACTIVE=prod
ENV MONGODB_URI=mongodb://prod-db:27017/parkingdb
ENV RAZORPAY_KEY=your_production_key
```

### Kubernetes Deployment
```yaml
# deployment.yaml
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "prod"
  - name: MONGODB_URI
    valueFrom:
      secretKeyRef:
        name: mongodb-secret
        key: uri
```

---

## Testing Configuration

### Verify Active Profile
```bash
curl http://localhost:8443/actuator/env | jq '.propertySources[] | select(.name | contains("application"))'
```

### Check Effective Configuration
```bash
curl http://localhost:8443/actuator/configprops
```

### View Active Loggers
```bash
curl http://localhost:8443/actuator/loggers
```

---

## Migration Checklist

✅ **Completed:**
1. ✅ Moved tracing configuration to `application.properties`
2. ✅ Moved logging configuration to `application.properties`
3. ✅ Moved metrics configuration to `application.properties`
4. ✅ Kept environment-specific endpoints in profile files
5. ✅ Updated production file with environment variable placeholders
6. ✅ Documented configuration structure

✅ **Benefits:**
- Single source of truth for common configs
- Clear separation of concerns
- Production-ready with secrets management
- Easier to maintain and scale
- Follows Spring Boot best practices
- Compatible with cloud deployments (AWS, Azure, GCP, Kubernetes)

---

## Additional Resources

- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [12-Factor App Configuration](https://12factor.net/config)
- [Spring Profiles Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles)


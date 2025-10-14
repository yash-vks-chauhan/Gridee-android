# Simple Tracing Enable/Disable Guide

## Quick Reference

### Enable/Disable Tracing with Simple Properties

You can now control Zipkin and OTLP tracing servers using simple properties. No complex code needed!

---

## Option 1: Disable Zipkin Only

**In `application-local.properties`:**
```properties
# Disable Zipkin
management.zipkin.tracing.enabled=false

# Keep OTLP enabled
management.otlp.tracing.enabled=true
```

---

## Option 2: Disable OTLP Only

**In `application-local.properties`:**
```properties
# Keep Zipkin enabled
management.zipkin.tracing.enabled=true

# Disable OTLP
management.otlp.tracing.enabled=false
```

---

## Option 3: Disable Both Zipkin and OTLP

**In `application-local.properties`:**
```properties
# Disable both exporters
management.zipkin.tracing.enabled=false
management.otlp.tracing.enabled=false
```

**Note:** Trace IDs will still be generated and appear in logs!

---

## Option 4: Disable All Tracing Completely

**In `application-local.properties`:**
```properties
# Disable entire tracing system
management.tracing.enabled=false
```

**Note:** No trace IDs will be generated at all.

---

## Common Scenarios

### Scenario 1: Local Development Without Zipkin/OTLP

Most developers don't run Zipkin locally. Just disable it:

**`application-local.properties`:**
```properties
management.zipkin.tracing.enabled=false
management.otlp.tracing.enabled=false
```

**Result:**
- ✅ App starts normally
- ✅ Trace IDs still in logs
- ✅ No connection errors

### Scenario 2: Production with Datadog/New Relic (No Zipkin)

**`application-prod.properties`:**
```properties
# Disable Zipkin
management.zipkin.tracing.enabled=false

# Use OTLP for Datadog/New Relic
management.otlp.tracing.enabled=true
management.otlp.tracing.endpoint=https://your-datadog-endpoint.com
```

### Scenario 3: Using Zipkin Only (No OTLP)

**`application-local.properties`:**
```properties
# Enable Zipkin
management.zipkin.tracing.enabled=true
management.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans

# Disable OTLP
management.otlp.tracing.enabled=false
```

### Scenario 4: Production - Control via Environment Variables

**Set environment variables:**
```bash
export ZIPKIN_ENABLED=true
export ZIPKIN_ENDPOINT=https://zipkin.prod.example.com/api/v2/spans
export OTLP_ENABLED=false
```

**`application-prod.properties` automatically uses them:**
```properties
management.zipkin.tracing.enabled=${ZIPKIN_ENABLED:true}
management.zipkin.tracing.endpoint=${ZIPKIN_ENDPOINT:https://...}
management.otlp.tracing.enabled=${OTLP_ENABLED:true}
```

---

## Quick Commands

### Start app with Zipkin/OTLP disabled
```bash
./gradlew bootRun
```
*(Already disabled in application-local.properties)*

### Start app with Zipkin enabled at runtime
```bash
./gradlew bootRun --args='--management.zipkin.tracing.enabled=true'
```

### Start app with all tracing disabled at runtime
```bash
./gradlew bootRun --args='--management.tracing.enabled=false'
```

---

## What You Get With Each Option

| Configuration | Trace IDs in Logs | Zipkin UI | OTLP Export | Performance Impact |
|---------------|-------------------|-----------|-------------|-------------------|
| All enabled | ✅ | ✅ | ✅ | Low |
| Zipkin only | ✅ | ✅ | ❌ | Very Low |
| OTLP only | ✅ | ❌ | ✅ | Very Low |
| Both disabled | ✅ | ❌ | ❌ | Minimal |
| Tracing disabled | ❌ | ❌ | ❌ | None |

---

## Recommended Settings

### Local Development (No Tracing Servers)
```properties
management.zipkin.tracing.enabled=false
management.otlp.tracing.enabled=false
```

### Staging/UAT (With Zipkin)
```properties
management.zipkin.tracing.enabled=true
management.zipkin.tracing.endpoint=http://zipkin-staging:9411/api/v2/spans
management.otlp.tracing.enabled=false
```

### Production (With OTLP to Datadog/New Relic)
```properties
management.zipkin.tracing.enabled=false
management.otlp.tracing.enabled=true
management.otlp.tracing.endpoint=${OTLP_ENDPOINT}
management.tracing.sampling.probability=0.1
```

---

## Testing

### Verify Current Settings
```bash
curl http://localhost:8443/actuator/env | grep -i "tracing.enabled"
```

### Check if Zipkin is Enabled
```bash
curl http://localhost:8443/actuator/env | grep "management.zipkin.tracing.enabled"
```

### Check if OTLP is Enabled
```bash
curl http://localhost:8443/actuator/env | grep "management.otlp.tracing.enabled"
```

---

## Summary

✅ **Simple property-based control** - No complex configuration classes  
✅ **Environment variable support** - Easy production deployment  
✅ **Flexible** - Enable/disable Zipkin and OTLP independently  
✅ **Safe** - App works fine even when servers are unavailable  
✅ **Trace IDs preserved** - Still in logs even when export is disabled

**Just change the properties and restart - that's it!**


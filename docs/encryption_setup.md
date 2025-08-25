# Token Encryption Setup Guide

This document explains how to set up and use the per-tenant token encryption system in SupportHero.

## Overview

The application now encrypts all OAuth tokens (Slack and Jira) using tenant-specific encryption keys. This provides strong cross-tenant isolation where each tenant's tokens can only be decrypted using their unique encryption key.

## Security Benefits

- **Cross-tenant isolation**: Tenant A's encryption key cannot decrypt Tenant B's tokens
- **Database compromise protection**: Even with database access, tokens cannot be decrypted without the master key
- **Tenant-specific keys**: Each tenant gets a unique encryption key derived from their ID and a master secret

## Setup Instructions

### 1. Set Master Key

The encryption system requires a master key. Set this as an environment variable:

```bash
# For development (use a strong, random key in production)
export ENCRYPTION_MASTER_KEY="your-super-secret-master-key-at-least-32-chars"

# For production, generate a key ONCE and store it securely:
# 1. Generate the key ONCE:
MASTER_KEY=$(openssl rand -base64 32)

# 2. Store it in your secret management system (Kubernetes, Vault, etc.)
# 3. Use the SAME key across all deployments and restarts
export ENCRYPTION_MASTER_KEY="$MASTER_KEY"

# ⚠️  CRITICAL: The master key must be the SAME across all deployments!
# ⚠️  If you lose or change the master key, all encrypted tokens become unrecoverable!
```

## How It Works

### Key Derivation
Each tenant gets a unique encryption key:
```
tenant_key = PBKDF2(master_key + tenant_id, salt="supporthero-tenant-encryption", iterations=100000)
```

### Encryption Process
1. **Store tokens**: `plaintext_token` → `AES256-GCM:base64_encrypted_data`
2. **Retrieve tokens**: `AES256-GCM:base64_encrypted_data` → `plaintext_token`

### Database Storage
Encrypted tokens are stored with a prefix to identify the encryption method:
```
# Before: "xoxb-slack-token-12345"
# After:  "AES256-GCM:dGVzdGVuY3J5cHRlZGRhdGE="
```

## Configuration

### Application Properties

```yaml
app:
  security:
    encryption:
      master-key: ${ENCRYPTION_MASTER_KEY:changeme-in-production}
```

### Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `ENCRYPTION_MASTER_KEY` | Master key for encryption | Yes | `changeme-in-production` |

## Security Considerations

### Master Key Management

1. **Use a strong key**: Minimum 32 characters, cryptographically random
2. **Keep it secret**: Never commit to version control
3. **Rotate regularly**: Plan for key rotation in production
4. **Backup securely**: Store backup copies in secure locations

### Production Deployment

```bash
# ⚠️  IMPORTANT: Only generate a new key for INITIAL deployment!
# If you already have encrypted tokens, use your existing master key!

# For INITIAL deployment - generate master key ONCE:
MASTER_KEY=$(openssl rand -base64 32)
echo "Save this key securely: $MASTER_KEY"

# Store the key in your secret management system
kubectl create secret generic supporthero-encryption \
  --from-literal=ENCRYPTION_MASTER_KEY="$MASTER_KEY"
  
# For subsequent deployments, always use the SAME master key:
# kubectl create secret generic supporthero-encryption \
#   --from-literal=ENCRYPTION_MASTER_KEY="<your-existing-master-key>"
```

## Monitoring and Troubleshooting

### Logs
- Encryption errors: `ERROR` level logs for failed operations
- Debug tenant operations: Set `DEBUG` level for detailed tenant-specific logs

### Common Issues

1. **Invalid encrypted token format**
   - Cause: Corrupted database data or wrong encryption service
   - Solution: Check master key

2. **Failed to decrypt token**
   - Cause: Wrong tenant ID or changed master key
   - Solution: Verify tenant ID mapping and master key consistency

## Key Rotation

⚠️ **WARNING**: Key rotation is a complex operation that requires careful planning.

To rotate the master key (advanced operation):

1. **Backup database**: Always backup before key rotation
2. **Implement key rotation logic**: The current implementation doesn't support automatic key rotation
3. **Manual process**: You would need to:
   - Decrypt all tokens with old key
   - Re-encrypt with new key
   - Update master key atomically
4. **Consider downtime**: This operation may require maintenance window

## Implementation Details

### Classes
- `TenantEncryptionService`: Core encryption/decryption logic
- `JiraTenantRepository`: Updated to encrypt/decrypt Jira tokens
- `SlackTenantRepository`: Updated to encrypt/decrypt Slack tokens

### Configuration
- `RepositoryConfiguration`: Updated to inject encryption service
- `application.yml`: Added encryption configuration properties
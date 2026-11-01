import crypto from 'crypto';
import { collections, FieldValue } from '../../config/firestore.ts';
import { generateApiKey, hashString } from '../../services/security.service.ts';
import { ApiKeyRecord, CreateApiKeyResponse } from '../../types/index.ts';
import { UserService } from '../users/user.service.ts';

/**
 * HWX SMS Gateway - API Key Management Service
 *
 * Implements:
 * 1. Cryptographically secure random key generation (hwx_live_...)
 * 2. SHA-256 storage only - never stores raw keys in database
 * 3. Shows raw key only once
 * 4. Revocation, expiration, and usage counting
 * 5. High-speed lookup index for sub-millisecond authentication
 */
import { QueryDocumentSnapshot } from 'firebase-admin/firestore';

export class ApiKeyService {
  /**
   * Generates and stores a new API key for the user.
   */
  static async createApiKey(
    uid: string,
    name: string,
    expiresAt?: string
  ): Promise<CreateApiKeyResponse> {
    const { rawKey, keyPrefix, keyHash } = generateApiKey();
    const keyId = crypto.randomUUID();
    const now = new Date().toISOString();

    const record: ApiKeyRecord = {
      id: keyId,
      uid,
      name,
      keyPrefix,
      keyHash,
      createdAt: now,
      expiresAt: expiresAt || undefined,
      usageCount: 0,
      status: 'active',
    };

    // Store in user's subcollection
    await collections.apiKeyDoc(uid, keyId).set(record);

    // Store in global lookup index for O(1) authentication
    await collections.apiKeyLookupDoc(keyHash).set({
      uid,
      keyId,
      createdAt: now,
    });

    return {
      id: keyId,
      name,
      apiKey: rawKey, // Plaintext key returned ONCE
      prefix: keyPrefix,
      createdAt: now,
      expiresAt,
    };
  }

  /**
   * Lists all API keys for a user (hides keyHash).
   */
  static async listApiKeys(uid: string): Promise<Omit<ApiKeyRecord, 'keyHash'>[]> {
    const snapshot = await collections.apiKeys(uid).orderBy('createdAt', 'desc').get();
    return snapshot.docs.map((doc: QueryDocumentSnapshot) => {
      const data = doc.data() as ApiKeyRecord;
      // Never expose keyHash in API response
      const { keyHash: _unused, ...safeData } = data;
      return safeData;
    });
  }

  /**
   * Revokes an existing API key.
   */
  static async revokeApiKey(uid: string, keyId: string): Promise<boolean> {
    const docRef = collections.apiKeyDoc(uid, keyId);
    const doc = await docRef.get();

    if (!doc.exists) {
      return false;
    }

    const keyData = doc.data() as ApiKeyRecord;

    await docRef.update({
      status: 'revoked',
      updatedAt: new Date().toISOString(),
    });

    // Remove from lookup index
    await collections.apiKeyLookupDoc(keyData.keyHash).delete().catch(() => {});

    return true;
  }

  /**
   * Validates a raw API key and increments usage statistics.
   */
  static async validateRawApiKey(rawKey: string): Promise<{
    uid: string;
    apiKey: ApiKeyRecord;
  } | null> {
    if (!rawKey || typeof rawKey !== 'string') return null;

    const keyHash = hashString(rawKey);

    // Check fast lookup index
    const lookupDoc = await collections.apiKeyLookupDoc(keyHash).get();
    if (!lookupDoc.exists) {
      return null;
    }

    const { uid, keyId } = lookupDoc.data() as { uid: string; keyId: string };
    const keyDoc = await collections.apiKeyDoc(uid, keyId).get();

    if (!keyDoc.exists) {
      return null;
    }

    const apiKey = keyDoc.data() as ApiKeyRecord;

    if (apiKey.status !== 'active') {
      return null;
    }

    // Check expiration if set
    if (apiKey.expiresAt) {
      const expires = new Date(apiKey.expiresAt).getTime();
      if (Date.now() > expires) {
        return null;
      }
    }

    // Update usage asynchronously
    collections.apiKeyDoc(uid, keyId).update({
      usageCount: FieldValue.increment(1),
      lastUsedAt: new Date().toISOString(),
    }).catch(() => {});

    UserService.getUserById(uid).then((user) => {
      if (user) {
        collections.userDoc(uid).update({
          'stats.apiRequestsCount': FieldValue.increment(1),
        }).catch(() => {});
      }
    }).catch(() => {});

    return { uid, apiKey };
  }
}

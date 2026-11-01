import { collections, FieldValue } from '../../config/firestore.ts';
import { UserProfile, UserStats } from '../../types/index.ts';

/**
 * HWX SMS Gateway - User Management Service
 */
export class UserService {
  /**
   * Retrieves or automatically creates a user profile upon verified Firebase token.
   */
  static async getOrCreateUser(decodedToken: {
    uid: string;
    email?: string;
    name?: string;
    picture?: string;
    email_verified?: boolean;
  }): Promise<UserProfile> {
    const userRef = collections.userDoc(decodedToken.uid);
    const doc = await userRef.get();

    const now = new Date().toISOString();

    if (!doc.exists) {
      const defaultStats: UserStats = {
        smsSentCount: 0,
        smsReceivedCount: 0,
        apiRequestsCount: 1,
        activeDevicesCount: 0,
        lastActiveAt: now,
      };

      const newUser: UserProfile = {
        uid: decodedToken.uid,
        email: decodedToken.email || '',
        displayName: decodedToken.name || '',
        photoURL: decodedToken.picture || '',
        emailVerified: !!decodedToken.email_verified,
        status: 'active',
        createdAt: now,
        updatedAt: now,
        stats: defaultStats,
      };

      await userRef.set(newUser);
      return newUser;
    }

    const userData = doc.data() as UserProfile;

    // Asynchronously update activity timestamp
    userRef.update({
      'stats.lastActiveAt': now,
      'stats.apiRequestsCount': FieldValue.increment(1),
      updatedAt: now,
    }).catch((err: unknown) => {
      console.warn(`[UserService] Failed to update stats for ${decodedToken.uid}:`, err);
    });

    return userData;
  }

  /**
   * Retrieves a user profile by UID.
   */
  static async getUserById(uid: string): Promise<UserProfile | null> {
    const doc = await collections.userDoc(uid).get();
    if (!doc.exists) return null;
    return doc.data() as UserProfile;
  }

  /**
   * Increments SMS statistics for a user.
   */
  static async incrementSmsCount(uid: string, direction: 'sent' | 'received'): Promise<void> {
    const field = direction === 'sent' ? 'stats.smsSentCount' : 'stats.smsReceivedCount';
    await collections.userDoc(uid).update({
      [field]: FieldValue.increment(1),
      updatedAt: new Date().toISOString(),
    });
  }

  /**
   * Updates the count of active devices for a user.
   */
  static async updateActiveDevicesCount(uid: string, delta: number): Promise<void> {
    await collections.userDoc(uid).update({
      'stats.activeDevicesCount': FieldValue.increment(delta),
      updatedAt: new Date().toISOString(),
    });
  }
}

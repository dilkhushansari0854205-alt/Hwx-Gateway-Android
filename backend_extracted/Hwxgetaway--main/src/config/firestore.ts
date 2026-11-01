import { getFirestore } from './firebase.ts';
import { FieldValue as FirestoreFieldValue } from 'firebase-admin/firestore';

/**
 * HWX SMS Gateway - Firestore Collection Accessors and Paths
 *
 * Implements the database hierarchy:
 * users/{uid}
 * users/{uid}/apiKeys/{keyId}
 * users/{uid}/devices/{deviceId}
 * users/{uid}/devices/{deviceId}/sims/{simId}
 * users/{uid}/bots/{botId}
 * users/{uid}/smsMessages/{messageId}
 * users/{uid}/smsJobs/{jobId}
 * users/{uid}/webhooks/{webhookId}
 * users/{uid}/webhookDeliveries/{deliveryId}
 */

export const collections = {
  users: () => getFirestore().collection('users'),
  userDoc: (uid: string) => getFirestore().collection('users').doc(uid),

  apiKeys: (uid: string) => getFirestore().collection('users').doc(uid).collection('apiKeys'),
  apiKeyDoc: (uid: string, keyId: string) =>
    getFirestore().collection('users').doc(uid).collection('apiKeys').doc(keyId),

  devices: (uid: string) => getFirestore().collection('users').doc(uid).collection('devices'),
  deviceEnrollmentRequests: (uid: string) => getFirestore().collection('users').doc(uid).collection('deviceEnrollmentRequests'),
  deviceEnrollmentRequestDoc: (uid: string, requestId: string) =>
    getFirestore().collection('users').doc(uid).collection('deviceEnrollmentRequests').doc(requestId),
  deviceBootstrapDoc: (uid: string) =>
    getFirestore().collection('users').doc(uid).collection('deviceMeta').doc('bootstrap'),
  deviceDoc: (uid: string, deviceId: string) =>
    getFirestore().collection('users').doc(uid).collection('devices').doc(deviceId),

  sims: (uid: string, deviceId: string) =>
    getFirestore().collection('users').doc(uid).collection('devices').doc(deviceId).collection('sims'),
  simDoc: (uid: string, deviceId: string, simId: string) =>
    getFirestore().collection('users').doc(uid).collection('devices').doc(deviceId).collection('sims').doc(simId),

  bots: (uid: string) => getFirestore().collection('users').doc(uid).collection('bots'),
  botDoc: (uid: string, botId: string) =>
    getFirestore().collection('users').doc(uid).collection('bots').doc(botId),

  smsMessages: (uid: string) => getFirestore().collection('users').doc(uid).collection('smsMessages'),
  smsMessageDoc: (uid: string, messageId: string) =>
    getFirestore().collection('users').doc(uid).collection('smsMessages').doc(messageId),

  smsJobs: (uid: string) => getFirestore().collection('users').doc(uid).collection('smsJobs'),
  smsJobDoc: (uid: string, jobId: string) =>
    getFirestore().collection('users').doc(uid).collection('smsJobs').doc(jobId),

  webhooks: (uid: string) => getFirestore().collection('users').doc(uid).collection('webhooks'),
  webhookDoc: (uid: string, webhookId: string) =>
    getFirestore().collection('users').doc(uid).collection('webhooks').doc(webhookId),

  webhookDeliveries: (uid: string) =>
    getFirestore().collection('users').doc(uid).collection('webhookDeliveries'),
  webhookDeliveryDoc: (uid: string, deliveryId: string) =>
    getFirestore().collection('users').doc(uid).collection('webhookDeliveries').doc(deliveryId),

  // Lookup indexes for ultra-fast, isolated authentications without full scans
  apiKeyLookups: () => getFirestore().collection('apiKeyLookups'),
  apiKeyLookupDoc: (keyHash: string) => getFirestore().collection('apiKeyLookups').doc(keyHash),

  deviceLookups: () => getFirestore().collection('deviceLookups'),
  deviceLookupDoc: (deviceId: string) => getFirestore().collection('deviceLookups').doc(deviceId),
};

export const FieldValue = FirestoreFieldValue;

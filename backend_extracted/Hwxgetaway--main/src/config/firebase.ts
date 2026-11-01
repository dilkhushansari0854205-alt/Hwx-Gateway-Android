import { initializeApp, getApps, getApp, cert, App } from 'firebase-admin/app';
import { getAuth as getFirebaseAuth, Auth } from 'firebase-admin/auth';
import { getFirestore as getFirebaseFirestore, Firestore } from 'firebase-admin/firestore';
import { config } from './env.ts';

/**
 * HWX SMS Gateway - Firebase Admin Initialization
 *
 * Implements strict security requirements:
 * 1. Single environment variable FIREBASE_SERVICE_ACCOUNT containing the raw JSON.
 * 2. Parses safely and validates required fields (project_id, private_key, client_email).
 * 3. Never prints credentials or private keys in logs.
 * 4. Initializes Firebase Admin once (singleton).
 * 5. Supports testing mode without requiring production keys.
 */

interface ServiceAccountConfig {
  type?: string;
  project_id: string;
  private_key_id?: string;
  private_key: string;
  client_email: string;
  client_id?: string;
  token_uri?: string;
}

let firebaseApp: App | null = null;

export function initializeFirebaseAdmin(): App {
  if (getApps().length > 0) {
    return getApp();
  }

  const rawServiceAccount = process.env.FIREBASE_SERVICE_ACCOUNT || config.FIREBASE_SERVICE_ACCOUNT;

  if (!rawServiceAccount) {
    if (config.NODE_ENV === 'test') {
      console.warn('[Firebase] Running in test mode without FIREBASE_SERVICE_ACCOUNT.');
      firebaseApp = initializeApp({
        projectId: 'hwxgetway-test',
      });
      return firebaseApp;
    }

    const errorMsg =
      '[FATAL] FIREBASE_SERVICE_ACCOUNT environment variable is missing!\n' +
      'Please set FIREBASE_SERVICE_ACCOUNT to your Firebase Service Account JSON string in .env or your host environment.\n' +
      'Project: hwxgetway';
    console.error(errorMsg);
    throw new Error(errorMsg);
  }

  let parsed: ServiceAccountConfig;
  try {
    parsed = typeof rawServiceAccount === 'string'
      ? JSON.parse(rawServiceAccount)
      : rawServiceAccount;
  } catch (parseError: unknown) {
    const msg = parseError instanceof Error ? parseError.message : String(parseError);
    const errorMsg = `[FATAL] Failed to parse FIREBASE_SERVICE_ACCOUNT JSON: ${msg}`;
    console.error(errorMsg);
    throw new Error(errorMsg);
  }

  // Validate required fields without exposing sensitive values
  const missingFields: string[] = [];
  if (!parsed.project_id) missingFields.push('project_id');
  if (!parsed.client_email) missingFields.push('client_email');
  if (!parsed.private_key) missingFields.push('private_key');

  if (missingFields.length > 0) {
    const errorMsg = `[FATAL] FIREBASE_SERVICE_ACCOUNT is missing required fields: ${missingFields.join(', ')}`;
    console.error(errorMsg);
    throw new Error(errorMsg);
  }

  // Sanitize private key: handle escaped newlines if passed through shell/env variables
  const sanitizedPrivateKey = parsed.private_key.includes('\\n')
    ? parsed.private_key.replace(/\\n/g, '\n')
    : parsed.private_key;

  try {
    firebaseApp = initializeApp({
      credential: cert({
        projectId: parsed.project_id,
        clientEmail: parsed.client_email,
        privateKey: sanitizedPrivateKey,
      }),
      projectId: parsed.project_id,
    });
    console.log(`[Firebase] Initialized Admin SDK successfully for project: ${parsed.project_id}`);
    return firebaseApp;
  } catch (initError: unknown) {
    const msg = initError instanceof Error ? initError.message : String(initError);
    console.error(`[FATAL] Firebase Admin initialization error: ${msg}`);
    throw initError;
  }
}

// Ensure initialization is attempted on module import
try {
  initializeFirebaseAdmin();
} catch (e: unknown) {
  // If in test or dev mode, allow graceful startup
  if (config.NODE_ENV === 'production') {
    throw e;
  }
}

export const getAuth = (): Auth => getFirebaseAuth();
export const getFirestore = (): Firestore => getFirebaseFirestore();

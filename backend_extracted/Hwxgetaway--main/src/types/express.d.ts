import { UserProfile, ApiKeyRecord, Device } from './index.ts';

declare global {
  namespace Express {
    interface Request {
      user?: UserProfile;
      apiKey?: ApiKeyRecord;
      device?: Device;
      idempotencyKey?: string;
    }
  }
}

export {};

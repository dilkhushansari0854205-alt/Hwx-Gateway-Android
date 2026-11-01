import request from 'supertest';
import { app } from '../../src/app.ts';

describe('HWX SMS Gateway - REST API Integration Tests', () => {
  describe('GET /health', () => {
    it('should return 200 with service health and metrics', async () => {
      const response = await request(app).get('/health');

      expect(response.status).toBe(200);
      expect(response.body.status).toBe('healthy');
      expect(response.body.service).toBe('HWX SMS Gateway Backend');
      expect(response.body.timestamp).toBeDefined();
    });
  });

  describe('GET /', () => {
    it('should return server metadata and endpoint directories', async () => {
      const response = await request(app).get('/');

      expect(response.status).toBe(200);
      expect(response.body.name).toBe('HWX SMS Gateway API Server');
      expect(response.body.documentation).toBe('/docs');
      expect(response.body.websocketEndpoint).toBe('/ws/device');
    });
  });

  describe('Authentication Enforcement', () => {
    it('should reject unauthenticated requests to /v1/auth/me with 401', async () => {
      const response = await request(app).get('/v1/auth/me');

      expect(response.status).toBe(401);
      expect(response.body.success).toBe(false);
      expect(response.body.error.code).toBe('AUTH_TOKEN_MISSING');
    });

    it('should reject unauthenticated requests to /v1/sms/send with 401', async () => {
      const response = await request(app)
        .post('/v1/sms/send')
        .send({
          bot_id: 'bot_test',
          device_id: 'dev_test',
          to: '+1234567890',
          message: 'Test message',
        });

      expect(response.status).toBe(401);
      expect(response.body.success).toBe(false);
    });

    it('should reject unauthenticated device enrollment requests with 401', async () => {
      const response = await request(app)
        .post('/v1/devices/enrollment-requests')
        .send({ name: 'Test phone', androidVersion: 'Android 15', appVersion: '1.0.0' });

      expect(response.status).toBe(401);
      expect(response.body.error.code).toBe('AUTH_TOKEN_MISSING');
    });

    it('should reject pending enrollment list without a trusted device token', async () => {
      const response = await request(app).get('/v1/devices/enrollment-requests/pending');

      expect(response.status).toBe(401);
      expect(response.body.error.code).toBe('DEVICE_CREDENTIALS_MISSING');
    });

    it('should reject enrollment status polling without Firebase authentication', async () => {
      const response = await request(app).get('/v1/devices/enrollment-requests/enr_test');

      expect(response.status).toBe(401);
      expect(response.body.error.code).toBe('AUTH_TOKEN_MISSING');
    });

    it('should reject invalid device heartbeat with 401', async () => {
      const response = await request(app)
        .post('/v1/devices/dev_fake/heartbeat')
        .send({ batteryLevel: 80 });

      expect(response.status).toBe(401);
      expect(response.body.success).toBe(false);
    });

    it('should reject unauthenticated SIM authorization patch with 401', async () => {
      const response = await request(app)
        .patch('/v1/devices/dev_1/sims/sim_1')
        .send({ isVerified: true });

      expect(response.status).toBe(401);
      expect(response.body.success).toBe(false);
    });

    it('should reject unauthenticated bot creation with 401', async () => {
      const response = await request(app)
        .post('/v1/bots')
        .send({
          name: 'Test Bot',
          assignedDeviceId: 'dev_1',
          assignedSimId: 'sim_1',
        });

      expect(response.status).toBe(401);
      expect(response.body.success).toBe(false);
    });
  });

  describe('Route Not Found (404)', () => {
    it('should return structured 404 error for non-existent endpoints', async () => {
      const response = await request(app).get('/v1/non_existent_route');

      expect(response.status).toBe(404);
      expect(response.body.success).toBe(false);
      expect(response.body.error.code).toBe('ROUTE_NOT_FOUND');
    });
  });
});

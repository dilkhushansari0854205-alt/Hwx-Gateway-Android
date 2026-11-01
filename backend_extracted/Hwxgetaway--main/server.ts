import http from 'http';
import { app } from './src/app.ts';
import { config } from './src/config/env.ts';
import { DeviceSocketManager } from './src/modules/websocket/deviceSocket.ts';

const PORT = config.PORT || 3000;

async function bootstrap() {
  // In development mode, mount Vite middleware to serve static/doc assets if needed
  if (config.NODE_ENV !== 'production') {
    try {
      const { createServer } = await import('vite');
      const vite = await createServer({
        server: { middlewareMode: true },
        appType: 'spa',
      });
      app.use(vite.middlewares);
      console.log('[Server] Vite middleware mounted for dev environment');
    } catch (err) {
      console.warn('[Server] Notice: Running without Vite middleware:', err);
    }
  }

  const server = http.createServer(app);

  // Initialize WebSocket device communication server on /ws/device
  const socketManager = DeviceSocketManager.getInstance();
  socketManager.init(server);

  server.listen(PORT, '0.0.0.0', () => {
    console.log(`===================================================`);
    console.log(`🚀 HWX SMS Gateway Backend running on port ${PORT}`);
    console.log(`🌍 Environment: ${config.NODE_ENV}`);
    console.log(`📖 Swagger OpenAPI Docs: http://0.0.0.0:${PORT}/docs`);
    console.log(`📡 WebSocket Device Gateway: ws://0.0.0.0:${PORT}/ws/device`);
    console.log(`🩺 Health Check: http://0.0.0.0:${PORT}/health`);
    console.log(`===================================================`);
  });

  // Graceful Shutdown handling for Render / Docker / Cloud Run
  const shutdown = (signal: string) => {
    console.log(`[Shutdown] Received ${signal}. Starting graceful termination...`);
    socketManager.close();

    server.close(() => {
      console.log('[Shutdown] HTTP and WebSocket servers closed cleanly.');
      process.exit(0);
    });

    // Force shutdown if connections do not terminate in 10s
    setTimeout(() => {
      console.error('[Shutdown] Forced termination after 10s timeout.');
      process.exit(1);
    }, 10000);
  };

  process.on('SIGTERM', () => shutdown('SIGTERM'));
  process.on('SIGINT', () => shutdown('SIGINT'));
}

bootstrap().catch((err) => {
  console.error('[FATAL] Bootstrap error:', err);
  process.exit(1);
});

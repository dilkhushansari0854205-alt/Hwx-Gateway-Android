process.env.NODE_ENV = 'test';
process.env.PORT = '3001';
process.env.FIREBASE_SERVICE_ACCOUNT = JSON.stringify({
  type: 'service_account',
  project_id: 'hwxgetway-test',
  private_key_id: 'test_key_id',
  private_key: '[REDACTED-SECRET]\n',
  client_email: 'test@hwxgetway.iam.gserviceaccount.com',
  client_id: '123456789',
  token_uri: 'https://oauth2.googleapis.com/token',
});

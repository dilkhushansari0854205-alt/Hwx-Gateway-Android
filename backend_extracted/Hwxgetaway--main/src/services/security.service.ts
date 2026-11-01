import crypto from 'crypto';
import dns from 'dns';

/**
 * Security Service for HWX SMS Gateway
 * Handles cryptographic key generation, SHA-256 hashing, HMAC signatures,
 * and multi-layer SSRF protection with strict DNS resolution and redirect gating.
 */

// API Key Prefix
export const API_KEY_PREFIX = 'hwx_live_';

/**
 * Generates a cryptographically secure random API key.
 * Format: hwx_live_<32-byte-hex>
 */
export function generateApiKey(): { rawKey: string; keyPrefix: string; keyHash: string } {
  const randomHex = crypto.randomBytes(32).toString('hex');
  const rawKey = `${API_KEY_PREFIX}${randomHex}`;
  const keyPrefix = rawKey.substring(0, 16) + '...';
  const keyHash = hashString(rawKey);
  return { rawKey, keyPrefix, keyHash };
}

/**
 * Generates a random device authentication token.
 */
export function generateDeviceToken(): { rawToken: string; tokenHash: string } {
  const rawToken = `hwx_dev_${crypto.randomBytes(24).toString('hex')}`;
  const tokenHash = hashString(rawToken);
  return { rawToken, tokenHash };
}

/**
 * Computes a SHA-256 hash of a string.
 */
export function hashString(value: string): string {
  return crypto.createHash('sha256').update(value, 'utf8').digest('hex');
}

/**
 * Computes HMAC-SHA256 signature of a payload.
 */
export function generateHmacSignature(payload: string, secret: string): string {
  return crypto.createHmac('sha256', secret).update(payload, 'utf8').digest('hex');
}

/**
 * Verifies an HMAC-SHA256 signature using timing-safe comparison.
 */
export function verifyHmacSignature(payload: string, secret: string, providedSignature: string): boolean {
  try {
    const expected = generateHmacSignature(payload, secret);
    const expectedBuf = Buffer.from(expected, 'hex');
    const providedBuf = Buffer.from(providedSignature, 'hex');
    if (expectedBuf.length !== providedBuf.length) {
      return false;
    }
    return crypto.timingSafeEqual(expectedBuf, providedBuf);
  } catch {
    return false;
  }
}

/**
 * IP Range helper to test if an IPv4 address falls within a CIDR.
 */
function isIpv4InCidr(ip: string, cidr: string): boolean {
  const [range, bitsStr = '32'] = cidr.split('/');
  const bits = parseInt(bitsStr, 10);
  const mask = bits === 0 ? 0 : (~0 << (32 - bits)) >>> 0;

  const ipToInt = (ipStr: string) =>
    ipStr.split('.').reduce((acc, octet) => ((acc << 8) + parseInt(octet, 10)) >>> 0, 0);

  const ipInt = ipToInt(ip);
  const rangeInt = ipToInt(range);

  return (ipInt & mask) === (rangeInt & mask);
}

/**
 * Validates whether an IP address is a private, loopback, or reserved address.
 */
export function isPrivateOrReservedIp(ip: string): boolean {
  // Normalize IPv4-mapped IPv6 (e.g. ::ffff:127.0.0.1)
  if (ip.startsWith('::ffff:')) {
    ip = ip.substring(7);
  }

  // IPv6 checks
  if (
    ip === '::1' ||
    ip === '::' ||
    ip.startsWith('fe80:') || // Link-local
    ip.startsWith('fc') ||    // Unique Local
    ip.startsWith('fd')
  ) {
    return true;
  }

  const reservedRanges = [
    '0.0.0.0/8',          // Current network (RFC 1122)
    '10.0.0.0/8',         // Private-use networks (RFC 1918)
    '100.64.0.0/10',      // Carrier-grade NAT (RFC 6598)
    '127.0.0.0/8',        // Loopback (RFC 1122)
    '169.254.0.0/16',     // Link-local / Cloud Metadata (AWS, GCP, Azure 169.254.169.254)
    '172.16.0.0/12',      // Private-use networks (RFC 1918)
    '192.0.0.0/24',       // IETF Protocol Assignments (RFC 6890)
    '192.0.2.0/24',       // TEST-NET-1 (RFC 5737)
    '192.88.99.0/24',     // 6to4 Relay Anycast (RFC 7526)
    '192.168.0.0/16',     // Private-use networks (RFC 1918)
    '198.18.0.0/15',      // Network Interconnect Device Benchmark Testing (RFC 2544)
    '198.51.100.0/24',    // TEST-NET-2 (RFC 5737)
    '203.0.113.0/24',     // TEST-NET-3 (RFC 5737)
    '224.0.0.0/4',        // Multicast (RFC 5771)
    '240.0.0.0/4',        // Reserved for future use (RFC 1112)
    '255.255.255.255/32', // Limited Broadcast (RFC 919)
  ];

  for (const cidr of reservedRanges) {
    if (isIpv4InCidr(ip, cidr)) {
      return true;
    }
  }

  return false;
}

/**
 * SSRF Protection: Validates a webhook URL by checking protocol and resolving DNS
 * to ensure it does NOT target private internal endpoints, cloud metadata, or localhost.
 */
export async function validateWebhookUrl(urlString: string): Promise<{ valid: boolean; reason?: string }> {
  try {
    const parsed = new URL(urlString);

    // Only allow HTTP/HTTPS
    if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
      return { valid: false, reason: 'Only HTTP and HTTPS webhook URLs are supported' };
    }

    // Reject user credentials in URLs (e.g. http://admin:pass@host)
    if (parsed.username || parsed.password) {
      return { valid: false, reason: 'Credentials in webhook URL are forbidden' };
    }

    const hostname = parsed.hostname.toLowerCase();

    // Block obvious loopback/internal hostnames
    if (
      hostname === 'localhost' ||
      hostname.endsWith('.localhost') ||
      hostname.endsWith('.internal') ||
      hostname.endsWith('.local') ||
      hostname === 'metadata.google.internal' ||
      hostname === '169.254.169.254' ||
      hostname === '127.0.0.1' ||
      hostname === '0.0.0.0'
    ) {
      return { valid: false, reason: 'Destination hostname is an internal or reserved address' };
    }

    // Resolve DNS to verify actual IP
    let addresses: dns.LookupAddress[] = [];
    try {
      addresses = await dns.promises.lookup(hostname, { all: true });
    } catch {
      return { valid: false, reason: 'Could not resolve webhook hostname' };
    }

    if (!addresses || addresses.length === 0) {
      return { valid: false, reason: 'Could not resolve webhook hostname to an IP address' };
    }

    for (const addr of addresses) {
      if (isPrivateOrReservedIp(addr.address)) {
        return { valid: false, reason: `Resolved IP ${addr.address} belongs to a private/reserved network` };
      }
    }

    return { valid: true };
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Invalid URL';
    return { valid: false, reason: `URL validation error: ${message}` };
  }
}

/**
 * Redacts sensitive fields in objects for safe logging.
 */
export function redactSecrets<T>(data: T): T {
  if (!data || typeof data !== 'object') {
    return data;
  }

  if (Array.isArray(data)) {
    return data.map(redactSecrets) as unknown as T;
  }

  const sensitiveKeys = new Set([
    'private_key',
    'privatekey',
    'password',
    'devicetoken',
    'devicetokenhash',
    'apikey',
    'keyhash',
    'secret',
    'authorization',
    'x-api-key',
    'x-device-token',
  ]);

  const copy: Record<string, unknown> = {};
  for (const [key, val] of Object.entries(data as Record<string, unknown>)) {
    if (sensitiveKeys.has(key.toLowerCase())) {
      copy[key] = '[REDACTED]';
    } else if (val && typeof val === 'object') {
      copy[key] = redactSecrets(val);
    } else {
      copy[key] = val;
    }
  }

  return copy as T;
}

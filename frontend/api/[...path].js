const BODY_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

function requestBody(req) {
  if (!BODY_METHODS.has(req.method) || req.body == null) return undefined;
  if (Buffer.isBuffer(req.body) || typeof req.body === 'string') return req.body;
  return JSON.stringify(req.body);
}

export default async function handler(req, res) {
  const origin = process.env.BACKEND_ORIGIN?.replace(/\/$/, '');
  if (!origin || !origin.startsWith('https://')) {
    return res.status(500).json({ message: 'BACKEND_ORIGIN is not configured' });
  }

  const headers = { ...req.headers };
  delete headers.host;
  delete headers.connection;
  delete headers['content-length'];

  try {
    const upstream = await fetch(`${origin}${req.url}`, {
      method: req.method,
      headers,
      body: requestBody(req),
      redirect: 'manual',
      signal: AbortSignal.timeout(25000),
    });

    upstream.headers.forEach((value, key) => {
      if (!['content-encoding', 'content-length', 'transfer-encoding', 'set-cookie'].includes(key)) {
        res.setHeader(key, value);
      }
    });

    const fallbackCookie = upstream.headers.get('set-cookie');
    const cookies = upstream.headers.getSetCookie?.() ?? (fallbackCookie ? [fallbackCookie] : []);
    if (cookies.length) res.setHeader('set-cookie', cookies);

    res.setHeader('cache-control', 'private, no-store');
    return res.status(upstream.status).send(Buffer.from(await upstream.arrayBuffer()));
  } catch (error) {
    console.error('Backend proxy failed:', error);
    return res.status(502).json({ message: 'Backend is unavailable' });
  }
}

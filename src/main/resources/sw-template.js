/* Service Worker template - placeholders replaced at runtime */
const APP_VERSION = '__APP_VERSION__';
const CACHE_PREFIX = 'pi-reports-';
const CACHE_NAME = `${CACHE_PREFIX}${APP_VERSION}`;
const PRECACHE_URLS = [
  '/',
  `/manifest.json?v=${APP_VERSION}`,
  `/css/main.css?v=${APP_VERSION}`,
  `/js/main.js?v=${APP_VERSION}`,
  '/favicon.svg',
  '/favicon.ico',
  '/img/favicon-32x32.png',
  '/img/favicon-16x16.png'
];
const STATIC_FILE_REGEX = /\.(?:css|js|png|jpg|jpeg|gif|svg|json|ico|webp|woff2?|ttf)$/i;

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(PRECACHE_URLS))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    const cacheNames = await caches.keys();
    await Promise.all(
      cacheNames
        .filter((name) => name.startsWith(CACHE_PREFIX) && name !== CACHE_NAME)
        .map((name) => caches.delete(name))
    );
    await self.clients.claim();
    const clients = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
    clients.forEach((client) => client.postMessage({ type: 'APP_VERSION', version: APP_VERSION }));
  })());
});

self.addEventListener('message', (event) => {
  if (!event.data) {
    return;
  }
  if (event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }
});

const networkFirst = async (request) => {
  const cache = await caches.open(CACHE_NAME);
  try {
    const response = await fetch(request);
    if (response && response.ok) {
      cache.put(request, response.clone());
    }
    return response;
  } catch (error) {
    const cached = await cache.match(request);
    if (cached) {
      return cached;
    }
    throw error;
  }
};

const cacheFirst = async (request) => {
  const cache = await caches.open(CACHE_NAME);
  const cached = await cache.match(request);
  if (cached) {
    return cached;
  }
  const response = await fetch(request);
  if (response && response.ok) {
    cache.put(request, response.clone());
  }
  return response;
};

const handleNavigation = async (request) => {
  const cache = await caches.open(CACHE_NAME);
  try {
    const response = await fetch(request);
    if (response && response.ok) {
      cache.put('/', response.clone());
    }
    return response;
  } catch (error) {
    const fallback = await cache.match('/') || await cache.match('/index.html');
    if (fallback) {
      return fallback;
    }
    return new Response('Offline', { status: 503, statusText: 'Offline' });
  }
};

self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') {
    return;
  }

  const url = new URL(event.request.url);

  // Allow other origins to pass through untouched
  if (url.origin !== self.location.origin) {
    return;
  }

  if (url.pathname === '/sw.js' || url.pathname.startsWith('/sw.js?')) {
    return;
  }

  if (url.pathname.startsWith('/api/')) {
    event.respondWith(
      fetch(event.request)
        .catch(() => new Response(
          JSON.stringify({ error: 'Offline - API unavailable' }),
          { headers: { 'Content-Type': 'application/json' } }
        ))
    );
    return;
  }

  if (event.request.mode === 'navigate' || event.request.destination === 'document') {
    event.respondWith(handleNavigation(event.request));
    return;
  }

  if (STATIC_FILE_REGEX.test(url.pathname)) {
    event.respondWith(networkFirst(event.request));
    return;
  }

  event.respondWith(cacheFirst(event.request));
});

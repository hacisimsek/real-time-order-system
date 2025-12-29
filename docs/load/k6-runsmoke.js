import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const token = `Bearer ${__ENV.DEV_TOKEN || ''}`;
const skuPool = (__ENV.SKU_POOL || 'ABC-001')
  .split(',')
  .map((sku) => sku.trim())
  .filter(Boolean);

// Adjustable weights for sub-scenarios
const orderRate = Number(__ENV.ORDER_RATE || 150);       // req/s
const inventoryRate = Number(__ENV.INVENTORY_RATE || 50);
const reportingRate = Number(__ENV.REPORTING_RATE || 40);

export const options = {
  scenarios: {
    orders: {
      executor: 'constant-arrival-rate',
      rate: orderRate,
      timeUnit: '1s',
      duration: __ENV.DURATION || '5m',
      preAllocatedVUs: 100,
      maxVUs: 200,
    },
    inventory: {
      executor: 'constant-arrival-rate',
      rate: inventoryRate,
      timeUnit: '1s',
      duration: __ENV.DURATION || '5m',
      preAllocatedVUs: 40,
      maxVUs: 100,
      startTime: '0s',
    },
    reporting: {
      executor: 'constant-arrival-rate',
      rate: reportingRate,
      timeUnit: '1s',
      duration: __ENV.DURATION || '5m',
      preAllocatedVUs: 40,
      maxVUs: 100,
      startTime: '0s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],        // <1% failure
    http_req_duration: ['p(95)<500'],      // p95 under 500ms
  },
};

export default function () {
  const scenario = exec.scenario.name || __ENV.K6_SCENARIO || 'orders';
  switch (scenario) {
    case 'orders':
      return hitOrder();
    case 'inventory':
      return hitInventory();
    case 'reporting':
      return hitReporting();
  }
}

function pickSku() {
  if (!skuPool.length) return 'ABC-001';
  return skuPool[Math.floor(Math.random() * skuPool.length)];
}

function hitOrder() {
  const body = JSON.stringify({
    customerId: `LOAD-${randomIntBetween(1000, 9999)}`,
    amountCents: randomIntBetween(1500, 4500),
    currency: 'TRY',
    items: [{ sku: pickSku(), qty: 1 }],
  });
  const res = http.post('http://localhost:8081/orders', body, {
    headers: {
      'Content-Type': 'application/json',
      Authorization: token,
    },
  });
  check(res, { 'order 200': (r) => r.status === 200 });
}

function hitInventory() {
  const delta = randomIntBetween(1, 3);
  const sku = pickSku();
  const res = http.put(
    `http://localhost:8083/inventory/${sku}/adjust`,
    JSON.stringify({ delta, reason: 'load' }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: token,
      },
    },
  );
  check(res, { 'inventory 200': (r) => r.status === 200 });
}

function hitReporting() {
  const pick = Math.random();
  let url;
  if (pick < 0.5) {
    url = 'http://localhost:8084/reports/orders?period=DAILY&refresh=true';
  } else if (pick < 0.8) {
    url = 'http://localhost:8084/reports/orders/totals?period=DAILY';
  } else {
    url = 'http://localhost:8084/reports/orders/top-customers?limit=5';
  }
  const res = http.get(url, { headers: { Authorization: token } });
  check(res, { 'report 200': (r) => r.status === 200 });
}

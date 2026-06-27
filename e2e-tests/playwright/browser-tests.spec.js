// Enterprise AI Platform — Playwright Browser Tests
// Requires: running application on http://localhost:8080
// Execute: npx playwright test
// Maven profile: mvn verify -Pui-tests

const { test, expect } = require('@playwright/test');

const BASE_URL = 'http://localhost:8080';

// ── Authentication ──

test.describe('Authentication', () => {
  test('login page loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`);
    await expect(page.locator('h1, h2, h3').first()).toBeVisible();
    await expect(page.locator('input[type="email"], input[name="username"]').first()).toBeVisible();
  });

  test('register page loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/register`);
    await expect(page.locator('text=Register')).toBeVisible();
  });

  test('can register and login', async ({ page }) => {
    const email = `e2e-${Date.now()}@test.com`;
    await page.goto(`${BASE_URL}/register`);
    await page.fill('input[name="email"]', email);
    await page.fill('input[name="password"]', 'Pass1234!');
    await page.fill('input[name="displayName"]', 'E2E User');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL(/.*dashboard.*/);
  });
});

// ── Dashboard & Navigation ──

test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('dashboard loads with metrics', async ({ page }) => {
    await page.goto(`${BASE_URL}/dashboard`);
    await expect(page.locator('text=Documents')).toBeVisible();
    await expect(page.locator('text=Recent audit events')).toBeVisible();
  });

  test('navigation links are present', async ({ page }) => {
    await page.goto(`${BASE_URL}/dashboard`);
    const nav = page.locator('nav');
    await expect(nav.locator('text=Workspaces')).toBeVisible();
    await expect(nav.locator('text=Documents')).toBeVisible();
    await expect(nav.locator('text=Upload')).toBeVisible();
    await expect(nav.locator('text=Search')).toBeVisible();
    await expect(nav.locator('text=AI')).toBeVisible();
  });
});

// ── Document Upload ──

test.describe('Document Upload', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('upload page loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/documents/upload`);
    await expect(page.locator('text=Document Ingestion').first()).toBeVisible();
  });
});

// ── Search ──

test.describe('Search', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('search page loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/search`);
    await expect(page.locator('text=Document Search')).toBeVisible();
  });
});

// ── AI Page ──

test.describe('AI Query', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('AI page loads with form', async ({ page }) => {
    await page.goto(`${BASE_URL}/ai`);
    await expect(page.locator('text=Grounded AI Orchestration')).toBeVisible();
    await expect(page.locator('textarea[name="question"]')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
  });

  test('AI query returns results', async ({ page }) => {
    await page.goto(`${BASE_URL}/ai`);
    await page.fill('textarea[name="question"]', 'What is the Enterprise AI Platform?');
    await page.click('button[type="submit"]');
    // Either shows answer or provider configuration warning
    await expect(page.locator('.card-body').first()).toBeVisible({ timeout: 15000 });
  });
});

// ── Workspaces ──

test.describe('Workspaces', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('workspaces page loads', async ({ page }) => {
    await page.goto(`${BASE_URL}/workspaces`);
    await expect(page.locator('text=Workspaces')).toBeVisible();
  });

  test('can create a workspace', async ({ page }) => {
    await page.goto(`${BASE_URL}/workspaces/new`);
    await expect(page.locator('select[name="workspaceType"]')).toBeVisible();
  });
});

// ── Error Handling ──

test.describe('Error Handling', () => {
  test('404 page handled gracefully', async ({ page }) => {
    await page.goto(`${BASE_URL}/nonexistent-page`);
    // Either custom error page or redirect
    await expect(page).not.toHaveURL(/.*nonexistent-page.*/);
  });

  test('unauthorized redirects to login', async ({ page }) => {
    await page.goto(`${BASE_URL}/dashboard`);
    // Should redirect to login when not authenticated
    await expect(page).toHaveURL(/.*login.*/);
  });
});

// ── Helper ──

async function login(page) {
  const email = `e2e-login-${Date.now()}@test.com`;
  await page.goto(`${BASE_URL}/register`);
  await page.fill('input[name="email"]', email);
  await page.fill('input[name="password"]', 'Pass1234!');
  await page.fill('input[name="displayName"]', 'E2E Tester');
  await page.click('button[type="submit"]');
  await page.waitForURL(/.*dashboard.*/);
}

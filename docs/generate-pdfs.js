const puppeteer = require('puppeteer');
const path = require('path');

async function generatePDF(htmlPath, pdfPath, title) {
  console.log(`Generating: ${title}`);
  const browser = await puppeteer.launch({ headless: 'new' });
  const page = await browser.newPage();

  const absolutePath = `file://${path.resolve(htmlPath)}`;
  await page.goto(absolutePath, { waitUntil: 'networkidle0', timeout: 30000 });

  await page.pdf({
    path: pdfPath,
    format: 'A4',
    margin: { top: '25mm', bottom: '25mm', left: '20mm', right: '20mm' },
    displayHeaderFooter: true,
    headerTemplate: `<div style="font-size:8pt;color:#666;text-align:center;width:100%;">${title}</div>`,
    footerTemplate: '<div style="font-size:8pt;color:#666;text-align:center;width:100%;">Page <span class="pageNumber"></span></div>',
    printBackground: true,
    scale: 0.95,
  });

  await browser.close();
  console.log(`Done: ${pdfPath}`);
}

(async () => {
  const docsDir = __dirname;

  await generatePDF(
    path.join(docsDir, 'architecture-manual.html'),
    path.join(docsDir, 'Enterprise-AI-Platform-Architecture-Manual.pdf'),
    'Enterprise AI Platform — Architecture Manual'
  );

  await generatePDF(
    path.join(docsDir, 'ai-fundamentals-guide.html'),
    path.join(docsDir, 'Enterprise-AI-Platform-AI-LLM-Fundamentals-Guide.pdf'),
    'AI & LLM Fundamentals — A Consultant\'s Guide'
  );

  console.log('\nBoth PDFs generated successfully.');
})().catch(err => {
  console.error('Error:', err);
  process.exit(1);
});

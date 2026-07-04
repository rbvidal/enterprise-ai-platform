const marked = require('marked');
const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

marked.setOptions({ breaks: false, gfm: true });



const jobs = [
  { md: 'Architecture-and-Engineering-Handbook.md', pdf: 'Enterprise-AI-Platform-Architecture-and-Engineering-Handbook.pdf', title: 'Enterprise AI Platform — Architecture & Engineering Handbook' },
  { md: 'Architecture-Decision-Records.md', pdf: 'Enterprise-AI-Platform-Architecture-Decision-Records.pdf', title: 'Enterprise AI Platform — Architecture Decision Records' },
  { md: 'Developer-Guide.md', pdf: 'Enterprise-AI-Platform-Developer-Guide.pdf', title: 'Enterprise AI Platform — Developer Guide' },
];

(async () => {
  const docsDir = __dirname;
  const browser = await puppeteer.launch({ headless: 'new' });

  for (const job of jobs) {
    const mdPath = path.join(docsDir, job.md);
    const pdfPath = path.join(docsDir, job.pdf);

    if (!fs.existsSync(mdPath)) {
      console.log(`SKIP: ${job.md} not found`);
      continue;
    }

    console.log(`Reading ${job.md}...`);
    const md = fs.readFileSync(mdPath, 'utf-8');
    const body = marked.parse(md);

    const html = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>${job.title}</title>
<style>
  @page { margin: 25mm 20mm; }
  body { font-family: "Segoe UI", "Helvetica Neue", Arial, sans-serif; font-size: 10.5pt; line-height: 1.65; color: #1a1a1a; max-width: 100%; }
  h1 { font-size: 22pt; margin-top: 0; page-break-before: avoid; }
  h2 { font-size: 14pt; margin-top: 2em; page-break-after: avoid; }
  h3 { font-size: 11.5pt; margin-top: 1.5em; page-break-after: avoid; }
  h4 { font-size: 10.5pt; margin-top: 1.2em; }
  p { margin: 0.6em 0; orphans: 3; widows: 3; }
  code { font-family: "Cascadia Code", "Fira Code", Consolas, monospace; font-size: 9.5pt; background: #f4f4f4; padding: 1px 4px; border-radius: 3px; }
  pre { background: #f4f4f4; padding: 10px 14px; border-radius: 4px; overflow-x: auto; font-size: 9pt; line-height: 1.4; }
  pre code { background: none; padding: 0; }
  blockquote { border-left: 3px solid #5a7; margin: 1em 0; padding: 0.6em 1em; background: #f8faf8; color: #333; page-break-inside: avoid; }
  table { border-collapse: collapse; width: 100%; margin: 1em 0; font-size: 9.5pt; page-break-inside: avoid; }
  th, td { border: 1px solid #ddd; padding: 6px 10px; text-align: left; }
  th { background: #f5f5f5; font-weight: 600; }
  hr { border: none; border-top: 1px solid #ddd; margin: 2em 0; }
  a { color: #0366d6; text-decoration: none; }
  strong { color: #111; }
  img { max-width: 100%; height: auto; }
  em { color: #444; }
  ul, ol { padding-left: 1.4em; }
  li { margin: 0.2em 0; }
  .page-break { page-break-before: always; }
</style>
</head>
<body>${body}</body>
</html>`;

    const htmlPath = path.join(docsDir, `_${job.pdf}.html`);
    fs.writeFileSync(htmlPath, html);

    const page = await browser.newPage();
    await page.goto(`file://${htmlPath}`, { waitUntil: 'networkidle0', timeout: 30000 });
    await page.pdf({
      path: pdfPath, format: 'A4',
      margin: { top: '25mm', bottom: '25mm', left: '20mm', right: '20mm' },
      displayHeaderFooter: true,
      headerTemplate: `<div style="font-size:8pt;color:#666;text-align:center;width:100%;">${job.title}</div>`,
      footerTemplate: '<div style="font-size:8pt;color:#666;text-align:center;width:100%;">Page <span class="pageNumber"></span></div>',
      printBackground: true, scale: 0.95,
    });
    await page.close();
    fs.unlinkSync(htmlPath);
    console.log(`Done: ${pdfPath}`);
  }

  await browser.close();
  console.log('\nAll PDFs generated successfully.');
})().catch(err => { console.error('Error:', err); process.exit(1); });

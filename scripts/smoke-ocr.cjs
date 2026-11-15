// Run: node scripts/smoke-ocr.cjs [PDF path]
// Creates a uniquely named test account and document in the local application.
const fs = require('node:fs');
const { randomUUID } = require('node:crypto');
const base = 'http://localhost/api';
const searchBase = 'http://localhost:9200';
async function main() {
  const credentials = { username: 'ocr-' + randomUUID().slice(0, 12), password: randomUUID() };
  const headers = { 'Content-Type': 'application/json', Origin: 'http://localhost' };
  let response = await fetch(base + '/users', { method: 'POST', headers, body: JSON.stringify(credentials) });
  if (response.status !== 201) throw Error('Registration: ' + response.status + ' ' + await response.text());
  console.log('Registration: 201 (' + credentials.username + ')');
  response = await fetch(base + '/users', { method: 'POST', headers, body: JSON.stringify(credentials) });
  if (response.status !== 409) throw Error('Duplicate registration: ' + response.status);
  console.log('Duplicate registration: 409');
  response = await fetch(base + '/sessions', { method: 'POST', headers, body: JSON.stringify(credentials) });
  if (!response.ok) throw Error('Login: ' + response.status);
  const { token } = await response.json();
  console.log('Login: 200');
  const form = new FormData();
  const pdfBytes = fs.readFileSync(process.argv[2] || 'hello-world.pdf');
  form.append('title', 'OCR smoke test');
  form.append('file', new Blob([pdfBytes], { type: 'application/pdf' }), 'sample.pdf');
  const auth = { Authorization: 'Bearer ' + token, Origin: 'http://localhost' };
  response = await fetch(base + '/document', { method: 'POST', headers: auth, body: form });
  if (response.status !== 201) throw Error('Upload: ' + response.status + ' ' + await response.text());
  const document = await response.json();
  console.log('Upload: 201, documentId=' + document.id);
  const download = await fetch(base + '/document/' + document.id + '/data', { headers: auth });
  if (!download.ok) throw Error('Download from storage: ' + download.status);
  if (!Buffer.from(await download.arrayBuffer()).equals(pdfBytes)) throw Error('Stored PDF differs from uploaded PDF');
  console.log('MinIO download: matches uploaded PDF');
  for (let attempt = 0; attempt < 45; attempt++) {
    const result = await fetch(searchBase + '/documents/_doc/' + document.id);
    if (!result.ok && result.status !== 404) throw Error('Read search document: ' + result.status);
    const current = await result.json();
    const text = current._source?.text;
    if (text && text.trim()) {
      const search = await fetch(base + '/document?query=' + encodeURIComponent(text.trim().split(/\s+/)[0]), { headers: auth });
      if (!search.ok) throw Error('OCR search: ' + search.status);
      // Search visibility can lag behind the realtime document GET until refresh.
      if (!(await search.json()).some(item => item.id === document.id)) {
        await new Promise(resolve => setTimeout(resolve, 2000));
        continue;
      }
      console.log('OCR saved in Elasticsearch: ' + JSON.stringify(text));
      console.log('OCR search: passed');
      return;
    }
    await new Promise(resolve => setTimeout(resolve, 2000));
  }
  throw Error('OCR did not complete within 90 seconds');
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });

const fs = require('fs');
const path = require('path');

const apiBaseUrl = process.env.VITE_API_URL || process.env.EPET_API_BASE_URL || '';
const outputPath = path.join(__dirname, 'config.js');

fs.writeFileSync(
    outputPath,
    [
        `window.VITE_API_URL = ${JSON.stringify(apiBaseUrl.replace(/\/$/, ''))};`,
        `window.EPET_API_BASE_URL = window.VITE_API_URL;`,
        ''
    ].join('\n'),
    'utf8'
);

console.log(`Wrote ${outputPath}`);

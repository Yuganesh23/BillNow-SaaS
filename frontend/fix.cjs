const fs = require('fs');
let c = fs.readFileSync('src/pages/CreateInvoice.jsx', 'utf8');

c = c.replace('             </div>\n          </div>\n          \n          <div className=\"w-full md:w-1/3 space-y-3\">', '             </div>\n          \n          <div className=\"w-full md:w-1/3 space-y-3\">');

c = c.replace('onChange={(e) => setDiscountAmount(e.target.value)}\n                  />', 'onChange={(e) => setDiscountAmount(e.target.value)}\n                  className=\"mt-1 block w-full max-w-xs border-slate-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm\"\n                  />');

fs.writeFileSync('src/pages/CreateInvoice.jsx', c);


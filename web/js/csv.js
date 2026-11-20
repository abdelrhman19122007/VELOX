// Reads a menu exported from Excel as "CSV UTF-8": name_ar, name_en, price, section, size, description, description_en.
// Quoted fields, commas and new lines inside quotes, and Excel's BOM are handled. Bad rows are reported, not sent.

export const MENU_TEMPLATE = [
  'name_ar,name_en,price,section,size,description',
  'كشري كبير,Large koshary,45,MEALS,MEDIUM,بالعدس والصلصة والدقة',
  'رز بلبن,Rice pudding,20,DESSERTS,SMALL,',
  'بيبسي كانز,Pepsi can,15,DRINKS,SMALL,',
  'أنبوبة بوتاجاز,Gas cylinder,180,GAS,LARGE,محتاجة توكتوك',
].join('\r\n');

function rowsOf(text) {
  const rows = [];
  let row = [], cell = '', quoted = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (quoted) {
      if (c === '"' && text[i + 1] === '"') { cell += '"'; i++; } else if (c === '"') quoted = false; else cell += c;
    } else if (c === '"') quoted = true;
    else if (c === ',' || c === ';') { row.push(cell); cell = ''; }
    else if (c === '\n' || c === '\r') {
      if (c === '\r' && text[i + 1] === '\n') i++;
      row.push(cell); rows.push(row); row = []; cell = '';
    } else cell += c;
  }
  if (cell !== '' || row.length) { row.push(cell); rows.push(row); }
  return rows.filter((r) => r.some((x) => x.trim() !== ''));
}

export function parseCsv(text) {
  const all = rowsOf(text.replace(/^﻿/, ''));
  const errors = [];
  if (!all.length) return { rows: [], errors: ['الملف فاضي'] };
  const head = all[0].map((h) => h.trim().toLowerCase());
  const col = (name) => head.indexOf(name);
  const need = ['name_ar', 'name_en', 'price'];
  const missing = need.filter((n) => col(n) < 0);
  if (missing.length) return { rows: [], errors: [`أعمدة ناقصة: ${missing.join(', ')}`] };
  const rows = [];
  all.slice(1).forEach((r, i) => {
    const get = (n) => (col(n) >= 0 ? (r[col(n)] || '').trim() : '');
    const price = Number(get('price').replace(/[^\d.]/g, ''));
    const section = (get('section') || 'OTHER').toUpperCase().replace(/\s+/g, '_');
    const size = (get('size') || 'MEDIUM').toUpperCase();
    const line = i + 2;
    if (!get('name_ar') || !get('name_en')) return errors.push(`سطر ${line}: الاسم ناقص`);
    if (!(price >= 1)) return errors.push(`سطر ${line}: السعر غلط`);
    if (!/^[A-Z_]{2,20}$/.test(section)) return errors.push(`سطر ${line}: القسم لازم حروف إنجليزي`);
    if (!['SMALL', 'MEDIUM', 'LARGE'].includes(size)) return errors.push(`سطر ${line}: الحجم SMALL أو MEDIUM أو LARGE`);
    if (/[<>]/.test(get('name_ar') + get('name_en') + get('description') + get('description_en'))) return errors.push(`سطر ${line}: ممنوع < أو >`);
    rows.push({ nameAr: get('name_ar'), name: get('name_en'), price, section, size, description: get('description') || null, descriptionEn: get('description_en') || null });
  });
  return { rows, errors };
}

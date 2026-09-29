(function(){
  const $=(s,r=document)=>r.querySelector(s),$$=(s,r=document)=>Array.from(r.querySelectorAll(s));
  $$('.faq-question').forEach((button)=>button.addEventListener('click',()=>{
    const item=button.closest('.faq-item'),open=item.classList.toggle('is-open');
    button.setAttribute('aria-expanded',String(open));
  }));
  $$('.copy-code').forEach((button)=>button.addEventListener('click',async()=>{
    const value=button.dataset.copy||'';
    try{await navigator.clipboard.writeText(value);}catch(_){const input=document.createElement('input');input.value=value;document.body.appendChild(input);input.select();document.execCommand('copy');input.remove();}
    const old=button.textContent;button.textContent='تم النسخ ✓';setTimeout(()=>button.textContent=old,1600);
  }));
  const areaSearch=$('#area-search');
  if(areaSearch)areaSearch.addEventListener('input',()=>{const q=areaSearch.value.trim().toLowerCase();$$('[data-area]').forEach((row)=>row.hidden=q&&!row.textContent.toLowerCase().includes(q));});
  const giftForm=$('#gift-form');
  if(giftForm)giftForm.addEventListener('submit',(event)=>{event.preventDefault();const amount=Number(giftForm.amount.value),email=giftForm.email.value.trim(),alert=$('#gift-alert');if(!email||amount<100){alert.textContent='راجع البريد الإلكتروني واختر قيمة تبدأ من 100 جنيه.';alert.className='inline-alert error';return;}const code='VG-'+Math.random().toString(36).slice(2,8).toUpperCase();alert.innerHTML=`تم إنشاء بطاقة بقيمة <strong>${amount} EGP</strong> وإرسالها إلى ${email}. كود التجربة: <strong>${code}</strong>`;alert.className='inline-alert success';giftForm.reset();});
  const addressForm=$('#address-form'),addressList=$('#address-list'),addressKey='velox_saved_addresses';
  function readAddresses(){try{return JSON.parse(localStorage.getItem(addressKey)||'[]')}catch(_){return[]}}
  function paintAddresses(){if(!addressList)return;const rows=readAddresses();addressList.innerHTML=rows.length?rows.map((a,i)=>`<article class="info-card saved-row"><div><strong>${a.label}</strong><p>${a.gov} — ${a.address}</p></div><button type="button" data-remove-address="${i}">حذف</button></article>`).join(''):'<div class="info-card"><p>لا توجد عناوين محفوظة بعد.</p></div>';$$('[data-remove-address]',addressList).forEach(b=>b.addEventListener('click',()=>{const rows=readAddresses();rows.splice(Number(b.dataset.removeAddress),1);localStorage.setItem(addressKey,JSON.stringify(rows));paintAddresses();}));}
  if(addressForm){paintAddresses();addressForm.addEventListener('submit',(event)=>{event.preventDefault();const data={label:addressForm.label.value.trim(),gov:addressForm.governorate.value,address:addressForm.address.value.trim()};if(!data.label||!data.gov||data.address.length<5)return;const rows=readAddresses();rows.push(data);localStorage.setItem(addressKey,JSON.stringify(rows));addressForm.reset();paintAddresses();});}
  $$('[data-demo-form]').forEach(form=>form.addEventListener('submit',(event)=>{event.preventDefault();const alert=$('.inline-alert',form);if(alert){alert.textContent='تم استلام طلبك بنجاح. سيتواصل معك فريق VELOX قريبًا.';alert.className='inline-alert success';}form.reset();}));
})();

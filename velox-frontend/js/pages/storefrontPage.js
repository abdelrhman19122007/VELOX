(function () {
  const cartKey = 'velox_cart';
  const locationKey = 'velox_location';
  const isCatalogPage = document.body.classList.contains('catalog-page');
  let allProducts = [];
  const params = new URLSearchParams(window.location.search);
  let activeCategory = params.get('category') || 'all';
  let activeStore = params.get('store') || '';
  let searchTerm = params.get('q') || '';
  let cart = readCart();

  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));
  const lang = () => window.VeloxI18n.getLang();
  const t = (key) => window.VeloxI18n.translate(key, lang());

  function readCart() {
    try {
      const value = JSON.parse(localStorage.getItem(cartKey));
      return Array.isArray(value) ? value : [];
    } catch (_) { return []; }
  }
  function saveCart() {
    localStorage.setItem(cartKey, JSON.stringify(cart));
    document.dispatchEvent(new CustomEvent('velox:cartchange', { detail: { count: cart.reduce((s, i) => s + Number(i.qty), 0) } }));
  }
  function productById(id) { return allProducts.find((p) => Number(p.id) === Number(id)); }
  function money(value) { return window.VeloxCatalogService.formatPrice(value); }
  function escapeHtml(value) { return String(value).replace(/[&<>'"]/g, (c) => ({ '&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#039;','"':'&quot;' }[c])); }

  function normalize(value) {
    return String(value ?? '').toLocaleLowerCase('ar-EG').normalize('NFKD')
      .replace(/[\u064B-\u065F\u0670]/g, '').replace(/[إأآا]/g, 'ا').replace(/ى/g, 'ي').replace(/ة/g, 'ه')
      .replace(/ؤ/g, 'و').replace(/ئ/g, 'ي').replace(/ـ/g, '').replace(/[^\p{L}\p{N}\s]/gu, ' ')
      .replace(/\s+/g, ' ').trim();
  }
  function levenshtein(a, b) {
    if (a === b) return 0; if (!a) return b.length; if (!b) return a.length;
    if (Math.abs(a.length - b.length) > 2) return 99;
    const prev = Array.from({ length: b.length + 1 }, (_, i) => i);
    for (let i=1;i<=a.length;i++) { const cur=[i]; for(let j=1;j<=b.length;j++) cur[j]=Math.min(cur[j-1]+1,prev[j]+1,prev[j-1]+(a[i-1]===b[j-1]?0:1)); for(let j=0;j<cur.length;j++) prev[j]=cur[j]; }
    return prev[b.length];
  }
  function searchScore(product, query) {
    const q=normalize(query); if(!q) return 1;
    const hay=normalize([product.nameAr,product.nameEn,product.storeAr,product.storeEn,product.descAr,product.descEn,product.category].join(' '));
    if(hay.includes(q)) return 100;
    const tokens=q.split(' ').filter(Boolean), words=hay.split(' '); let matched=0;
    for(const token of tokens){ if(words.some(w=>w===token)) matched+=3; else if(words.some(w=>w.startsWith(token))) matched+=2; else if(words.some(w=>w.includes(token))) matched+=1; else if(token.length>=4 && words.some(w=>levenshtein(token,w)<=2)) matched+=1; }
    return matched >= Math.max(1,Math.ceil(tokens.length/2)) ? matched : 0;
  }

  function categoryUrl(category) {
    let url = `products.html?category=${encodeURIComponent(category || 'all')}`;
    if (activeStore) url += `&store=${encodeURIComponent(activeStore)}`;
    return url;
  }
  function navigateToCategory(category) {
    window.location.href = categoryUrl(category);
  }

  function renderCategories() {
    const wrap = $('#category-grid'); if (!wrap) return;
    const categories=window.VeloxCatalogService.getCategories();
    wrap.innerHTML=categories.map(c=>{
      const name=t(c.titleKey), desc=lang()==='ar'?c.descAr:c.descEn;
      return `<button type="button" class="category-card ${activeCategory===c.id?'is-active':''}" data-category="${escapeHtml(c.id)}" style="--category-tint:${c.tint}">
        <span class="category-icon">${c.icon}</span><span class="category-copy"><strong>${escapeHtml(name)}</strong><small>${escapeHtml(desc)}</small></span><span class="category-arrow">↗</span>
      </button>`;
    }).join('');
    $$('#category-grid [data-category]').forEach(btn=>btn.addEventListener('click',()=>navigateToCategory(btn.dataset.category)));
  }

  function renderFilters() {
    const wrap=$('#filter-pills'); if(!wrap) return;
    wrap.innerHTML=window.VeloxCatalogService.getCategories().map(c=>`<button type="button" class="filter-pill ${activeCategory===c.id?'is-active':''}" data-category="${escapeHtml(c.id)}">${escapeHtml(t(c.titleKey))}</button>`).join('');
    $$('#filter-pills [data-category]').forEach(btn=>btn.addEventListener('click',()=>{
      activeCategory=btn.dataset.category;
      if(isCatalogPage) { const u=new URL(window.location.href); u.searchParams.set('category',activeCategory); if(searchTerm) u.searchParams.set('q',searchTerm); else u.searchParams.delete('q'); history.replaceState({},'',u); }
      renderFilters(); renderProducts(); renderCatalogHeading();
    }));
  }

  function getVisibleProducts() {
    const q=searchTerm.trim();
    return allProducts.filter(p=>(activeCategory==='all'||p.category===activeCategory)
      &&(!activeStore||Number(p.store_id)===Number(activeStore)))
      .map(p=>({product:p,score:q?searchScore(p,q):1})).filter(x=>x.score>0).sort((a,b)=>b.score-a.score).map(x=>x.product);
  }

  function renderCatalogHeading() {
    if(!isCatalogPage) return;
    const title=$('#catalog-title'), subtitle=$('#catalog-subtitle'), count=$('#catalog-count');
    // Store view (?store=id): heading shows the store with its rating.
    if (activeStore) {
      const meta = window.VeloxStoreMeta && window.VeloxStoreMeta.get
        ? window.VeloxStoreMeta.get(activeStore) : null;
      const sample = allProducts.find((p) => Number(p.store_id) === Number(activeStore));
      const name = (meta && (lang()==='ar' ? meta.nameAr : meta.name))
        || (sample && (lang()==='ar' ? sample.storeAr : sample.storeEn)) || '';
      if (title) title.textContent = name;
      if (subtitle) subtitle.textContent = meta && meta.rating != null
        ? `★ ${meta.rating} · ${getVisibleProducts().length} ${lang()==='ar'?'منتج':'products'}`
        : `${getVisibleProducts().length} ${lang()==='ar'?'منتج':'products'}`;
      if (count) count.textContent = String(getVisibleProducts().length);
      return;
    }
    const cats=window.VeloxCatalogService.getCategories();
    const cat=cats.find(c=>c.id===activeCategory) || cats[0];
    if(title) title.textContent=activeCategory==='all' ? (lang()==='ar'?'كل منتجات VELOX':'All VELOX products') : t(cat?.titleKey || 'category.all');
    if(subtitle) subtitle.textContent=activeCategory==='all' ? (lang()==='ar'?'اختار قسمًا أو ابحث عن منتج، ثم افتح الصورة لرؤية التفاصيل بوضوح.':'Choose a category or search for a product, then open the image for a closer look.') : (lang()==='ar' ? cat?.descAr || '' : cat?.descEn || '');
    if(count) count.textContent=String(getVisibleProducts().length);
  }

  function renderSearchMeta(count) {
    const meta=$('#search-meta'); if(!meta) return;
    if(!searchTerm.trim()){meta.hidden=true;meta.textContent='';return;}
    meta.hidden=false;
    meta.innerHTML=lang()==='ar'?`<strong>${count}</strong> نتيجة للبحث عن <span>“${escapeHtml(searchTerm.trim())}”</span>`:`<strong>${count}</strong> result${count===1?'':'s'} for <span>“${escapeHtml(searchTerm.trim())}”</span>`;
  }

  function renderProducts() {
    const grid=$('#product-grid'); if(!grid) return;
    const products=getVisibleProducts();
    grid.innerHTML=products.map(p=>{
      const name=lang()==='ar'?p.nameAr:p.nameEn, store=lang()==='ar'?p.storeAr:p.storeEn, desc=lang()==='ar'?p.descAr:p.descEn;
      const badge=p.badge?`<span class="product-badge">${escapeHtml(p.badge)}</span>`:'';
      return `<article class="product-card" data-product-id="${p.id}" style="--product-glow:${p.glow}">
        <div class="product-media" data-product-view="${p.id}">${badge}<button type="button" class="product-zoom" data-product-view="${p.id}" aria-label="${lang()==='ar'?'عرض صورة المنتج':'View product image'}">⌕</button><button type="button" class="fav-btn${isFav(p.id)?' is-active':''}" data-fav="${p.id}" aria-label="${lang()==='ar'?'إضافة للمفضلة':'Add to favorites'}">${isFav(p.id)?'♥':'♡'}</button>
          <img class="product-image" src="${escapeHtml(p.image || '')}" alt="${escapeHtml(name)}" loading="lazy" onerror="this.style.display='none';this.nextElementSibling.hidden=false;">
          <span class="product-fallback" aria-hidden="true" hidden>🛍️</span>
        </div>
        <div class="product-body"><div class="product-store">${escapeHtml(store)}</div><h3>${escapeHtml(name)}</h3><p class="product-desc">${escapeHtml(desc)}</p>
          <div class="product-bottom"><div class="price">${money(p.price)} <small>EGP</small></div><button type="button" class="add-btn" data-add-product="${p.id}" aria-label="${escapeHtml(t('product.addButton'))}">+</button></div>
        </div>
      </article>`;
    }).join('');
    const empty=$('#empty-products'), hasSearch=!!searchTerm.trim();
    if(empty){empty.hidden=products.length!==0; const title=$('[data-empty-title]',empty),sub=$('[data-empty-subtitle]',empty); if(title) title.textContent=hasSearch?t('home.emptySearchTitle'):t('home.emptyTitle'); if(sub) sub.textContent=hasSearch?t('home.emptySearchSubtitle'):t('home.emptySubtitle');}
    grid.hidden=products.length===0; renderSearchMeta(products.length); renderCatalogHeading();
    $$('#product-grid [data-add-product]').forEach(btn=>btn.addEventListener('click',e=>{e.stopPropagation();addToCart(Number(btn.dataset.addProduct));}));
    $$('#product-grid [data-fav]').forEach(btn=>btn.addEventListener('click',e=>{e.stopPropagation();toggleFav(Number(btn.dataset.fav));}));
    $$('#product-grid [data-product-view]').forEach(el=>el.addEventListener('click',()=>openProduct(Number(el.dataset.productView))));
  }

  function openProduct(id){
    const p=productById(id), modal=$('#product-modal'); if(!p||!modal) return;
    const name=lang()==='ar'?p.nameAr:p.nameEn, store=lang()==='ar'?p.storeAr:p.storeEn, desc=lang()==='ar'?p.descAr:p.descEn;
    $('#product-modal-image').src=p.image||''; $('#product-modal-image').alt=name;
    const thumbs=$('#product-modal-thumbs');
    if(thumbs){
      const gallery=Array.isArray(p.gallery)&&p.gallery.length?p.gallery:[p.image].filter(Boolean);
      thumbs.hidden=gallery.length<=1;
      thumbs.innerHTML=gallery.map((src,i)=>`<button type="button" class="product-modal-thumb ${i===0?'is-active':''}" data-thumb-src="${escapeHtml(src)}" aria-label="${lang()==='ar'?`عرض الصورة ${i+1}`:`View image ${i+1}`}"><img src="${escapeHtml(src)}" alt="" loading="lazy"></button>`).join('');
      $$('#product-modal-thumbs [data-thumb-src]').forEach(btn=>btn.addEventListener('click',()=>{
        $('#product-modal-image').src=btn.dataset.thumbSrc;
        $$('#product-modal-thumbs .product-modal-thumb').forEach(b=>b.classList.toggle('is-active',b===btn));
      }));
    }
    $('#product-modal-store').textContent=store; $('#product-modal-title').textContent=name; $('#product-modal-desc').textContent=desc; $('#product-modal-price').textContent=money(p.price);
    const badge=$('#product-modal-badge'); badge.textContent=p.badge||''; badge.hidden=!p.badge;
    $('#product-modal-add').dataset.productId=p.id;
    openModal('#product-modal');
  }

  function setSearch(value, syncUrl=true){
    searchTerm=value;
    const input=$('#product-search'); if(input) input.value=value;
    const clear=$('#search-clear'); if(clear) clear.hidden=!value;
    if(isCatalogPage && syncUrl){const u=new URL(window.location.href); if(value) u.searchParams.set('q',value); else u.searchParams.delete('q'); history.replaceState({},'',u);}
    renderProducts();
  }

  function addToCart(id){
    if(!productById(id)) return; const existing=cart.find(i=>Number(i.id)===Number(id));
    if(existing) existing.qty+=1; else cart.push({id,qty:1});
    saveCart(); renderCart(); openCart(); showToast(t('product.added'),'success');
  }
  function changeQty(id,delta){const item=cart.find(i=>Number(i.id)===Number(id)); if(!item)return; item.qty+=delta; if(item.qty<=0) cart=cart.filter(i=>Number(i.id)!==Number(id)); saveCart(); renderCart();}
  function cartTotal(){return cart.reduce((sum,i)=>{const p=productById(i.id);return sum+(p?Number(p.price)*Number(i.qty):0);},0);}
  function renderCart(){
    const items=$('#cart-items'); if(!items)return;
    items.innerHTML=cart.map(i=>{const p=productById(i.id);if(!p)return'';const name=lang()==='ar'?p.nameAr:p.nameEn,store=lang()==='ar'?p.storeAr:p.storeEn;return `<div class="cart-item"><div class="cart-item-media"><img src="${escapeHtml(p.image||'')}" alt="" onerror="this.style.display='none';this.nextElementSibling.hidden=false"><span hidden>🛍️</span></div><div><h4>${escapeHtml(name)}</h4><small>${escapeHtml(store)} · ${money(p.price)}</small></div><div class="cart-item-right"><div class="cart-item-price">${money(p.price*i.qty)}</div><div class="qty-control"><button type="button" data-qty="${p.id}" data-delta="-1">−</button><span>${i.qty}</span><button type="button" data-qty="${p.id}" data-delta="1">+</button></div></div></div>`;}).join('');
    const count=cart.reduce((s,i)=>s+Number(i.qty),0); $('#cart-empty').hidden=count>0; $('#cart-footer').hidden=count===0; $('#cart-total').textContent=money(cartTotal()); $('#cart-count').textContent=count;
    $$('#cart-items [data-qty]').forEach(btn=>btn.addEventListener('click',()=>changeQty(Number(btn.dataset.qty),Number(btn.dataset.delta))));
  }
  function openCart(){const d=$('#cart-drawer');if(!d)return;d.hidden=false;d.classList.add('is-open');d.setAttribute('aria-hidden','false');$('#drawer-backdrop').hidden=false;document.body.classList.add('drawer-open');}
  document.addEventListener('velox:open-side',()=>closeCart());
  document.addEventListener('velox:open-cart',()=>openCart());
  document.addEventListener('velox:close-cart',()=>closeCart());
  function closeCart(){const d=$('#cart-drawer');if(!d)return;d.classList.remove('is-open');d.setAttribute('aria-hidden','true');d.hidden=true;$('#drawer-backdrop').hidden=true;document.body.classList.remove('drawer-open');}
  function openModal(id){const m=$(id);if(!m)return;m.hidden=false;document.body.classList.add('modal-open');}
  function closeModal(modal){if(!modal)return;modal.hidden=true;if(!$('.modal-backdrop:not([hidden])'))document.body.classList.remove('modal-open');}

  function openAuth(){if(window.VeloxAuthService.getSession()) return placeDemoOrder(); switchAuthTab('login');clearAuthAlerts();openModal('#auth-modal');}
  function switchAuthTab(tab){$$('.auth-tab').forEach(b=>b.classList.toggle('is-active',b.dataset.authTab===tab)); if($('#modal-login-form'))$('#modal-login-form').hidden=tab!=='login'; if($('#modal-register-form'))$('#modal-register-form').hidden=tab!=='register'; if($('#auth-modal-title'))$('#auth-modal-title').textContent=tab==='login'?t('authModal.loginTitle'):t('authModal.registerTitle'); if($('#auth-modal-subtitle'))$('#auth-modal-subtitle').textContent=tab==='login'?t('authModal.loginSubtitle'):t('authModal.registerSubtitle');}
  function clearAuthAlerts(){if($('#modal-login-alert'))$('#modal-login-alert').innerHTML='';if($('#modal-register-alert'))$('#modal-register-alert').innerHTML='';}
  function updateAccountUI(){const session=window.VeloxAuthService.getSession(),label=$('#account-label'); if(!label)return; if(session?.user){const first=String(session.user.full_name||'').trim().split(/\s+/)[0]||'VELOX';label.textContent=lang()==='ar'?`أهلاً ${first}`:`Hi ${first}`;$('#account-btn').dataset.action='logout';$('#account-btn').classList.add('is-logged');} else {label.textContent=t('home.login');$('#account-btn').dataset.action='login';$('#account-btn').classList.remove('is-logged');}}
  async function loginFromModal(event){event.preventDefault();const form=event.currentTarget,email=form.email.value.trim(),password=form.password.value,alert=$('#modal-login-alert');if(!window.VeloxValidation.isValidEmail(email)||!password){window.VeloxDom.showAlert(alert,'error',t('login.error.invalid'));return;}const submit=$('#modal-login-submit');window.VeloxDom.setButtonLoading(submit,true);try{await window.VeloxAuthService.login({email,password});window.VeloxDom.showAlert(alert,'success',t('login.successSimple'));updateAccountUI();setTimeout(()=>{closeModal($('#auth-modal'));if(cart.length)placeDemoOrder();},550);}catch(err){window.VeloxDom.showAlert(alert,'error',t(err.i18nKey||'login.error.generic'));}finally{window.VeloxDom.setButtonLoading(submit,false);}}
  async function registerFromModal(event){event.preventDefault();const form=event.currentTarget,data={full_name:form.full_name.value.trim(),email:form.email.value.trim(),phone_number:form.phone_number.value.trim(),governorate:form.governorate.value,password:form.password.value},alert=$('#modal-register-alert');const valid=data.full_name.length>=2&&window.VeloxValidation.isValidEmail(data.email)&&window.VeloxValidation.isValidEgyptPhone(data.phone_number)&&data.governorate&&window.VeloxValidation.hasMinLength(data.password,8);if(!valid){window.VeloxDom.showAlert(alert,'error',lang()==='ar'?'راجع البيانات المطلوبة قبل المتابعة.':'Please check the required fields before continuing.');return;}const submit=$('#modal-register-submit');window.VeloxDom.setButtonLoading(submit,true);try{await window.VeloxAuthService.register(data);window.VeloxDom.showAlert(alert,'success',t('register.successSimple'));updateAccountUI();setTimeout(()=>{closeModal($('#auth-modal'));if(cart.length)placeDemoOrder();},700);}catch(err){window.VeloxDom.showAlert(alert,'error',t(err.i18nKey||'register.error.generic'));}finally{window.VeloxDom.setButtonLoading(submit,false);}}
  function getFavs(){try{const f=JSON.parse(localStorage.getItem('velox_favs')||'[]');return Array.isArray(f)?f:[];}catch(_){return[];}}
  function isFav(id){return getFavs().some(f=>Number(f)===Number(id));}
  function toggleFav(id){let f=getFavs();const i=f.findIndex(x=>Number(x)===Number(id));if(i>=0)f.splice(i,1);else f.push(Number(id));localStorage.setItem('velox_favs',JSON.stringify(f));renderProducts();}
  async function placeDemoOrder(){if(!cart.length)return;const session=window.VeloxAuthService.getSession();if(!session?.user)return openAuth();const items=cart.map(i=>({product_id:i.id,quantity:i.qty}));const paySel=$('#pay-method');const paymentMethod=paySel?paySel.value:'CASH_ON_DELIVERY';try{const result=await window.VeloxOrderService.createOrder({user_id:session.user.id,items,total_amount:cartTotal(),paymentMethod});cart=[];saveCart();renderCart();closeCart();const code=result?.orderCode||result?.orderId||'';const total=result?.total!=null?` · ${result.total} EGP`:'';showToast(`${t('checkout.success')}${code?` #${code}`:''}${total}`,'success',5200);}catch(err){showToast(err.message||t('checkout.backendHint'),'error',6200);}}
  function showToast(message,type='success',duration=2800){let toast=$('#velox-toast');if(!toast){toast=document.createElement('div');toast.id='velox-toast';toast.className='velox-toast';document.body.appendChild(toast);}toast.className=`velox-toast is-${type} is-visible`;toast.textContent=message;clearTimeout(showToast.timer);showToast.timer=setTimeout(()=>toast.classList.remove('is-visible'),duration);}

  function bindEvents(){
    const search=$('#product-search');
    if(search){let timer;search.addEventListener('input',e=>{clearTimeout(timer);timer=setTimeout(()=>setSearch(e.target.value),100);});search.addEventListener('keydown',e=>{if(e.key==='Enter'){e.preventDefault();if(isCatalogPage)renderProducts();else window.location.href=`products.html?category=all&q=${encodeURIComponent(search.value.trim())}`;}if(e.key==='Escape')setSearch('');});}
    $('#search-clear')?.addEventListener('click',()=>setSearch(''));
    $('#cart-close')?.addEventListener('click',closeCart);$('#drawer-backdrop')?.addEventListener('click',closeCart);
    $('#checkout-btn')?.addEventListener('click',()=>{if(!cart.length)return;closeCart();openAuth();});
    $('#account-btn')?.addEventListener('click',async()=>{if(window.VeloxAuthService.getSession()){try{await window.VeloxApiClient.request('/auth/logout',{method:'POST'});}catch(_){}window.VeloxAuthService.logout();updateAccountUI();showToast(lang()==='ar'?'تم تسجيل الخروج.':'You have been logged out.');}else openAuth();});
    $('#location-btn')?.addEventListener('click',()=>openModal('#location-modal'));
    $$('#location-grid [data-location]').forEach(btn=>btn.addEventListener('click',()=>{localStorage.setItem(locationKey,btn.dataset.location);closeModal($('#location-modal'));showToast(t('location.saved').replace('{{name}}',btn.textContent.trim()));}));
    $$('[data-close-modal]').forEach(btn=>btn.addEventListener('click',()=>closeModal(btn.closest('.modal-backdrop'))));
    ['#auth-modal','#location-modal','#product-modal'].forEach(sel=>$(sel)?.addEventListener('click',e=>{if(e.target===e.currentTarget)closeModal(e.currentTarget);}));
    $$('.auth-tab').forEach(btn=>btn.addEventListener('click',()=>switchAuthTab(btn.dataset.authTab)));
    $('#modal-login-form')?.addEventListener('submit',loginFromModal);$('#modal-register-form')?.addEventListener('submit',registerFromModal);
    $$('[data-toggle-password]').forEach(btn=>btn.addEventListener('click',()=>{const input=$('#'+btn.dataset.togglePassword);if(input)input.type=input.type==='password'?'text':'password';}));
    $('#product-modal-add')?.addEventListener('click',()=>{const id=Number($('#product-modal-add').dataset.productId);closeModal($('#product-modal'));addToCart(id);});
    window.addEventListener('keydown',e=>{if((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==='k'){e.preventDefault();search?.focus();}if(e.key==='Escape'){closeCart();closeModal($('#auth-modal'));closeModal($('#location-modal'));closeModal($('#product-modal'));}});
    document.addEventListener('velox:langchange',()=>{renderCategories();renderFilters();renderProducts();renderCart();updateAccountUI();switchAuthTab($('.auth-tab.is-active')?.dataset.authTab||'login');renderCatalogHeading();});
  }

  let isRefreshingCatalog=false;
  async function refreshCatalog(){if(isRefreshingCatalog)return;isRefreshingCatalog=true;try{const catalog=await window.VeloxCatalogService.loadCatalog();if(Array.isArray(catalog.products)&&catalog.products.length)allProducts=catalog.products;if(Array.isArray(catalog.categories)&&catalog.categories.length)window.VeloxCatalogService.replaceCategories?.(catalog.categories);}catch(_){if(!allProducts.length)allProducts=window.VeloxCatalogService.getProducts();}finally{if(!allProducts.length)allProducts=window.VeloxCatalogService.getProducts();if(!window.VeloxCatalogService.getCategories().some(c=>c.id===activeCategory))activeCategory='all';renderCategories();renderFilters();renderProducts();renderCart();updateAccountUI();renderCatalogHeading();if($('#product-search')&&searchTerm)$('#product-search').value=searchTerm;if($('#search-clear'))$('#search-clear').hidden=!searchTerm;isRefreshingCatalog=false;}}

  document.addEventListener('DOMContentLoaded',async()=>{bindEvents();switchAuthTab('login');await refreshCatalog();startNotifPoll();});
  window.addEventListener('pageshow',e=>{if(e.persisted)refreshCatalog();});

  let notifSeenId=0;
  try{notifSeenId=Number(localStorage.getItem('velox_notif_seen')||0);}catch(_){}
  function paintNotifBadge(n){
    const h=$('#notification-count'); if(h){h.textContent=n>0?n:'';h.hidden=n<=0;}
    document.querySelectorAll('.notification-badge').forEach(b=>{b.textContent=n>0?n:'';b.hidden=n<=0;});
  }
  async function pollNotifications(){
    try{
      if(window.VELOX_CONFIG.USE_MOCK_API)return;
      const s=window.VeloxAuthService.getSession(); if(!s?.user)return;
      const c=await window.VeloxApiClient.request('/notifications/unread-count?userId='+encodeURIComponent(s.user.email));
      paintNotifBadge(Number(c.unread||0));
      const list=await window.VeloxApiClient.request('/notifications?userId='+encodeURIComponent(s.user.email)+'&limit=5');
      if(Array.isArray(list)&&list.length){
        const maxId=Math.max(...list.map(n=>Number(n.id)||0));
        const fresh=list.filter(n=>Number(n.id)>notifSeenId);
        if(notifSeenId>0&&fresh.length){
          const n=fresh[fresh.length-1];
          showToast(`${n.title} — ${n.message}`,'success',5200);
          if(fresh.some(x=>x.type==='ORDER_DELIVERED')) showDeliveryPopupForLatest();
        }
        if(maxId>notifSeenId){notifSeenId=maxId;try{localStorage.setItem('velox_notif_seen',String(maxId));}catch(_){}}
      }
    }catch(_){}
  }
  function startNotifPoll(){pollNotifications();setInterval(pollNotifications,20000);}

  /* Delivery popup: invoice right after DELIVERED, under no other UI. */
  async function showDeliveryPopupForLatest(){
    try{
      const s=window.VeloxAuthService.getSession(); if(!s?.user||!s.token)return;
      const r=await window.VeloxApiClient.request('/orders/history?userId='+encodeURIComponent(s.user.email)+'&page=0&size=10');
      const done=((r&&r.content)||[]).filter(o=>o.status==='DELIVERED');
      if(!done.length)return;
      const o=done[0];
      let seen={}; try{seen=JSON.parse(localStorage.getItem('velox_delivery_popup')||'{}');}catch(_){}
      if(seen[o.orderId])return;
      seen[o.orderId]=1;
      try{localStorage.setItem('velox_delivery_popup',JSON.stringify(seen));}catch(_){}
      closeDeliveryPopup();
      const ov=document.createElement('div'); ov.id='delivery-popup'; ov.className='modal-backdrop';
      ov.innerHTML=`<div class="modal-card"><h2>${t('pg.track.deliveredTitle')}</h2>`
        +`<p>${t('pg.track.deliveredSub')} #${o.orderId} · ${o.total} EGP</p>`
        +`<div style="display:flex;gap:8px;margin-top:12px"><button type="button" class="btn btn-primary" id="delivery-invoice">${t('pg.orders.invoice')}</button>`
        +`<button type="button" class="btn" id="delivery-close">${t('pg.track.close')}</button></div></div>`;
      document.body.appendChild(ov);
      const close=()=>closeDeliveryPopup();
      ov.querySelector('#delivery-close').addEventListener('click',close);
      ov.addEventListener('click',e=>{if(e.target===ov)close();});
      ov.querySelector('#delivery-invoice').addEventListener('click',async()=>{
        try{
          const res=await fetch(window.VELOX_CONFIG.API_BASE_URL+'/orders/'+encodeURIComponent(o.orderId)+'/invoice',{headers:{Authorization:'Bearer '+s.token}});
          if(!res.ok)throw new Error('failed');
          const blob=await res.blob(); const a=document.createElement('a');
          a.href=URL.createObjectURL(blob); a.download='INV-'+o.orderId+'.pdf';
          document.body.appendChild(a); a.click();
          setTimeout(()=>{URL.revokeObjectURL(a.href);a.remove();},800);
        }catch(_){showToast(t('checkout.backendHint'),'error',4000);}
      });
    }catch(_){}
  }
  function closeDeliveryPopup(){document.getElementById('delivery-popup')?.remove();}
})();

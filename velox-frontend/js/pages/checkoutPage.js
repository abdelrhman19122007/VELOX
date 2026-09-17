(function(){
  const cartKey='velox_cart';
  const $=s=>document.querySelector(s);
  const $$=s=>Array.from(document.querySelectorAll(s));
  const lang=()=>window.VeloxI18n.getLang();
  const t=k=>window.VeloxI18n.translate(k,lang());
  const AR=()=>lang()==='ar';
  const money=v=>window.VeloxCatalogService.formatPrice(v);
  const esc=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#039;','"':'&quot;'}[c]));

  let cart=[];
  let currentStep=1;
  let shippingFee=20;
  let selectedGov='';
  let orderResult=null;
  let resumeOrderAfterLogin=false;

  function readCart(){try{const v=JSON.parse(localStorage.getItem(cartKey));return Array.isArray(v)?v:[];}catch(_){return[];}}
  function saveCart(c){localStorage.setItem(cartKey,JSON.stringify(c));document.dispatchEvent(new CustomEvent('velox:cartchange',{detail:{count:c.reduce((s,i)=>s+Number(i.qty),0)}}));}
  function productById(id){return window.VeloxCatalogService.getProducts().find(p=>Number(p.id)===Number(id));}
  function cartTotal(){return cart.reduce((s,i)=>{const p=productById(i.id);return s+(p?Number(p.price)*Number(i.qty):0);},0);}

  // Governorates
  const GOV_AR={
    CAIRO:'القاهرة',GIZA:'الجيزة',ALEXANDRIA:'الإسكندرية',DAMIETTA:'دمياط',BEHEIRA:'البحيرة',
    KAFR_EL_SHEIKH:'كفر الشيخ',GHARBIA:'الغربية',MENOFIA:'المنوفية',QALYUBIA:'القليوبية',
    SHARKIA:'الشرقية',DAKAHLIA:'الدقهلية',PORT_SAID:'بورسعيد',ISMAILIA:'الإسماعيلية',
    SUEZ:'السويس',NORTH_SINAI:'شمال سيناء',SOUTH_SINAI:'جنوب سيناء',FAYOUM:'الفيوم',
    BENI_SUEF:'بني سويف',MINYA:'المنيا',ASYUT:'أسيوط',SOHAG:'سوهاج',QENA:'قنا',
    LUXOR:'الأقصر',ASWAN:'أسوان',NEW_VALLEY:'الوادي الجديد',MATROUH:'مطروح',RED_SEA:'البحر الأحمر'
  };
  const GOV_SHIPPING={
    CAIRO:20,GIZA:25,ALEXANDRIA:40,DAMIETTA:60,BEHEIRA:50,KAFR_EL_SHEIKH:55,GHARBIA:50,
    MENOFIA:45,QALYUBIA:30,SHARKIA:45,DAKAHLIA:50,PORT_SAID:60,ISMAILIA:55,SUEZ:55,
    NORTH_SINAI:75,SOUTH_SINAI:75,FAYOUM:45,BENI_SUEF:50,MINYA:60,ASYUT:65,SOHAG:65,
    QENA:70,LUXOR:70,ASWAN:75,NEW_VALLEY:80,MATROUH:70,RED_SEA:70
  };
  const GOV_DAYS={
    CAIRO:1,GIZA:1,ALEXANDRIA:2,DAMIETTA:3,BEHEIRA:2,KAFR_EL_SHEIKH:3,GHARBIA:2,
    MENOFIA:2,QALYUBIA:1,SHARKIA:2,DAKAHLIA:2,PORT_SAID:3,ISMAILIA:3,SUEZ:3,
    NORTH_SINAI:5,SOUTH_SINAI:4,FAYOUM:2,BENI_SUEF:2,MINYA:3,ASYUT:3,SOHAG:3,
    QENA:4,LUXOR:4,ASWAN:4,NEW_VALLEY:5,MATROUH:4,RED_SEA:4
  };

  function populateGovernorates(){
    const sel=$('#checkout-governorate');
    if(!sel)return;
    const keys=Object.keys(GOV_AR);
    sel.innerHTML=`<option value="">${t('checkout.selectGov')}</option>`+keys.map(k=>{
      const label=AR()?GOV_AR[k]:k.replace(/_/g,' ').toLowerCase().replace(/\b\w/g,c=>c.toUpperCase());
      return `<option value="${k}">${label}</option>`;
    }).join('');
  }

  // Render step
  function goToStep(n){
    currentStep=n;
    $$('.checkout-panel').forEach(p=>p.hidden=true);
    $(`#step-${n}`).hidden=false;
    $$('.step').forEach(s=>{
      const sn=Number(s.dataset.step);
      s.classList.toggle('is-active',sn===n);
      s.classList.toggle('is-done',sn<n);
    });
    if(n===4) renderConfirmation();
    window.scrollTo({top:0,behavior:'smooth'});
  }

  // Step 1: Cart Review
  function renderCheckoutItems(){
    const wrap=$('#checkout-items');if(!wrap)return;
    if(!cart.length){wrap.innerHTML='';$('#checkout-empty').hidden=false;$('#step1-next').hidden=true;return;}
    $('#checkout-empty').hidden=true;$('#step1-next').hidden=false;
    wrap.innerHTML=cart.map(i=>{
      const p=productById(i.id);if(!p)return'';
      const name=AR()?p.nameAr:p.nameEn;
      return `<div class="checkout-cart-item">
        <div class="checkout-cart-item-img">
          <img src="${esc(p.image||'')}" alt="" onerror="this.style.display='none'">
        </div>
        <div><h4>${esc(name)}</h4><small>${money(p.price)}</small></div>
        <div class="checkout-cart-item-right">
          <div class="checkout-cart-item-price">${money(p.price*i.qty)}</div>
          <div class="checkout-cart-qty">× ${i.qty}</div>
        </div>
      </div>`;
    }).join('');
  }

  // Step 2: Validate address
  function validateStep2(){
    let valid=true;
    const fields=[
      {id:'checkout-name',err:'checkout-name-error'},
      {id:'checkout-phone',err:'checkout-phone-error'},
      {id:'checkout-governorate',err:'checkout-gov-error'},
      {id:'checkout-address',err:'checkout-addr-error'}
    ];
    fields.forEach(f=>{
      const el=$('#'+f.id),errEl=$('#'+f.err);
      const val=(el.value||'').trim();
      if(!val){
        valid=false;
        el.closest('.field-app').classList.add('has-error');
        if(errEl)errEl.hidden=false;
      }else{
        el.closest('.field-app').classList.remove('has-error');
        if(errEl)errEl.hidden=true;
      }
    });
    return valid;
  }

  // Step 3: Payment validation
  function validateStep3(){
    const sel=document.querySelector('input[name="payment"]:checked');
    if(!sel){
      const err=$('#checkout-pay-error');if(err)err.hidden=false;
      return false;
    }
    let valid=true;
    if(sel.value==='WALLET')valid=/^01\d{9}$/.test(($('#wallet-phone')?.value||'').replace(/\s/g,''));
    if(sel.value==='VISA'){
      const number=($('#card-number')?.value||'').replace(/\s/g,'');
      valid=($('#card-holder')?.value||'').trim().length>=3&&/^\d{16}$/.test(number)&&/^(0[1-9]|1[0-2])\/\d{2}$/.test($('#card-expiry')?.value||'')&&/^\d{3,4}$/.test($('#card-cvv')?.value||'');
    }
    const err=$('#checkout-pay-error');if(err){err.hidden=valid;err.textContent=valid?'':(AR()?'أكمل بيانات طريقة الدفع بشكل صحيح.':'Complete the payment details correctly.');}
    if(!valid)return false;
    if(err)err.hidden=true;
    return true;
  }

  // Step 4: Render confirmation summary
  function renderConfirmation(){
    const items=$('#summary-items');if(!items)return;
    // Items
    items.innerHTML=cart.map(i=>{
      const p=productById(i.id);if(!p)return'';
      const name=AR()?p.nameAr:p.nameEn;
      return `<div style="display:flex;justify-content:space-between;padding:6px 0;font-size:13px;border-bottom:1px solid var(--color-border)">
        <span>${esc(name)} × ${i.qty}</span><strong>${money(p.price*i.qty)}</strong>
      </div>`;
    }).join('');
    // Address
    const govKey=$('#checkout-governorate').value;
    const govName=AR()?(GOV_AR[govKey]||govKey):govKey.replace(/_/g,' ');
    const addr=$('#checkout-address').value;
    const phone=$('#checkout-phone').value;
    const name=$('#checkout-name').value;
    $('#summary-address').innerHTML=`<strong>${esc(name)}</strong><br>${esc(addr)}<br>${govName} · ${esc(phone)}`;
    // Payment
    const payMethod=document.querySelector('input[name="payment"]:checked')?.value||'CASH_ON_DELIVERY';
    const payLabels={CASH_ON_DELIVERY:AR()?'كاش عند الاستلام':'Cash on Delivery',WALLET:AR()?'المحفظة':'Wallet',VISA:AR()?'فيزا / ماستركارد':'Visa / Mastercard'};
    $('#summary-payment').textContent=payLabels[payMethod]||payMethod;
    // Totals
    const sub=cartTotal();
    shippingFee=GOV_SHIPPING[govKey]||20;
    const total=sub+shippingFee;
    $('#summary-subtotal').textContent=money(sub);
    $('#summary-shipping').textContent=money(shippingFee);
    $('#summary-total').textContent=money(total);
  }

  // Place order
  async function placeOrder(){
    const btn=$('#confirm-order');
    const session=window.VeloxAuthService.getSession();
    if(!session?.user){
      resumeOrderAfterLogin=true;
      openAuthModal();
      return;
    }
    if(!cart.length||!validateStep2()||!validateStep3())return;
    btn.disabled=true;btn.textContent=AR()?'جاري التأكيد...':'Confirming...';
    const govKey=$('#checkout-governorate').value;
    const items=cart.map(i=>({product_id:i.id,quantity:i.qty}));
    const payMethod=document.querySelector('input[name="payment"]:checked')?.value||'CASH_ON_DELIVERY';
    const notes=($('#checkout-notes').value||'').trim();
    try{
      orderResult=await window.VeloxOrderService.createOrder({
        user_id:session.user.id,
        items,
        total_amount:cartTotal()+shippingFee,
        paymentMethod:payMethod,
        governorate:govKey,
        address:$('#checkout-address').value,
        notes:notes||undefined
      });
      const code=orderResult?.orderCode||orderResult?.orderId||('VELOX-'+Date.now());
      const eta=GOV_DAYS[govKey]||1;
      const etaText=AR()?`${eta} ${eta===1?'يوم':'أيام'}`:`${eta} day${eta>1?'s':''}`;
      // Show confirmation
      $$('.checkout-panel').forEach(p=>p.hidden=true);
      $('#confirmation-screen').hidden=false;
      $$('.step').forEach(s=>s.classList.add('is-done'));
      $('#confirm-order-id').textContent='#'+code;
      $('#confirm-eta').textContent=etaText;
      $('#confirm-total').textContent=money(cartTotal()+shippingFee);
      // Clear cart
      cart=[];saveCart(cart);
    }catch(err){
      showToast(err.message||(AR()?'حدث خطأ':'An error occurred'),'error');
      btn.disabled=false;btn.textContent=t('checkout.confirmBtn');
    }
  }

  function showToast(msg,type='success',dur=3500){
    let toast=$('#velox-toast');
    if(!toast){toast=document.createElement('div');toast.id='velox-toast';toast.className='velox-toast';document.body.appendChild(toast);}
    toast.className=`velox-toast is-${type} is-visible`;toast.textContent=msg;
    clearTimeout(showToast._t);showToast._t=setTimeout(()=>toast.classList.remove('is-visible'),dur);
  }

  function openAuthModal(){
    const modal=$('#checkout-auth-modal');
    if(!modal)return;
    modal.hidden=false;document.body.classList.add('modal-open');
    setTimeout(()=>$('#checkout-login-email')?.focus(),60);
  }

  function closeAuthModal(){
    const modal=$('#checkout-auth-modal');if(!modal)return;
    modal.hidden=true;document.body.classList.remove('modal-open');
  }

  async function loginAtCheckout(event){
    event.preventDefault();
    const email=($('#checkout-login-email')?.value||'').trim();
    const password=$('#checkout-login-password')?.value||'';
    const alert=$('#checkout-login-alert');
    const submit=$('#checkout-login-submit');
    if(!/^\S+@\S+\.\S+$/.test(email)||!password){
      alert.textContent=AR()?'اكتب بريدًا إلكترونيًا وكلمة مرور صحيحة.':'Enter a valid email and password.';return;
    }
    submit.disabled=true;submit.textContent=AR()?'جاري تسجيل الدخول...':'Signing in...';alert.textContent='';
    try{
      await window.VeloxAuthService.login({email,password});
      alert.classList.add('is-success');alert.textContent=AR()?'تم تسجيل الدخول بنجاح.':'Signed in successfully.';
      setTimeout(()=>{closeAuthModal();if(resumeOrderAfterLogin){resumeOrderAfterLogin=false;placeOrder();}},350);
    }catch(err){
      alert.classList.remove('is-success');alert.textContent=err.message||(AR()?'تعذر تسجيل الدخول.':'Could not sign in.');
      submit.disabled=false;submit.textContent=AR()?'تسجيل الدخول وإكمال الطلب':'Sign in and complete order';
    }
  }

  // Init
  document.addEventListener('DOMContentLoaded',()=>{
    cart=readCart();
    populateGovernorates();
    renderCheckoutItems();
    goToStep(1);

    // Step navigation
    $('#step1-next')?.addEventListener('click',()=>{
      if(!cart.length)return;goToStep(2);
    });
    $('#step2-back')?.addEventListener('click',()=>goToStep(1));
    $('#step2-next')?.addEventListener('click',()=>{if(validateStep2())goToStep(3);});
    $('#step3-back')?.addEventListener('click',()=>goToStep(2));
    $('#step3-next')?.addEventListener('click',()=>{if(validateStep3())goToStep(4);});
    $('#step4-back')?.addEventListener('click',()=>goToStep(3));
    $('#confirm-order')?.addEventListener('click',placeOrder);
    $('#checkout-login-form')?.addEventListener('submit',loginAtCheckout);
    $('#checkout-auth-close')?.addEventListener('click',closeAuthModal);
    $('#checkout-auth-modal')?.addEventListener('click',e=>{if(e.target===e.currentTarget)closeAuthModal();});

    // Payment selection
    $$('.payment-option').forEach(opt=>{
      opt.addEventListener('click',()=>{
        $$('.payment-option').forEach(o=>o.classList.remove('is-selected'));
        opt.classList.add('is-selected');
        opt.querySelector('input').checked=true;
        const method=opt.dataset.method;
        if($('#wallet-details'))$('#wallet-details').hidden=method!=='WALLET';
        if($('#card-details'))$('#card-details').hidden=method!=='VISA';
      });
    });

    $('#card-number')?.addEventListener('input',e=>{e.target.value=e.target.value.replace(/\D/g,'').slice(0,16).replace(/(.{4})/g,'$1 ').trim();});
    $('#card-expiry')?.addEventListener('input',e=>{const d=e.target.value.replace(/\D/g,'').slice(0,4);e.target.value=d.length>2?d.slice(0,2)+'/'+d.slice(2):d;});
    $('#card-cvv')?.addEventListener('input',e=>{e.target.value=e.target.value.replace(/\D/g,'').slice(0,4);});

    // Governorate change -> update shipping
    $('#checkout-governorate')?.addEventListener('change',e=>{
      selectedGov=e.target.value;
      shippingFee=GOV_SHIPPING[selectedGov]||20;
      if(currentStep===4) renderConfirmation();
    });

    // Track order button
    $('#track-order-btn')?.addEventListener('click',()=>{
      if(orderResult?.orderId||orderResult?.orderCode){
        const code=orderResult.orderCode||orderResult.orderId;
        window.location.href=`orders.html?order=${encodeURIComponent(code)}`;
      }else{
        window.location.href='home.html';
      }
    });

    // If not logged in, redirect to home
    if(!window.VeloxAuthService.getSession()){
      // Allow viewing but warn on confirm
    }
  });

  // i18n refresh
  document.addEventListener('velox:langchange',()=>{
    populateGovernorates();
    renderCheckoutItems();
    if(currentStep===4)renderConfirmation();
  });
})();

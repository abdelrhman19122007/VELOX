(function(){
  const root=document.documentElement;
  const toast=document.getElementById('app-toast');
  function applyTheme(){
    const t=localStorage.getItem('velox_theme') || 'light'; root.setAttribute('data-theme',t);
  }
  function toastMsg(msg){ if(!toast)return; toast.textContent=msg; toast.classList.add('show'); setTimeout(()=>toast.classList.remove('show'),2200); }
  document.addEventListener('DOMContentLoaded',()=>{
    applyTheme();
    document.querySelectorAll('#page-theme,#page-theme-secondary').forEach(btn=>btn.addEventListener('click',()=>{
      const next=root.getAttribute('data-theme')==='dark'?'light':'dark'; root.setAttribute('data-theme',next); localStorage.setItem('velox_theme',next); toastMsg(next==='dark'?'تم تفعيل الوضع الليلي':'تم تفعيل الوضع النهاري');
    }));
    document.getElementById('clear-notifications')?.addEventListener('click',()=>{
      document.querySelectorAll('.notification-item').forEach(x=>x.classList.remove('unread'));
      toastMsg('تم تعليم الإشعارات كمقروءة');
    });
    const rating=document.querySelectorAll('[data-rate]');
    rating.forEach(btn=>btn.addEventListener('click',()=>{
      const value=btn.dataset.rate; rating.forEach(b=>b.classList.toggle('active',Number(b.dataset.rate)<=Number(value))); document.getElementById('rating-value').value=value;
    }));
    document.querySelectorAll('[data-save-setting]').forEach(input=>input.addEventListener('change',()=>{localStorage.setItem(input.dataset.saveSetting,input.checked?'1':'0');}));
  });
})();

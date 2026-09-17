(function(){
  function addStyle(){const s=document.createElement('style');s.textContent='.google-login-wrap{margin-top:14px}.auth-divider{display:flex;align-items:center;gap:10px;color:#8b94a7;font-size:12px;margin:14px 0}.auth-divider:before,.auth-divider:after{content:"";height:1px;background:#e2e6f0;flex:1}.google-setup{width:100%;height:44px;border:1px solid #d8dce8;border-radius:10px;background:#fff;color:#4b5568;font-weight:800}.google-login-error{font-size:12px;color:#b4233b;margin-top:8px;text-align:center}';document.head.appendChild(s);}
  async function init(){
    const forms=[...document.querySelectorAll('#login-form,#modal-login-form')];if(!forms.length)return;addStyle();
    let config={enabled:false,clientId:''};try{config=await window.VeloxApiClient.request('/auth/google-config')}catch(_){}
    for(const form of forms){const wrap=document.createElement('div');wrap.className='google-login-wrap';wrap.innerHTML='<div class="auth-divider"><span>أو</span></div><div class="google-button"></div><div class="google-login-error" aria-live="polite"></div>';form.appendChild(wrap);if(!config.enabled){wrap.querySelector('.google-button').innerHTML='<button type="button" class="google-setup" disabled>متابعة باستخدام Google — يحتاج Client ID</button>';continue;}}
    if(!config.enabled)return;
    const script=document.createElement('script');script.src='https://accounts.google.com/gsi/client';script.async=true;script.onload=()=>{google.accounts.id.initialize({client_id:config.clientId,callback:async(response)=>{try{await window.VeloxAuthService.googleLogin(response.credential);window.location.href='index.html?welcome=google';}catch(err){document.querySelectorAll('.google-login-error').forEach(el=>el.textContent='تعذر التحقق من حساب Google. حاول مرة أخرى.');}}});document.querySelectorAll('.google-button').forEach(el=>google.accounts.id.renderButton(el,{theme:'outline',size:'large',width:Math.round(el.getBoundingClientRect().width),text:'continue_with',shape:'rectangular'}));};document.head.appendChild(script);
  }
  document.readyState==='loading'?document.addEventListener('DOMContentLoaded',init):init();
})();

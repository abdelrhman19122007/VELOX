/* Supplied Stitch design: sample interactions never submit a real order. */
(function () {
  const showPreviewNotice = () => window.alert('دي معاينة للتصميم الأصلي ببيانات تجريبية، ولا يتم إرسال طلب أو خصم أي مبلغ. للتسوق بالبيانات الفعلية افتح shop.html.');
  document.querySelectorAll('button').forEach(button => {
    const text = button.textContent.trim();
    if (text.includes('تأكيد ودفع الطلب')) button.addEventListener('click', showPreviewNotice);
    if (text.includes('نسخ الكود')) button.addEventListener('click', async () => {
      try { await navigator.clipboard.writeText('VELOX30'); } catch (_) { window.prompt('كود العرض في التصميم:', 'VELOX30'); }
    });
    if (text === 'favorite') {
      button.setAttribute('aria-label', 'إضافة للمفضلة في المعاينة');
      button.addEventListener('click', () => {
        const active = button.getAttribute('aria-pressed') !== 'true';
        button.setAttribute('aria-pressed', String(active));
        button.style.color = active ? '#ff5e1e' : '';
      });
    }
    if (text === 'بحث' || text.includes('انطلق')) button.addEventListener('click', () => {
      const input = button.closest('div')?.querySelector('input') || document.querySelector('input[type="search"]');
      window.location.href = 'products.html?category=all&q=' + encodeURIComponent(input?.value || '');
    });
    if (text === 'add' && !button.hasAttribute('onclick')) button.addEventListener('click', () => { window.location.href = 'design-store.html'; });
  });
  document.querySelectorAll('input[type="search"]').forEach(input => input.addEventListener('keydown', event => {
    if (event.key === 'Enter') window.location.href = 'products.html?category=all&q=' + encodeURIComponent(input.value);
  }));
  // Replace sample claims of external activity with honest preview feedback.
  window.handleCallCourier = showPreviewNotice;
  window.handleSupportDialog = showPreviewNotice;
  window.sendChatMessage = function () {
    const input = document.getElementById('chat-input');
    const messages = document.getElementById('chat-messages');
    if (!input?.value.trim() || !messages) return;
    const message = document.createElement('div');
    message.className = 'bg-primary-container text-on-primary-container p-2 rounded-lg text-body-sm';
    message.textContent = input.value + ' (رسالة تجريبية غير مرسلة)';
    messages.appendChild(message); input.value = '';
  };
})();

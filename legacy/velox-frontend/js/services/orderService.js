(function () {
  async function createOrder(payload) {
    const cfg=window.VELOX_CONFIG;
    if(!cfg.USE_MOCK_API) return window.VeloxApiClient.request(cfg.ENDPOINTS.orders,{method:'POST',body:payload});
    await new Promise(resolve=>setTimeout(resolve,cfg.MOCK_LATENCY_MS));
    return {success:true,orderId:'VELOX-DEMO-'+Date.now(),...payload};
  }
  window.VeloxOrderService={createOrder};
})();

// Google OAuth is registered for IntelliJ's local origin. Keep every local
// entry point on that single canonical origin to prevent origin_mismatch.
if ((location.hostname === 'localhost' || location.hostname === '127.0.0.1') &&
    (location.port === '5500' || location.hostname === '127.0.0.1')) {
  location.replace(`http://localhost:63342${location.pathname}${location.search}${location.hash}`);
}

window.VELOX_CONFIG={
  USE_MOCK_API:false,
  API_BASE_URL:'http://localhost:8080/api',
  MOCK_LATENCY_MS:450,
  ENDPOINTS:{login:'/auth/login',register:'/auth/register',products:'/products',categories:'/categories',orders:'/orders'},
  GOVERNORATES:['CAIRO','GIZA','ALEXANDRIA','DAMIETTA','BEHEIRA','KAFR_EL_SHEIKH','GHARBIA','MENOFIA','QALYUBIA','SHARKIA','DAKAHLIA','PORT_SAID','ISMAILIA','SUEZ','NORTH_SINAI','SOUTH_SINAI','FAYOUM','BENI_SUEF','MINYA','ASYUT','SOHAG','QENA','LUXOR','ASWAN','NEW_VALLEY','MATROUH','RED_SEA']
};

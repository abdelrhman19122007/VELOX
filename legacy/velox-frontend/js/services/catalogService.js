(function () {
  const products = [
    {id:6,nameAr:'طبق مشويات مشكل (1 كجم)',nameEn:'Mixed Grill Platter (1 kg)',storeAr:'أبو طارق والشبراوي',storeEn:'Abou Tarek & Shabrawy',category:'food',price:450,glow:'#fff0df',image:'https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=900&q=82',descAr:'تشكيلة مشويات شهية بحجم عائلي.',descEn:'A generous family-size grilled meat platter.',badge:'BEST'},
    {id:7,nameAr:'راب شاورما لحم',nameEn:'Beef Shawarma Wrap',storeAr:'أبو طارق والشبراوي',storeEn:'Abou Tarek & Shabrawy',category:'food',price:95,glow:'#fff1df',image:'https://images.unsplash.com/photo-1529006557810-274b9b2fc783?auto=format&fit=crop&w=900&q=82',descAr:'شاورما لحم متبلة داخل راب طازج.',descEn:'Seasoned beef shawarma wrapped fresh.',badge:'HOT'},
    {id:8,nameAr:'كريب تشيكن كرانشي',nameEn:'Chicken Crepe Crunchy',storeAr:'أبو طارق والشبراوي',storeEn:'Abou Tarek & Shabrawy',category:'food',price:110,glow:'#ffeade',image:'assets/images/products/chicken-crepe-user.jpeg',gallery:['assets/images/products/chicken-crepe-user.jpeg'],descAr:'كريب دجاج مقرمش بطعم غني.',descEn:'Crispy chicken crepe with rich flavour.',badge:''},
    {id:9,nameAr:'ملوخية مع نصف فرخة',nameEn:'Molokhia with Half Chicken',storeAr:'أبو طارق والشبراوي',storeEn:'Abou Tarek & Shabrawy',category:'food',price:160,glow:'#edf7df',image:'assets/images/products/molokhia-half-chicken.jpeg',descAr:'طبق مصري تقليدي بلمسة منزلية.',descEn:'A classic Egyptian-style comfort meal.',badge:'LOCAL'},
    {id:10,nameAr:'بيتزا سوبر سوبريم',nameEn:'Pizza Super Supreme',storeAr:'Buffalo & Pizza Hut',storeEn:'Buffalo & Pizza Hut',category:'food',price:260,glow:'#fff0d8',image:'https://images.unsplash.com/photo-1574071318508-1cdbab80d002?auto=format&fit=crop&w=900&q=82',descAr:'بيتزا كبيرة محملة بالإضافات.',descEn:'A loaded large pizza with premium toppings.',badge:'TOP'},
    {id:11,nameAr:'برجر لحم بالمشروم',nameEn:'Double Mushroom Beef Burger',storeAr:'Buffalo & Pizza Hut',storeEn:'Buffalo & Pizza Hut',category:'food',price:180,glow:'#ffe8e5',image:'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=900&q=82',descAr:'دبل برجر لحم مع مشروم وصوص خاص.',descEn:'Double beef burger with mushroom and house sauce.',badge:''},
    {id:12,nameAr:'وجبة ستربس تشيكن',nameEn:'Crispy Chicken Strips Meal',storeAr:'Buffalo & Pizza Hut',storeEn:'Buffalo & Pizza Hut',category:'food',price:165,glow:'#fff1dc',image:'assets/images/products/chicken-strips-user.jpeg',gallery:['assets/images/products/chicken-strips-user.jpeg','assets/images/products/chicken-strips.jpeg'],descAr:'ستربس دجاج مقرمشة مع وجبة كاملة.',descEn:'Crispy chicken strips served as a full meal.',badge:''},
    {id:13,nameAr:'مكرونة ألفريدو إيطالي',nameEn:'Italian Pasta Alfredo',storeAr:'Buffalo & Pizza Hut',storeEn:'Buffalo & Pizza Hut',category:'food',price:140,glow:'#fff4e6',image:'https://images.unsplash.com/photo-1473093295043-cdd812d0e601?auto=format&fit=crop&w=900&q=82',descAr:'مكرونة كريمية بصوص ألفريدو.',descEn:'Creamy pasta with classic Alfredo sauce.',badge:''},
    {id:14,nameAr:'خبز بالثوم والجبنة',nameEn:'Cheesy Garlic Bread',storeAr:'Buffalo & Pizza Hut',storeEn:'Buffalo & Pizza Hut',category:'food',price:75,glow:'#fff3dc',image:'assets/images/products/cheesy-garlic-bread-user.jpeg',gallery:['assets/images/products/cheesy-garlic-bread-user.jpeg'],descAr:'خبز ثوم دافئ بغطاء جبنة ذائب.',descEn:'Warm garlic bread topped with melted cheese.',badge:''},
    {id:15,nameAr:'جاكيت جينز كاجوال',nameEn:'Casual Denim Jacket',storeAr:'Zara Casual',storeEn:'Zara Casual',category:'fashion',price:1200,glow:'#e9efff',image:'assets/images/products/denim-jacket.jpeg',descAr:'جاكيت جينز عملي للستايل اليومي.',descEn:'A versatile denim jacket for everyday style.',badge:'NEW'},
    {id:16,nameAr:'قبعة صيفية Bucket Hat',nameEn:'Summer Bucket Hat',storeAr:'Zara Casual',storeEn:'Zara Casual',category:'fashion',price:300,glow:'#fff2de',image:'assets/images/products/summer-hat-user.jpeg',gallery:['assets/images/products/summer-hat-user.jpeg'],descAr:'قبعة خفيفة مناسبة لأيام الصيف.',descEn:'A lightweight bucket hat for sunny days.',badge:''},
    {id:17,nameAr:'عطر لاكوست فرنسي 100 مل',nameEn:'Lacoste French Perfume 100ml',storeAr:'Lacoste Luxury',storeEn:'Lacoste Luxury',category:'fashion',price:4500,glow:'#f3e9ff',image:'assets/images/products/lacoste-lhomme-user.jpeg',gallery:['assets/images/products/lacoste-lhomme-user.jpeg','assets/images/products/lacoste-perfume.jpeg'],descAr:'عطر أنيق بإطلالة فاخرة.',descEn:'An elegant signature fragrance with a premium feel.',badge:'LUXE'},
    {id:18,nameAr:'قميص بولو كلاسيك',nameEn:'Classic Croco Polo Shirt',storeAr:'Lacoste Luxury',storeEn:'Lacoste Luxury',category:'fashion',price:3800,glow:'#eef6ea',image:'https://photos6.spartoo.hu/photos/160/16026859/16026859_1200_A.jpg',descAr:'قميص بولو بقصة كلاسيكية راقية.',descEn:'A refined polo shirt with a classic fit.',badge:''},
    {id:19,nameAr:'طقم حزام جلد طبيعي',nameEn:'Genuine Leather Belt Set',storeAr:'Lacoste Luxury',storeEn:'Lacoste Luxury',category:'fashion',price:2200,glow:'#f6e9dc',image:'assets/images/products/leather-belt-user.jpeg',gallery:['assets/images/products/leather-belt-user.jpeg'],descAr:'أحزمة جلد طبيعي بتشطيب فاخر.',descEn:'A premium set of genuine leather belts.',badge:''},
    {id:20,nameAr:'حذاء Ultraboost للجري',nameEn:'Ultraboost Running Sneakers',storeAr:'Adidas Sport',storeEn:'Adidas Sport',category:'fashion',price:2400,glow:'#e7f7ef',image:'assets/images/products/ultraboost-running.jpeg',descAr:'حذاء رياضي خفيف للأداء اليومي.',descEn:'Lightweight performance sneakers for everyday runs.',badge:'SPORT'},
    {id:21,nameAr:'طقم تريننج رياضي',nameEn:'Athletic Tracksuit Set',storeAr:'Adidas Sport',storeEn:'Adidas Sport',category:'fashion',price:1750,glow:'#edf1ff',image:'assets/images/products/athletic-tracksuit-user.jpeg',gallery:['assets/images/products/athletic-tracksuit-user.jpeg','assets/images/products/athletic-tracksuit.jpeg'],descAr:'طقم عملي للتمرين والحركة.',descEn:'A versatile set made for training and movement.',badge:''},
    {id:22,nameAr:'كاب رياضي + أساور',nameEn:'Sport Cap & Wristbands',storeAr:'Adidas Sport',storeEn:'Adidas Sport',category:'fashion',price:450,glow:'#edf5ff',image:'assets/images/products/sport-cap-wristbands.jpeg',gallery:['assets/images/products/sport-cap-wristbands.jpeg','assets/images/products/sport-caps-gallery.jpeg'],descAr:'إكسسوارات بسيطة تكمل لوكك الرياضي.',descEn:'Simple accessories to complete a sporty look.',badge:''},
    {id:23,nameAr:'iPhone 15 Pro Max 256GB',nameEn:'iPhone 15 Pro Max 256GB',storeAr:'Apple Flagship Store',storeEn:'Apple Flagship Store',category:'electronics',price:65000,glow:'#edf0f5',image:'assets/images/products/iphone-15-pro-max.jpeg',descAr:'هاتف Apple الرائد بسعة 256 جيجابايت.',descEn:'Apple flagship smartphone with 256GB storage.',badge:'PRO'},
    {id:24,nameAr:'MacBook Air M2 13 بوصة',nameEn:'MacBook Air M2 13-inch',storeAr:'Apple Flagship Store',storeEn:'Apple Flagship Store',category:'electronics',price:52000,glow:'#e9eef5',image:'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=900&q=82',descAr:'لابتوب خفيف بشريحة M2.',descEn:'A lightweight laptop powered by the M2 chip.',badge:'APPLE'},
    {id:25,nameAr:'Samsung Galaxy S24 Ultra',nameEn:'Samsung Galaxy S24 Ultra',storeAr:'Samsung Smart Hub',storeEn:'Samsung Smart Hub',category:'electronics',price:48000,glow:'#eeeaff',image:'https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?auto=format&fit=crop&w=900&q=82',descAr:'هاتف سامسونج الرائد للأداء القوي.',descEn:'A flagship Samsung phone built for power.',badge:'NEW'},
    {id:26,nameAr:'Galaxy Watch 6',nameEn:'Smart Watch Galaxy Watch 6',storeAr:'Samsung Smart Hub',storeEn:'Samsung Smart Hub',category:'electronics',price:8500,glow:'#e8f8ff',image:'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=82',descAr:'ساعة ذكية لمتابعة النشاط والإشعارات.',descEn:'A smart watch for activity and notifications.',badge:''},
    {id:27,nameAr:'قاعدة شحن لاسلكية سريعة',nameEn:'Wireless Fast Charging Pad',storeAr:'Samsung Smart Hub',storeEn:'Samsung Smart Hub',category:'electronics',price:950,glow:'#e8f4ff',image:'assets/images/products/wireless-charger-user.jpeg',gallery:['assets/images/products/wireless-charger-user.jpeg'],descAr:'شحن لاسلكي سريع وعملي للمكتب والمنزل.',descEn:'A clean, fast wireless charging pad for desk or home.',badge:'SMART'}
  ];
  let categories=[
    {id:'all',icon:'✦',titleKey:'category.all',descAr:'كل المختارات — 22 منتج',descEn:'All picks — 22 products',tint:'#e9ecff'},
    {id:'food',icon:'🍽️',titleKey:'category.food',descAr:'9 وجبات ومشروبات',descEn:'9 meals & drinks',tint:'#fff0d9'},
    {id:'electronics',icon:'⌁',titleKey:'category.electronics',descAr:'5 أجهزة وتقنيات',descEn:'5 tech products',tint:'#e9e4ff'},
    {id:'fashion',icon:'◈',titleKey:'category.fashion',descAr:'8 قطعة وإكسسوار',descEn:'8 fashion picks',tint:'#ffe8f2'}
  ];
  function getProducts(){return products.slice();}
  function getCategories(){return categories.slice();}
  function replaceCategories(next){
    if (!Array.isArray(next) || !next.length) return;
    categories = next.map((c) => ({
      id: c.id ?? c.code ?? c.category,
      icon: c.icon ?? '◈',
      titleKey: c.titleKey ?? (c.id ? `category.${c.id}` : 'category.all'),
      descAr: c.descAr ?? c.descriptionAr ?? '',
      descEn: c.descEn ?? c.descriptionEn ?? '',
      tint: c.tint ?? '#e9ecff'
    })).filter((c) => c.id);
  }
  async function loadCatalog(){
    const cfg=window.VELOX_CONFIG;
    if(cfg.USE_MOCK_API) return {products:getProducts(),categories:getCategories()};
    const [productData, categoryData]=await Promise.all([window.VeloxApiClient.request(cfg.ENDPOINTS.products),window.VeloxApiClient.request(cfg.ENDPOINTS.categories)]);
    return {products:Array.isArray(productData)?productData:(productData.products||[]),categories:Array.isArray(categoryData)?categoryData:(categoryData.categories||[])};
  }
  function formatPrice(value){return new Intl.NumberFormat('ar-EG',{maximumFractionDigits:0}).format(value)+' EGP';}
  window.VeloxCatalogService={getProducts,getCategories,replaceCategories,loadCatalog,formatPrice};
})();

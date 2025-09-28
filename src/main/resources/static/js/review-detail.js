// /js/review-detail.js
(function () {
  function res(el){try{return el.href||el.src||'';}catch(_){return ''}}
  function bad(u){
    try{
      const a=new URL(u,location.href);
      if (a.href===location.href) return true;                    // 현재 상세
      if (/^\/cafes\/\d+\/reviews(?:$|[/?#])/.test(a.pathname)) return true; // 리뷰 리스트
      return false;
    }catch(_){return false}
  }
  function shouldDrop(el){
    const tag=el.tagName?.toLowerCase();
    if(tag==='link' && el.getAttribute('rel')?.toLowerCase()==='stylesheet'){
      const u = res(el) || el.getAttribute('href') || '';
      return bad(u);
    }
    if(tag==='script' && el.hasAttribute('src')){
      const u = res(el) || el.getAttribute('src') || '';
      return bad(u);
    }
    return false;
  }
  function purge(root){
    (root||document).querySelectorAll('link[rel="stylesheet"],script[src]').forEach(function(el){
      if(shouldDrop(el)){ console.warn('[detail-js] remove bad tag:', el.outerHTML); el.remove(); }
    });
  }

  // 초기/동적 모두 차단
  purge(document);
  const mo=new MutationObserver(function(muts){
    muts.forEach(function(m){
      m.addedNodes && m.addedNodes.forEach(function(node){
        if(!(node instanceof Element)) return;
        if(shouldDrop(node)){ console.warn('[detail-js] blocked added tag:', node.outerHTML); node.remove(); return; }
        purge(node);
      });
    });
  });
  mo.observe(document.documentElement,{childList:true,subtree:true});
})();

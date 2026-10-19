(() => {
  const menu = document.querySelector('#menu-toggle');
  const shade = document.querySelector('#nav-shade');
  function setMenu(open) { document.body.classList.toggle('nav-open',open); menu?.setAttribute('aria-expanded',String(open)); }
  menu?.addEventListener('click',()=>setMenu(!document.body.classList.contains('nav-open')));
  shade?.addEventListener('click',()=>setMenu(false));
  document.addEventListener('keydown',e=>{if(e.key==='Escape')setMenu(false);});
  document.querySelectorAll('.app-sidebar a').forEach(a=>a.addEventListener('click',()=>setMenu(false)));
})();

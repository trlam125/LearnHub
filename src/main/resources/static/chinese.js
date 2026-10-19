(() => {
  let activeAudio;
  function notify(text) { const el=document.querySelector('#speech-status')||document.querySelector('#review-status'); if(el)el.textContent=text; }
  function speak(text,url,language) {
    language=language||'zh-CN';
    activeAudio?.pause();
    if ('speechSynthesis' in window) speechSynthesis.cancel();
    if (url) {
      activeAudio=new Audio(url);
      activeAudio.play().catch(()=>notify('Chưa phát được audio. Kiểm tra kết nối hoặc thử lại.'));
      return;
    }
    if (!('speechSynthesis' in window)) { notify('Trình duyệt chưa hỗ trợ đọc phát âm.'); return; }
    const voices=speechSynthesis.getVoices();
    const code=language.toLowerCase();
    const voice=voices.find(v=>v.lang.toLowerCase()===code)||voices.find(v=>v.lang.toLowerCase().startsWith(code.split('-')[0]));
    if (voices.length && !voice) { notify('Thiết bị chưa có giọng đọc cho ngôn ngữ này. Có thể thêm giọng trong cài đặt thiết bị.'); return; }
    const utterance=new SpeechSynthesisUtterance(text);utterance.lang=language;utterance.rate=.8;
    if(voice)utterance.voice=voice;
    utterance.onerror=()=>notify('Không phát được giọng đọc trên thiết bị này.');
    speechSynthesis.speak(utterance);
  }
  document.querySelectorAll('[data-speak]').forEach(button=>button.addEventListener('click',()=>speak(button.dataset.speak,button.dataset.audio,button.dataset.language)));
  const data=document.querySelector('#flashcard-data');
  if(!data)return;
  const deck=JSON.parse(data.textContent);
  let index=0,flipped=false,busy=false;
  const reviewed=new Set();
  const $=id=>document.getElementById(id);
  function controls() {
    $('review-again').disabled=busy||!flipped||reviewed.has(deck[index].id);
    $('review-known').disabled=busy||!flipped||reviewed.has(deck[index].id);
    $('next-card').disabled=busy;
    $('previous-card').disabled=busy;
    $('shuffle-cards').disabled=busy;
  }
  function draw() {
    flipped=false;
    const word=deck[index];
    $('card-count').textContent=`${index+1} / ${deck.length} thẻ`;
    $('card-hanzi').textContent=word.term;
    $('card-hanzi').lang=word.language;
    $('card-pinyin').textContent=word.pronunciation;
    $('card-meaning').textContent=word.meaning;
    $('card-example').textContent=[word.example,word.translation].filter(Boolean).join(' — ');
    $('card-meaning').hidden=true;$('card-example').hidden=true;
    $('card-side').textContent='TỪ VỰNG';$('flip-hint').textContent='Nhấn vào thẻ để xem nghĩa ↻';
    $('flip-card').classList.remove('is-flipped');
    $('review-status').textContent=reviewed.has(word.id)?'Bạn đã ghi nhận thẻ này trong bộ ôn.':'Lật thẻ trước khi đánh giá.';
    controls();
  }
  $('flip-card').addEventListener('click',()=>{
    if(busy)return;
    flipped=!flipped;
    $('card-meaning').hidden=!flipped;$('card-example').hidden=!flipped;
    $('card-side').textContent=flipped?'NGHĨA & VÍ DỤ':'TỪ VỰNG';
    $('flip-hint').textContent=flipped?'Bạn nhớ từ này đến đâu?':'Nhấn vào thẻ để xem nghĩa ↻';
    $('flip-card').classList.toggle('is-flipped',flipped);controls();
  });
  $('speak-card').addEventListener('click',()=>speak(deck[index].term,deck[index].audio,deck[index].language));
  $('previous-card').addEventListener('click',()=>{index=(index+deck.length-1)%deck.length;draw();});
  $('next-card').addEventListener('click',()=>{index=(index+1)%deck.length;draw();});
  $('shuffle-cards').addEventListener('click',()=>{
    for(let i=deck.length-1;i>0;i--){const j=Math.floor(Math.random()*(i+1));[deck[i],deck[j]]=[deck[j],deck[i]];}
    index=0;draw();
  });
  async function review(remembered) {
    if(busy||!flipped)return;
    const word=deck[index];busy=true;controls();$('review-status').textContent='Đang lưu lịch ôn…';
    try {
      const response=await fetch(word.review_url,{method:'POST',credentials:'same-origin',
        headers:{[document.querySelector('meta[name="csrf-header"]').content]:document.querySelector('#review-csrf [name=_csrf]').value},
        body:new URLSearchParams({remembered:remembered?'1':'0'})});
      if(!response.ok||!response.headers.get('Content-Type')?.includes('application/json'))throw new Error('review');
      const result=await response.json();reviewed.add(word.id);
      $('review-status').textContent=`Đã lưu. Lịch ôn tiếp theo: ${result.next_review.split('-').reverse().join('/')}. Chọn “Thẻ sau” để tiếp tục.`;
    } catch(_) { $('review-status').textContent='Chưa lưu được. Kiểm tra kết nối hoặc đăng nhập lại rồi thử lại.'; }
    finally {busy=false;controls();}
  }
  $('review-again').addEventListener('click',()=>review(false));
  $('review-known').addEventListener('click',()=>review(true));
  draw();
})();

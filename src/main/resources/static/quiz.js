(() => {
  const form = document.querySelector('#quiz-form');
  if (!form) return;
  const status = document.querySelector('#save-status');
  const timer = document.querySelector('#timer');
  const startedAt = performance.now();
  const receivedAt = performance.getEntriesByType?.('navigation')?.[0]?.responseStart;
  const renderElapsed = receivedAt > 0 ? Math.max(0, startedAt - receivedAt) : 0;
  const remainingMillis = Math.max(0, Number(timer.dataset.remainingMillis) - renderElapsed);
  const button = document.querySelector('#submit-quiz');
  let revision = 0, savedRevision = 0, saving = false, submitting = false;
  function answers() {
    const result = {};
    form.querySelectorAll('[data-question]').forEach(q => {
      result[q.dataset.question] = [...q.querySelectorAll('input:checked')].map(el => Number(el.value));
    });
    return result;
  }
  async function save() {
    if (saving || submitting || revision === savedRevision) return;
    saving = true;
    let saved = false;
    const sendingRevision = revision;
    status.textContent = 'Đang lưu…';
    try {
      const response = await fetch(form.dataset.saveUrl, {
        method: 'POST', credentials: 'same-origin',
        headers: {'Content-Type': 'application/json', [document.querySelector('meta[name="csrf-header"]').content]: form.querySelector('[name=_csrf]').value},
        body: JSON.stringify(answers())
      });
      if (!response.ok || !response.headers.get('Content-Type')?.includes('application/json')) throw new Error('save');
      const data = await response.json();
      if (data.submitted) { location.reload(); return; }
      savedRevision = sendingRevision;
      saved = true;
      status.textContent = revision === savedRevision
        ? 'Đã lưu lúc ' + new Date().toLocaleTimeString('vi-VN') : 'Đang lưu thay đổi mới…';
    } catch (_) { status.textContent = 'Chưa lưu được. Giữ trang mở, kiểm tra kết nối.'; }
    finally {
      saving = false;
      // Drain changes made during this request immediately, before the next timer.
      // Failed requests still use the periodic retry, avoiding a tight retry loop.
      if (saved && revision !== savedRevision && !submitting) save();
    }
  }
  form.addEventListener('change', () => { revision++; save(); });
  setInterval(save, 5000);
  function tick() {
    const remaining = remainingMillis - (performance.now() - startedAt);
    const seconds = Math.max(0, Math.ceil(remaining / 1000));
    timer.textContent = `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
    if (remaining <= 0 && !submitting) {
      submitting = true; button.disabled = true;
      status.textContent = 'Đã hết giờ. Đang lấy kết quả từ máy chủ…';
      // Server grades previously saved answers. Late browser answers are not accepted.
      location.reload();
    }
  }
  setInterval(tick, 1000); tick();
  form.addEventListener('submit', e => {
    if (submitting) { e.preventDefault(); return; }
    if (!confirm('Nộp bài và xem kết quả?')) { e.preventDefault(); return; }
    submitting = true; button.disabled = true;
  });
  window.addEventListener('beforeunload', e => {
    if (!submitting && revision !== savedRevision) { e.preventDefault(); e.returnValue = ''; }
  });
})();

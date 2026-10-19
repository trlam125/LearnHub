(() => {
  'use strict';
  const file = document.getElementById('material-file');
  const status = document.getElementById('material-file-status');
  if (!file || !status) return;
  file.addEventListener('change', () => {
    const chosen = file.files && file.files[0];
    const tooLarge = chosen && chosen.size >= 1000000;
    const message = tooLarge ? 'File từ 1 MB trở lên: hãy lưu trên Google Drive, OneDrive hoặc nơi chia sẻ khác rồi thêm bằng liên kết.' : '';
    file.setCustomValidity(message);
    status.textContent = message;
    if (tooLarge) file.reportValidity();
  });
})();

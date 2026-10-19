(() => {
  document.querySelectorAll('[data-relation-search]').forEach(input => {
    const select = document.getElementById(input.dataset.target);
    const status = document.getElementById(input.dataset.target + '-search-status');
    let revision = 0, timer;
    input.addEventListener('input', () => {
      const current = ++revision;
      clearTimeout(timer);
      timer = setTimeout(async () => {
        status.textContent = 'Đang tìm…';
        try {
          const url = new URL(input.dataset.url, location.origin);
          url.searchParams.set('q', input.value);
          const response = await fetch(url, {credentials: 'same-origin', headers: {Accept: 'application/json'}});
          if (!response.ok || !response.headers.get('Content-Type')?.includes('application/json')) throw new Error('search');
          const options = await response.json();
          if (current !== revision) return;
          const selected = select.selectedOptions[0];
          const value = select.value;
          select.replaceChildren(new Option('— Chọn —', ''));
          if (value && !options.some(option => option.value === value))
            select.add(new Option(selected.textContent, value));
          options.forEach(option => select.add(new Option(option.label, option.value)));
          select.value = value;
          status.textContent = options.length ? 'Chọn mục phù hợp trong danh sách bên dưới.' : 'Không có kết quả. Mục đang chọn được giữ lại.';
        } catch (_) {
          if (current === revision) status.textContent = 'Chưa tìm được. Kiểm tra kết nối rồi thử lại.';
        }
      }, 250);
    });
  });
})();

const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/quiz.js'), 'utf8');

function page() {
  let monotonic = 0, selected = '11', reloads = 0;
  const events = {}, intervals = {}, requests = [], status = {};
  const form = {dataset: {saveUrl: '/attempts/test/save'}, addEventListener: (event, fn) => events[event] = fn,
    querySelector: () => ({value: 'csrf'}), querySelectorAll: () => [{dataset: {question: '1'},
      querySelectorAll: () => [{value: selected}]}]};
  const elements = {'#quiz-form': form, '#save-status': status, '#timer': {dataset: {remainingMillis: '2000'}},
    '#submit-quiz': {}, 'meta[name="csrf-header"]': {content: 'X-CSRF-TOKEN'}};
  vm.runInNewContext(source, {
    document: {querySelector: selector => elements[selector]}, performance: {now: () => monotonic}, Date,
    fetch: (_, options) => new Promise((resolve, reject) => requests.push({answers: JSON.parse(options.body), resolve, reject})),
    window: {addEventListener: (event, fn) => events[event] = fn}, location: {reload() { reloads++; }},
    setInterval: (fn, delay) => intervals[delay] = fn, confirm: () => true
  });
  return {requests, status, events,
    change(answer, at) { selected = answer; monotonic = at; events.change(); },
    tick(at) { monotonic = at; intervals[1000](); }, retry() { intervals[5000](); },
    get reloads() { return reloads; }};
}
const flush = () => new Promise(setImmediate);
const saved = {ok: true, headers: {get: () => 'application/json'}, json: async () => ({submitted: false})};

(async () => {
  const current = page();
  current.change('11', 0); current.change('12', 500);
  assert.equal(current.requests.length, 1);
  current.requests[0].resolve(saved); await flush();
  assert.equal(current.requests.length, 2, 'A newer change must be sent immediately after the first save');
  assert.deepEqual(current.requests[1].answers, {'1': [12]});
  assert.doesNotMatch(current.status.textContent, /Đã lưu/);
  current.requests[1].resolve(saved); await flush();
  assert.match(current.status.textContent, /Đã lưu/);
  current.tick(2000); assert.equal(current.reloads, 1);

  const failed = page(); failed.change('11', 0); failed.change('12', 100);
  failed.requests[0].reject(new Error('offline')); await flush();
  assert.equal(failed.requests.length, 1, 'Failure must not create an immediate retry loop');
  assert.match(failed.status.textContent, /Chưa lưu/);
  failed.retry(); assert.deepEqual(failed.requests[1].answers, {'1': [12]});
  failed.requests[1].resolve(saved); await flush();

  const submitted = page(); submitted.change('11', 0); submitted.change('12', 100);
  submitted.events.submit({preventDefault() {}});
  submitted.requests[0].resolve(saved); await flush();
  assert.equal(submitted.requests.length, 1, 'The final form submission owns answers after submit');
  console.log('PASS: pending quiz answers save immediately, status stays accurate, failures retry periodically, submit stops autosave');
})().catch(error => { console.error(error); process.exitCode = 1; });

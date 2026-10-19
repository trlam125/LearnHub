const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/quiz.js'), 'utf8');

function clock(remaining, wallClock) {
  let monotonic = 0, reloads = 0, tick;
  const timer = {dataset: {remainingMillis: String(remaining)}};
  const button = {};
  const elements = {'#quiz-form': {addEventListener() {}}, '#timer': timer,
    '#save-status': {}, '#submit-quiz': button};
  class WallClock extends Date { static now() { return wallClock; } }
  vm.runInNewContext(source, {
    Date: WallClock, performance: {now: () => monotonic},
    document: {querySelector: selector => elements[selector]},
    window: {addEventListener() {}}, location: {reload() { reloads++; }},
    setInterval(callback, delay) { if (delay === 1000) tick = callback; }
  });
  return {advance(value) { monotonic = value; tick(); },
    get reloads() { return reloads; }, get display() { return timer.textContent; }};
}

const accurate = clock(1500, Date.now());
assert.equal(accurate.display, '00:02');
accurate.advance(1000);
assert.equal(accurate.reloads, 0);
assert.equal(accurate.display, '00:01');
accurate.advance(1499);
assert.equal(accurate.reloads, 0);
accurate.advance(1500);
assert.equal(accurate.reloads, 1);
accurate.advance(2000);
assert.equal(accurate.reloads, 1);

for (const wallClock of [0, Date.now() - 86400000, Date.now() + 86400000]) {
  const skewed = clock(60000, wallClock);
  assert.equal(skewed.reloads, 0);
  assert.equal(skewed.display, '01:00');
  skewed.advance(59500);
  assert.equal(skewed.reloads, 0);
  skewed.advance(60000);
  assert.equal(skewed.reloads, 1);
}
console.log('PASS: quiz countdown uses server duration, waits for the full deadline and ignores client clock skew');

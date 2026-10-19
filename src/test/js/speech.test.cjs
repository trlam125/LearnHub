const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const script = fs.readFileSync(path.join(__dirname, '../../main/resources/static/chinese.js'), 'utf8');
function run(language, voices, audio) {
    let click, spoken, played;
    const status = {textContent: ''};
    const button = {dataset: {speak: 'hello', audio, language}, addEventListener: (_, action) => {click = action;}};
    const speech = {cancel() {}, getVoices: () => voices, speak: value => {spoken = value;}};
    vm.runInNewContext(script, {
        window: {speechSynthesis: speech}, speechSynthesis: speech,
        SpeechSynthesisUtterance: function(text) {this.text = text;},
        Audio: function(url) {this.pause = () => {}; this.play = () => {played = url; return Promise.resolve();};},
        document: {
            querySelectorAll: () => [button],
            querySelector: selector => selector === '#speech-status' ? status : null
        }
    });
    click();
    return {spoken, played, message: status.textContent};
}
const voices = [{lang: 'zh-CN'}, {lang: 'en-US'}];
assert.equal(run('en-US', voices).spoken.voice.lang, 'en-US');
assert.equal(run('zh-CN', voices).spoken.voice.lang, 'zh-CN');
assert.equal(run('en-US', [{lang: 'en-GB'}]).spoken.voice.lang, 'en-GB');
assert.equal(run('en-US', []).spoken.lang, 'en-US');
const missing = run('en-US', [{lang: 'zh-CN'}]);
assert.equal(missing.spoken, undefined);
assert.ok(missing.message.includes('chưa có giọng đọc'));
const recorded = run('en-US', voices, 'https://example.test/teacher.mp3');
assert.equal(recorded.played, 'https://example.test/teacher.mp3');
assert.equal(recorded.spoken, undefined);
console.log('PASS: 8 pronunciation checks');

// Run with:  node --test extension/test/eventMapper.test.js
const test = require('node:test');
const assert = require('node:assert');
const { createMapper, makeEventId } = require('../eventMapper.js');

test('tab switch is reported once per hidden period', () => {
  const m = createMapper();
  assert.deepStrictEqual(m.visibility(true, 1000).map(e => e.type), ['TAB_SWITCH']);
  assert.deepStrictEqual(m.visibility(true, 1100), []);            // duplicate hidden signal
  m.visibility(false, 5000);
  assert.deepStrictEqual(m.visibility(true, 9000).map(e => e.type), ['TAB_SWITCH']);   // legitimate repeat
});

test('blur caused by a tab switch is not also counted as a window exit', () => {
  const m = createMapper();
  m.visibility(true, 1000);
  assert.deepStrictEqual(m.blur(true, 1300), []);                  // page hidden at check time
  assert.deepStrictEqual(m.blur(false, 1400), []);                 // within grace period
});

test('window exit once per focus loss, again after focus returns', () => {
  const m = createMapper();
  assert.deepStrictEqual(m.blur(false, 10000).map(e => e.type), ['WINDOW_EXIT']);
  assert.deepStrictEqual(m.blur(false, 10500), []);
  m.focus();
  assert.deepStrictEqual(m.blur(false, 20000).map(e => e.type), ['WINDOW_EXIT']);
});

test('clipboard events within 400 ms count once; later ones count again', () => {
  const m = createMapper();
  assert.strictEqual(m.clipboard('copy', 1000).length, 1);
  assert.strictEqual(m.clipboard('copy', 1200).length, 0);
  assert.strictEqual(m.clipboard('paste', 1500).length, 1);
});

test('event ids match the backend pattern and are unique', () => {
  const pattern = /^[A-Za-z0-9:_-]{8,64}$/;
  const ids = new Set();
  for (let i = 0; i < 200; i++) {
    const id = makeEventId('MULTIPLE_PERSON');
    assert.ok(pattern.test(id), id);
    ids.add(id);
  }
  assert.strictEqual(ids.size, 200);
});

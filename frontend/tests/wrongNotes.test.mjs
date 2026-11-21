import test from 'node:test';
import assert from 'node:assert/strict';
import {wrongIds,wrongBySubject} from '../src/wrongNotes.js';

test('wrong then correct removes from wrong set',()=>{
  const h=[{id:1,correct:false},{id:2,correct:false},{id:1,correct:true}];
  assert.deepEqual([...wrongIds(h)],[2]);
});
test('correct then wrong again is wrong',()=>{
  const h=[{id:1,correct:true},{id:1,correct:false}];
  assert.deepEqual([...wrongIds(h)],[1]);
});
test('counts by subject ignore deleted questions',()=>{
  const h=[{id:1,correct:false},{id:2,correct:false},{id:3,correct:false}];
  const qs=[{id:1,subject:'A'},{id:2,subject:'A'},{id:3,subject:'B'}];
  assert.deepEqual(wrongBySubject(h,qs.slice(0,2).concat([{id:9,subject:'B'}])),{A:2});
});

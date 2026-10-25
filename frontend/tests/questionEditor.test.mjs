import test from 'node:test';
import assert from 'node:assert/strict';
import {changeQuestionType} from '../src/questionEditor.js';

test('short-answer to multiple-choice supplies editable choices and a valid answer',()=>{
 const q={type:'SHORT',title:'질문',answer:'답',choices:[]};
 const next=changeQuestionType(q,'MC');
 assert.equal(next.choices.length,4);assert.ok(next.choices.some(c=>c.id===next.answer));assert.equal(q.type,'SHORT');
});
test('clicking the already selected type preserves the answer',()=>{
 const q={type:'SHORT',answer:'답',choices:[]};assert.equal(changeQuestionType(q,'SHORT'),q);
});
test('OX and essay transitions initialize suitable answer fields',()=>{
 const q={type:'MC',answer:'b',choices:[{id:'a',text:'A'},{id:'b',text:'B'}]};
 assert.equal(changeQuestionType(q,'OX').answer,'O');assert.equal(changeQuestionType(q,'ESSAY').answer,'');
 assert.equal(changeQuestionType({...q,type:'SHORT'},'MC').choices[1].text,'B');
});

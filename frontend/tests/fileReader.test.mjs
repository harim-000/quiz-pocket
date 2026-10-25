import test from 'node:test';
import assert from 'node:assert/strict';
import {fileKind,decodeText,readQuestionFile,docxText} from '../src/fileReader.js';

test('only TXT, PDF, DOCX extensions are accepted',()=>{
 for(const name of ['a.TXT','a.PDF','a.DOCX'])assert.equal(fileKind(name),name.split('.').at(-1).toLowerCase());
 for(const name of ['a.json','a.doc','a.pdf.exe','pdf'])assert.throws(()=>fileKind(name));
});
test('UTF-8 text becomes reviewed question drafts without fictitious page numbers',async()=>{
 const text='단답형\n01. 정수 타입은?\n정답: int\n해설: 정수 타입\n서술형\n02. 설명하시오.\n예시 답안과 풀이\n모범 설명';
 const result=await readQuestionFile(new File([text],'문제.txt',{type:'text/plain'}));
 assert.equal(result.questions.length,2);assert.equal(result.questions[0].answer,'int');assert.equal(result.questions[1].type,'ESSAY');assert.equal(result.questions[0].sourcePage,null);
});
test('binary, empty, oversized and invalid documents fail clearly',async()=>{
 assert.throws(()=>decodeText(new Uint8Array([0,1,2])));
 await assert.rejects(()=>readQuestionFile(new File([],'empty.txt')));
 await assert.rejects(()=>readQuestionFile({name:'large.txt',size:16*1024*1024}));
 await assert.rejects(()=>readQuestionFile(new File(['not a pdf'],'fake.pdf')));
 await assert.rejects(()=>docxText(new Uint8Array([1,2,3])));
});

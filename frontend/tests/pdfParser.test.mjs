import test from 'node:test';
import assert from 'node:assert/strict';
import {parseQuestionPages,draftIssues} from '../src/pdfParser.js';

test('choice answer IDs, code and model answers survive page extraction',()=>{
 const {questions}=parseQuestionPages([{page:1,text:'1\n객관식 01~20\n01. 출력은?\nint x = 1;\n1) A\n2) B\n정답: 2) B\n해설: 설명\n단답형 21~30\n21. 타입?\n정답: int\n해설: 설명'},{page:2,text:'2\n서술형 31~34\n31. 설명하시오.\n예시 답안과 풀이\n정답: 첫 문장\n이어지는 모범 답안\n복습 포인트: 다시 보기\n32. 주소는?\n예시 답안과 풀이\n&x는 주소다.'}]);
 assert.deepEqual(questions.map(q=>q.type),['MC','SHORT','ESSAY','ESSAY']);
 assert.equal(questions[0].answer,'c2');assert.match(questions[0].title,/int x = 1;/);
 assert.match(questions[2].answer,/이어지는 모범 답안/);assert.match(questions[2].explanation,/복습 포인트/);
 assert.equal(questions[3].answer,'&x는 주소다.');
});
test('unresolved answers are flagged instead of invented',()=>{
 const {questions}=parseQuestionPages([{page:1,text:'객관식\n01. 질문\n1) A\n2) B\n정답: 9) 존재하지 않는 선지'}]);
 assert.equal(questions[0].answer,'');assert.ok(draftIssues(questions[0]).length);
 assert.ok(parseQuestionPages([{page:1,text:'문제 없는 자료'}]).warnings.length);
});
test('a question can span pages and OX answers are detected',()=>{
 const {questions}=parseQuestionPages([{page:1,text:'객관식\n01. 질문\n1) A'},{page:2,text:'2\n2) B\n정답: 2\n해설: 여러 쪽\nOX\n02. 참인가?\n정답: O'}]);
 assert.equal(questions[0].choices.length,2);assert.equal(questions[0].answer,'c2');assert.equal(questions[1].type,'OX');
});

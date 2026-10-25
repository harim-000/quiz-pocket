// Deterministic parser for numbered Korean question sheets; no generated answers.
const questionLine=/^(?:문제\s*)?(\d{1,3})[.．]\s+(.+)$/;
const sectionLine=/^(객관식|단답형|서술형|OX|O\s*\/\s*X)\s*(?:문제|문항)?\s*(?:\d+\s*[~～\-–]\s*\d+)?\s*$/i;
const kinds={'객관식':'MC','단답형':'SHORT','서술형':'ESSAY','OX':'OX','O/X':'OX'};
const choiceLine=/^(?:(\d{1,2})[)）]\s*|([①②③④⑤⑥⑦⑧⑨⑩])\s*|([A-J])[.)]\s+)(.*)$/;
const labelIndex=s=>{const match=s.trim().match(/^(?:(\d{1,2})\s*(?:[)）.]|번|$)|([①②③④⑤⑥⑦⑧⑨⑩])|([A-J])(?:[.)]|$))/);return match?(match[1]?Number(match[1]):match[2]?'①②③④⑤⑥⑦⑧⑨⑩'.indexOf(match[2])+1:match[3].charCodeAt(0)-64):null};
export function draftIssues(q){
 const issues=[];
 if(!q.title?.trim())issues.push('질문을 입력해 주세요.');
 if((q.title||'').length>10000)issues.push('질문이 10,000자를 초과해요.');
 if(!q.answer?.trim())issues.push('정답 또는 모범 답안을 입력해 주세요.');
 if((q.answer||'').length>10000||(q.explanation||'').length>10000)issues.push('답안 또는 해설이 너무 길어요.');
 for(const [key,max] of [['keywords',200],['aliases',500]])if((q[key]||[]).filter(x=>x.trim()).length>30||(q[key]||[]).some(x=>x.trim().length>max))issues.push('키워드·허용 정답의 개수와 길이를 확인해 주세요.');
 if(q.type==='MC'){
  if(q.choices.length<2||q.choices.length>10)issues.push('객관식 선지는 2~10개여야 해요.');
  if(q.choices.some(c=>!c.text.trim()||c.text.length>5000))issues.push('빈 선지 또는 너무 긴 선지를 확인해 주세요.');
  if(!q.choices.some(c=>c.id===q.answer))issues.push('정답 선지를 선택해 주세요.');
 }
 if(q.type==='OX'&&!['O','X'].includes(q.answer))issues.push('O 또는 X를 선택해 주세요.');
 return issues;
}
export function parseQuestionPages(pages){
 let section=null,current=null;const blocks=[],warnings=[];
 for(const page of pages){
  const lines=page.text.replace(/\r/g,'').split('\n');
  // Only remove standalone edge page numbers, never numeric answers within a question.
  while(lines.length&&!lines[0].trim())lines.shift();
  if(lines[0]?.trim()===String(page.page))lines.shift();
  while(lines.length&&!lines.at(-1).trim())lines.pop();
  if(lines.at(-1)?.trim()===String(page.page))lines.pop();
  for(const raw of lines){
   const line=raw.trim();if(!line)continue;
   const header=line.match(sectionLine);
   if(header){section=kinds[header[1].replace(/\s/g,'').toUpperCase()]||kinds[header[1]];current=null;continue;}
   const start=line.match(questionLine);
   if(start){current={number:Number(start[1]),page:page.page,type:section,title:[start[2]],choices:[],answer:[],explanation:[],mode:'title',model:false};blocks.push(current);continue;}
   if(!current)continue;
   if(/^예시\s*답안(?:과\s*풀이)?\s*[:：]?$/.test(line)||/^모범\s*답안\s*[:：]?$/.test(line)){current.model=true;current.mode='answer';continue;}
   const answer=line.match(/^(?:정답|답)\s*[:：]\s*(.*)$/);
   if(answer){current.mode='answer';if(answer[1])current.answer.push(answer[1]);continue;}
   const explain=line.match(/^(해설|풀이|복습\s*포인트)\s*[:：]\s*(.*)$/);
   if(explain){current.mode='explanation';current.explanation.push(explain[1].startsWith('복습')?line:explain[2]);continue;}
   const choice=line.match(choiceLine);
   if(choice&&(current.mode==='title'||current.mode==='choice')&&current.type!=='ESSAY'&&current.type!=='SHORT'&&current.type!=='OX'){
    const number=choice[1]?Number(choice[1]):choice[2]?'①②③④⑤⑥⑦⑧⑨⑩'.indexOf(choice[2])+1:choice[3].charCodeAt(0)-64;
    current.choices.push({id:'c'+number,number,text:choice[4]});current.mode='choice';continue;
   }
   if(current.mode==='choice')current.choices.at(-1).text+='\n'+raw.trimEnd();
   else current[current.mode].push(raw.trimEnd());
  }
 }
 const numbers=new Set();
 const questions=blocks.map((b,index)=>{
  const rawAnswer=b.answer.join('\n').trim();
  const type=b.type||(b.choices.length>=2?'MC':b.model?'ESSAY':/^[OXox○×]$/.test(rawAnswer)?'OX':'SHORT');
  let answer=rawAnswer;
  if(type==='MC'){
   const number=labelIndex(rawAnswer);const found=b.choices.find(c=>c.number===number)||b.choices.find(c=>c.text.trim()===rawAnswer);
   answer=found?.id||'';
  }else if(type==='OX')answer=rawAnswer.trim().toUpperCase().replace('○','O').replace('×','X');
  const q={id:'pdf-'+index,number:b.number,sourcePage:b.page,type,title:b.title.join('\n').trim(),choices:b.choices.map(({id,text})=>({id,text:text.trim()})),answer,explanation:b.explanation.join('\n').trim(),keywords:[],aliases:[]};
  q.warnings=draftIssues(q);
  if(numbers.has(b.number))q.warnings.push('중복된 문제 번호예요. 내용을 비교해 주세요.');numbers.add(b.number);
  if(type==='ESSAY')q.warnings.push('모범 답안과 키워드를 직접 검토해 주세요.');
  return q;
 });
 if(!questions.length)warnings.push('문제를 찾지 못했어요. 이 버전은 “01. 질문”, “1) 선지”, “정답:”, “해설:” 형식을 지원해요. 스캔 PDF는 OCR이 필요해요.');
 return {questions,warnings};
}

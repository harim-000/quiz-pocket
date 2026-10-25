export function changeQuestionType(question,type){
 if(question.type===type)return question;
 const choices=question.choices?.length>=2?question.choices:['a','b','c','d'].map(id=>({id,text:''}));
 return {...question,type,choices,answer:type==='MC'?choices[0].id:type==='OX'?'O':'',keywords:question.keywords||[],aliases:question.aliases||[]};
}

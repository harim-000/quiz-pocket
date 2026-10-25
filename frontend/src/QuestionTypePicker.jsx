import React from 'react';
const types={MC:'객관식',SHORT:'단답형',ESSAY:'서술형',OX:'OX'};
export default function QuestionTypePicker({value,onChange,disabled=false,name='question-type'}){
 return <fieldset className="type-picker"><legend>문제 유형</legend><div>{Object.entries(types).map(([type,label])=><label key={type} className={value===type?'selected':''}><input type="radio" name={name} value={type} checked={value===type} disabled={disabled} onChange={()=>onChange(type)}/><span>{label}</span></label>)}</div></fieldset>;
}

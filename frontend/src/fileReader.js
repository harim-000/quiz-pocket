import {parseQuestionPages} from './pdfParser.js';
export const ACCEPTED_FILES='.txt,.pdf,.docx,text/plain,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document';
export function fileKind(name){const extension=name.match(/\.(txt|pdf|docx)$/i)?.[1].toLowerCase();if(!['txt','pdf','docx'].includes(extension))throw Error('TXT, PDF, DOCX 파일만 가져올 수 있어요.');return extension;}
export function decodeText(bytes){
 let text;
 try{text=new TextDecoder('utf-8',{fatal:true}).decode(bytes);}catch{try{text=new TextDecoder('euc-kr',{fatal:true}).decode(bytes);}catch{throw Error('텍스트 인코딩을 읽지 못했어요. UTF-8로 저장한 TXT를 선택해 주세요.');}}
 if(/[\u0000-\u0008\u000e-\u001f]/.test(text))throw Error('텍스트 파일이 아니거나 내용이 손상됐어요.');
 return text.replace(/^\uFEFF/,'');
}
export async function docxText(bytes){
 if(bytes[0]!==0x50||bytes[1]!==0x4b)throw Error('올바른 DOCX 문서가 아니에요.');
 const {unzipSync}=await import('fflate');let content;
 try{
  const entries=unzipSync(bytes,{filter:entry=>{
   if(entry.name!=='word/document.xml')return false;
   if(entry.originalSize>5*1024*1024)throw Error('expanded-limit');
   return true;
  }});content=entries['word/document.xml'];
 }catch{throw Error('DOCX가 손상됐거나 암호가 걸렸거나, 문서 내용이 너무 커요.');}
 if(!content)throw Error('DOCX 본문을 찾을 수 없어요. .doc가 아닌 .docx 문서인지 확인해 주세요.');
 const doc=new DOMParser().parseFromString(new TextDecoder().decode(content),'application/xml');
 if(doc.getElementsByTagName('parsererror').length)throw Error('DOCX 본문 형식이 올바르지 않아요.');
 const textOf=node=>{
  if(node.localName==='t')return node.textContent||'';
  if(['br','cr'].includes(node.localName))return '\n';
  if(node.localName==='tab')return '\t';
  return [...node.childNodes].map(textOf).join('');
 };
 return [...doc.getElementsByTagNameNS('*','p')].map(textOf).join('\n');
}
async function pdfPages(bytes,onProgress){
 const start=new TextDecoder('ascii').decode(bytes.slice(0,1024));if(!start.includes('%PDF-'))throw Error('올바른 PDF 파일이 아니에요.');
 const pdfjs=await import('pdfjs-dist/build/pdf.mjs');
 const worker=await import('pdfjs-dist/build/pdf.worker.min.mjs?url');pdfjs.GlobalWorkerOptions.workerSrc=worker.default;
 const task=pdfjs.getDocument({data:bytes,isEvalSupported:false,useSystemFonts:true});
 const timer=setTimeout(()=>task.destroy(),90000);
 try{
  const doc=await task.promise;if(doc.numPages>80)throw Error('80쪽 이하의 PDF를 선택해 주세요.');
  const pages=[];let total=0;
  for(let n=1;n<=doc.numPages;n++){
   onProgress(`${n} / ${doc.numPages}쪽 읽는 중…`);
   const page=await doc.getPage(n),content=await page.getTextContent();let text='',lastY=null,lastEnd=null;
   for(const item of content.items){
    if(typeof item.str!=='string')continue;
    const x=item.transform[4],y=item.transform[5];
    if(lastY!==null&&Math.abs(y-lastY)>3&&!text.endsWith('\n'))text+='\n';
    else if(lastEnd!==null&&x-lastEnd>2&&!/\s$/.test(text))text+=' ';
    text+=item.str;if(item.hasEOL)text+='\n';lastY=y;lastEnd=x+item.width;
   }
   total+=text.length;if(total>500000)throw Error('추출한 텍스트가 너무 많아요. 단원별로 나눠 주세요.');
   pages.push({page:n,text});page.cleanup();
  }
  if(total<20)throw Error('읽을 수 있는 텍스트가 없어요. 스캔 PDF는 현재 지원하지 않아요.');
  return pages;
 }finally{clearTimeout(timer);await task.destroy();}
}
export async function readQuestionFile(file,onProgress=()=>{}){
 const kind=fileKind(file.name);if(file.size>15*1024*1024)throw Error('15MB 이하의 파일을 선택해 주세요.');
 if(!file.size)throw Error('빈 파일이에요.');
 const bytes=new Uint8Array(await file.arrayBuffer());let pages;
 if(kind==='pdf')pages=await pdfPages(bytes,onProgress);
 else{
  onProgress(kind==='docx'?'DOCX 본문 읽는 중…':'텍스트 읽는 중…');
  const text=kind==='docx'?await docxText(bytes):decodeText(bytes);
  if(text.length>500000)throw Error('추출한 텍스트가 너무 많아요. 단원별로 나눠 주세요.');
  pages=[{page:1,text}];
 }
 const result=parseQuestionPages(pages);
 return {...result,kind,questions:result.questions.map(q=>({...q,sourcePage:kind==='pdf'?q.sourcePage:null}))};
}

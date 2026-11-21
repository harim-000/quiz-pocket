// 문제별 가장 최근 기록이 오답인 문제 id만 "누적 오답"으로 본다.
// 오답이었던 문제를 다시 풀어 맞히면 최신 기록이 정답이 되어 자동으로 빠진다.
export function wrongIds(history){
  const latest=new Map();
  for(const h of history)latest.set(h.id,h.correct);
  return new Set([...latest].filter(([,ok])=>!ok).map(([id])=>id));
}

export function wrongBySubject(history,questions){
  const ids=wrongIds(history);
  const counts={};
  for(const q of questions)if(ids.has(q.id))counts[q.subject]=(counts[q.subject]||0)+1;
  return counts;
}

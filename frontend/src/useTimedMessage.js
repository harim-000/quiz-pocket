import {useEffect,useState,useCallback} from 'react';
export function useTimedMessage(duration=5000){
 const [message,setMessage]=useState({text:''});
 const show=useCallback(text=>setMessage({text}),[]);
 useEffect(()=>{if(!message.text)return;const timer=setTimeout(()=>setMessage({text:''}),duration);return()=>clearTimeout(timer);},[message,duration]);
 return [message.text,show];
}

#!/usr/bin/env python3
"""Build app/src/main/assets/mushaf-lines.json: the Madinah mushaf line layout.
Source: quran-library data/mushaf/pages (word -> page/line, 1421H print).
Only LINE BREAKS are taken from it; the Quran text stays app's quran-simple.txt.
Each app token is mapped to the library word it belongs to by edit-distance DP on
base-letter skeletons (the app splits 'يا' from the vocative, the print joins them).
usage: build_lines.py <quran-library-dir> <quran-simple.txt> <out.json>"""
import json, sys, unicodedata
lib_dir, qtxt, out = sys.argv[1:4]
MAP = {'ٱ':'ا','أ':'ا','إ':'ا','آ':'ء','ى':'ي','ئ':'ء','ؤ':'ء','ة':'ه'}
def skel(s):
    r=[]
    for c in s:
        if unicodedata.category(c)=='Lo' and c!='ـ': r.append(MAP.get(c,c))
        elif c in '\u0654\u0655': r.append('ء')   # hamza mark
    return ''.join(r).replace('ا','')  # alefs vary in print; compare consonants
def isletter(t): return any(unicodedata.category(c).startswith('L') for c in t)
def ed(a,b):
    p=list(range(len(b)+1))
    for i,x in enumerate(a,1):
        q=[i]
        for j,y in enumerate(b,1): q.append(min(p[j]+1,q[-1]+1,p[j-1]+(x!=y)))
        p=q
    return p[-1]
ours={}
for ln in open(qtxt,encoding='utf8'):
    parts=ln.rstrip('\n').split('|',2)
    if len(parts)==3 and parts[0].isdigit(): ours[(int(parts[0]),int(parts[1]))]=parts[2].split()
pages=[json.load(open(f'{lib_dir}/data/mushaf/pages/page-{n:03d}.json')) for n in range(1,605)]
libw={}
for p in pages:
    for l in p['lines']:
        if l['type']=='words':
            for w in l['words']:
                if not w['end']: libw.setdefault((w['surah'],w['ayah']),[]).append(w)
# our token indexes that are real words (letters); marks attach to previous word
align={}   # (s,a) -> list over our tokens of lib word index (pos-1)
bad=[]
for k,toks in ours.items():
    s,a=k
    start=4 if a==1 and s not in (1,9) and len(toks)>4 and skel(toks[0])==skel('بسم') else 0
    toks=toks[start:]; ours[k]=toks; start=0
    lw=libw[k]
    widx=[i for i in range(start,len(toks)) if isletter(toks[i])]
    A=[skel(toks[i]) for i in widx]; B=[skel(w['text']) for w in lw]
    n,m=len(A),len(B); INF=10**9
    D=[[INF]*(m+1) for _ in range(n+1)]; bk={}
    D[0][0]=0
    for i in range(n):
        for j in range(m):
            if D[i][j]>=INF: continue
            for kk in (1,2,3):
                if i+kk>n: break
                c=D[i][j]+ed(''.join(A[i:i+kk]),B[j])+(kk-1)*0
                if c<D[i+kk][j+1]: D[i+kk][j+1]=c; bk[(i+kk,j+1)]=(i,j)
    if D[n][m]>=INF or D[n][m]>max(3,m//3): bad.append((k,D[n][m])); continue
    asg=[0]*n; i,j=n,m
    while (i,j)!=(0,0):
        pi,pj=bk[(i,j)]
        for t in range(pi,i): asg[t]=pj
        i,j=pi,pj
    a_=[-1]*len(toks); cur=0
    for t,idx in enumerate(widx): a_[idx]=asg[t]
    last=0
    for t in range(len(toks)):
        if a_[t]<0: a_[t]=last
        else: last=a_[t]
    align[k]=a_
if bad: print('UNALIGNED',len(bad),bad[:10]); sys.exit(1)
res=[]
nt={k:len(v) for k,v in ours.items()}
for p in pages:
    lines=[]
    for l in p['lines']:
        if l['type']=='header': lines.append({'k':'h','s':l['surah']}); continue
        if l['type']=='basmala': lines.append({'k':'b'}); continue
        segs=[]  # [surah, ayah, tokFrom, tokTo(excl)] in app tokens (ayah-end marker implied when tokTo==len)
        for w in l['words']:
            if w['end']: continue
            k=(w['surah'],w['ayah']); al=align[k]; j=w['pos']-1
            toks=[t for t,x in enumerate(al) if x==j]
            if not toks: print('EMPTY',k,j); sys.exit(1)
            t0,t1=toks[0],toks[-1]+1
            if segs and segs[-1][0]==k[0] and segs[-1][1]==k[1] and segs[-1][3]==t0: segs[-1][3]=t1
            else: segs.append([k[0],k[1],t0,t1])
        lines.append({'k':'w','c':1 if l.get('centered') else 0,'g':segs})
    res.append(lines)
json.dump({'v':1,'pages':res},open(out,'w'),ensure_ascii=False,separators=(',',':'))
print('ok',len(res))

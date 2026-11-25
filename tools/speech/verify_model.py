"""يتحقق في CI من النموذج المرشح (Whisper base مضبوط على القرآن، ONNX): هل يعمل، هل يخرج حركات،
وما نسبة تطابقه مع نص القرآن الموثّق؟ لا يُحفظ شيء في المستودع."""
import json, subprocess, sys, time, unicodedata, urllib.request, os, re
import numpy as np
import onnxruntime as ort
from transformers import WhisperFeatureExtractor, WhisperTokenizer

W = "/tmp/model"
os.makedirs(W, exist_ok=True)

def fetch(url, dest):
    if not os.path.exists(dest):
        req = urllib.request.Request(url, headers={"User-Agent": "mushafi-ci"})
        with urllib.request.urlopen(req, timeout=300) as r, open(dest, "wb") as f:
            f.write(r.read())

ONNX = "https://huggingface.co/seferdemir/whisper-base-ar-quran-onnx/resolve/main/"
BASE = "https://huggingface.co/tarteel-ai/whisper-base-ar-quran/resolve/main/"
for n in ("encoder_uint8.onnx", "decoder_int8.onnx"):
    fetch(ONNX + n, f"{W}/{n}")
for n in ("preprocessor_config.json", "vocab.json", "merges.txt", "tokenizer_config.json",
          "added_tokens.json", "special_tokens_map.json", "normalizer.json", "config.json"):
    fetch(BASE + n, f"{W}/{n}")
print("sizes MB:", {n: round(os.path.getsize(f"{W}/{n}") / 1e6, 1) for n in ("encoder_uint8.onnx", "decoder_int8.onnx")})

fe = WhisperFeatureExtractor.from_pretrained(W)
tok = WhisperTokenizer.from_pretrained(W)
enc = ort.InferenceSession(f"{W}/encoder_uint8.onnx", providers=["CPUExecutionProvider"])
dec = ort.InferenceSession(f"{W}/decoder_int8.onnx", providers=["CPUExecutionProvider"])
print("encoder inputs", [i.name for i in enc.get_inputs()], "decoder inputs", [i.name for i in dec.get_inputs()])

quran = {}
for line in open("app/src/main/assets/quran-simple.txt", encoding="utf-8"):
    parts = line.rstrip("\n").split("|", 2)
    if len(parts) != 3 or not parts[0].isdigit():
        continue
    quran[(int(parts[0]), int(parts[1]))] = parts[2]

def strip(t):
    t = unicodedata.normalize("NFKD", t)
    t = re.sub(r"[ً-ٰٟۖ-ۭـ]", "", t)
    t = t.replace("ٱ", "ا").replace("أ", "ا").replace("إ", "ا").replace("آ", "ا").replace("ى", "ي").replace("ة", "ه")
    return t

def wer(ref, hyp):
    r, h = ref.split(), hyp.split()
    d = list(range(len(h) + 1))
    for i in range(1, len(r) + 1):
        prev, d[0] = d[0], i
        for j in range(1, len(h) + 1):
            cur = d[j]
            d[j] = min(d[j] + 1, d[j - 1] + 1, prev + (r[i - 1] != h[j - 1]))
            prev = cur
    return d[len(h)] / max(1, len(r))

def transcribe(path):
    raw = subprocess.run(["ffmpeg", "-v", "quiet", "-i", path, "-f", "f32le", "-ar", "16000", "-ac", "1", "-"],
                         capture_output=True, check=True).stdout
    audio = np.frombuffer(raw, dtype=np.float32)
    feats = fe(audio, sampling_rate=16000, return_tensors="np").input_features.astype(np.float32)
    t0 = time.time()
    hidden = enc.run(None, {"input_features": feats})[0]
    ids = [50258, 50272, 50359, 50363]
    for _ in range(160):
        logits = dec.run(None, {"input_ids": np.array([ids], dtype=np.int64), "encoder_hidden_states": hidden})[0]
        nxt = int(logits[0, -1].argmax())
        if nxt == 50257:
            break
        ids.append(nxt)
    return tok.decode(ids[4:], skip_special_tokens=True), time.time() - t0, len(audio) / 16000

folder = "Alafasy_64kbps"
for s, a in [(1, 2), (112, 1), (112, 2), (36, 1), (2, 255), (114, 4)]:
    mp3 = f"/tmp/{s:03d}{a:03d}.mp3"
    fetch(f"https://everyayah.com/data/{folder}/{s:03d}{a:03d}.mp3", mp3)
    text, secs, dur = transcribe(mp3)
    ref = quran[(s, a)]
    if a == 1 and s not in (1, 9):
        ref = ref  # الملف الموثّق يبدأ بالبسملة في الآية الأولى
    has_marks = bool(re.search(r"[ً-ْٰ]", text))
    print(f"--- {s}:{a} audio {dur:.1f}s infer {secs:.1f}s diacritics_in_output={has_marks}")
    print("model:", text)
    print("ref  :", ref)
    print("WER no-harakat: %.2f" % wer(strip(ref), strip(text)))
    print("WER with harakat: %.2f" % wer(ref, text))

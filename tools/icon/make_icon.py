"""يولّد أيقونة «مصحفي»: رسم متجهي للتطبيق (adaptive icon) وصورة 512 للمتجر من نفس الأشكال."""
import math, os
from PIL import Image, ImageDraw

GOLD = "#D9B45A"
GOLD_DARK = "#B8923A"
CREAM = "#F5EDD6"
GREEN_IN = "#145C45"
GREEN_OUT = "#082A20"
C = 54.0  # مركز الإطار 108

def petals():
    return [(C + 25 * math.cos(math.radians(a)), C + 25 * math.sin(math.radians(a))) for a in range(0, 360, 30)]

def circle_path(cx, cy, r):
    return f"M{cx - r:.2f},{cy:.2f}a{r:.2f},{r:.2f} 0 1,0 {2 * r:.2f},0a{r:.2f},{r:.2f} 0 1,0 {-2 * r:.2f},0"

def page_left():
    return "M54,64 C49,61 42,60 37,61.5 L37,44 C42,42.5 49,43.5 54,47 Z"

def page_right():
    return "M54,64 C59,61 66,60 71,61.5 L71,44 C66,42.5 59,43.5 54,47 Z"

def lines(side):
    out = []
    for y in (49, 53, 57):
        if side == "l":
            out.append(f"M41,{y} L50,{y + 1.5}")
        else:
            out.append(f"M67,{y} L58,{y + 1.5}")
    return " ".join(out)

def foreground():
    rosette = " ".join(circle_path(x, y, 6.2) for x, y in petals())
    return f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:fillColor="{GOLD}" android:pathData="{rosette}" />
    <path android:fillColor="{GOLD_DARK}" android:pathData="{circle_path(C, C, 25)}" />
    <path android:fillColor="{GREEN_IN}" android:pathData="{circle_path(C, C, 21.5)}" />
    <path android:fillColor="#00000000" android:strokeColor="{GOLD}" android:strokeWidth="1.1"
        android:pathData="{circle_path(C, C, 19.2)}" />
    <path android:fillColor="{CREAM}" android:pathData="{page_left()}" />
    <path android:fillColor="{CREAM}" android:pathData="{page_right()}" />
    <path android:fillColor="#00000000" android:strokeColor="{GOLD_DARK}" android:strokeWidth="0.9"
        android:strokeLineCap="round" android:pathData="M54,47 L54,64 {lines('l')} {lines('r')}" />
</vector>
'''

def background():
    return f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor" xmlns:aapt="http://schemas.android.com/aapt">
            <gradient android:type="radial" android:centerX="54" android:centerY="54"
                android:gradientRadius="76" android:startColor="{GREEN_IN}" android:endColor="{GREEN_OUT}" />
        </aapt:attr>
    </path>
</vector>
'''

ADAPTIVE = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
'''

def bez(p0, p1, p2, p3, n=24):
    pts = []
    for i in range(n + 1):
        t = i / n
        x = (1 - t) ** 3 * p0[0] + 3 * (1 - t) ** 2 * t * p1[0] + 3 * (1 - t) * t * t * p2[0] + t ** 3 * p3[0]
        y = (1 - t) ** 3 * p0[1] + 3 * (1 - t) ** 2 * t * p1[1] + 3 * (1 - t) * t * t * p2[1] + t ** 3 * p3[1]
        pts.append((x, y))
    return pts

def render_png(path, size=512):
    k = size / 108.0
    img = Image.new("RGB", (size, size), GREEN_OUT)
    px = img.load()
    for y in range(size):
        for x in range(size):
            d = math.hypot(x / k - C, y / k - C) / 76.0
            d = min(1.0, d)
            a = tuple(int(GREEN_IN[i:i + 2], 16) for i in (1, 3, 5))
            b = tuple(int(GREEN_OUT[i:i + 2], 16) for i in (1, 3, 5))
            px[x, y] = tuple(int(a[j] + (b[j] - a[j]) * d) for j in range(3))
    d = ImageDraw.Draw(img)
    def circ(cx, cy, r, fill=None, outline=None, w=1):
        d.ellipse([(cx - r) * k, (cy - r) * k, (cx + r) * k, (cy + r) * k], fill=fill, outline=outline, width=int(w * k))
    for x, y in petals():
        circ(x, y, 6.2, fill=GOLD)
    circ(C, C, 25, fill=GOLD_DARK)
    circ(C, C, 21.5, fill=GREEN_IN)
    circ(C, C, 19.2, outline=GOLD, w=1.1)
    left = bez((54, 64), (49, 61), (42, 60), (37, 61.5)) + [(37, 44)] + bez((37, 44), (42, 42.5), (49, 43.5), (54, 47))
    right = bez((54, 64), (59, 61), (66, 60), (71, 61.5)) + [(71, 44)] + bez((71, 44), (66, 42.5), (59, 43.5), (54, 47))
    for poly in (left, right):
        d.polygon([(x * k, y * k) for x, y in poly], fill=CREAM)
    d.line([(54 * k, 47 * k), (54 * k, 64 * k)], fill=GOLD_DARK, width=max(1, int(0.9 * k)))
    for yy in (49, 53, 57):
        d.line([(41 * k, yy * k), (50 * k, (yy + 1.5) * k)], fill=GOLD_DARK, width=max(1, int(0.9 * k)))
        d.line([(67 * k, yy * k), (58 * k, (yy + 1.5) * k)], fill=GOLD_DARK, width=max(1, int(0.9 * k)))
    img.save(path)

if __name__ == "__main__":
    res = "app/src/main/res"
    os.makedirs(f"{res}/mipmap-anydpi", exist_ok=True)
    open(f"{res}/drawable/ic_launcher_foreground.xml", "w", encoding="utf-8").write(foreground())
    open(f"{res}/drawable/ic_launcher_background.xml", "w", encoding="utf-8").write(background())
    open(f"{res}/mipmap-anydpi/ic_launcher.xml", "w", encoding="utf-8").write(ADAPTIVE)
    open(f"{res}/mipmap-anydpi/ic_launcher_round.xml", "w", encoding="utf-8").write(ADAPTIVE)
    os.makedirs("docs/store", exist_ok=True)
    render_png("docs/store/icon-512.png", 512)
    print("done")

from PIL import Image, ImageDraw
import io
from typing import Optional

AP_COLORS = {
    0: (50, 102, 173),    # 青
    1: (142, 68, 173),    # 紫
    2: (22, 160, 133),    # 緑
    3: (211, 84, 0),      # オレンジ
    4: (41, 128, 185),    # 水色
}

def rssi_to_color(rssi: int, alpha: int = 220):
    if rssi >= -65: return (29, 158, 117, alpha)
    if rssi >= -75: return (239, 159, 39, alpha)
    return (216, 90, 48, alpha)

def plot_survey_map(
    floor_map_bytes: bytes,
    aps: list,
    snapshots: list,
    show_connection_lines: bool = True,
    show_rssi_labels: bool = True,
) -> bytes:
    """フロアマップに測定点とAPをプロットしてPNG bytesを返す"""

    img = Image.open(io.BytesIO(floor_map_bytes)).convert('RGBA')
    W, H = img.size
    overlay = Image.new('RGBA', (W, H), (0,0,0,0))
    draw = ImageDraw.Draw(overlay)

    # AP名→座標マップ
    ap_positions = {ap.name: (int(ap.map_x * W), int(ap.map_y * H)) for ap in aps}

    # 接続線を描画
    if show_connection_lines:
        for sp in snapshots:
            if sp.map_x is None or sp.map_y is None:
                continue
            sx, sy = int(sp.map_x * W), int(sp.map_y * H)
            if sp.connected_ap in ap_positions:
                apx, apy = ap_positions[sp.connected_ap]
                connected_rssi = sp.ap_best_rssi.get(sp.connected_ap, -99)
                color = rssi_to_color(connected_rssi, 160)
                # 破線
                dx, dy = apx - sx, apy - sy
                dist = (dx**2 + dy**2) ** 0.5
                if dist > 0:
                    steps = max(int(dist / 12), 1)
                    for i in range(0, steps, 2):
                        x1 = int(sx + dx * i / steps)
                        y1 = int(sy + dy * i / steps)
                        x2 = int(sx + dx * min(i+1, steps) / steps)
                        y2 = int(sy + dy * min(i+1, steps) / steps)
                        draw.line([(x1,y1),(x2,y2)], fill=color, width=2)

    # APを三角形で描画
    for idx, ap in enumerate(aps):
        px, py = int(ap.map_x * W), int(ap.map_y * H)
        color = AP_COLORS.get(idx % len(AP_COLORS), (100,100,100))
        size = 16
        pts = [
            (px, py-size),
            (px+int(size*0.85), py+int(size*0.7)),
            (px-int(size*0.85), py+int(size*0.7))
        ]
        draw.polygon(pts, fill=(*color, 230))
        draw.polygon(pts, outline=(255,255,255,255))
        # ラベル
        label = ap.name.replace('AP_605H_','').replace('AP_','')[:12]
        lw = len(label) * 6
        draw.rectangle([px-lw//2-2, py+size+2, px+lw//2+2, py+size+14], fill=(0,0,0,180))
        draw.text((px-lw//2, py+size+2), label, fill=(255,255,255,255))

    # 測定点を描画
    for sp in snapshots:
        if sp.map_x is None or sp.map_y is None:
            continue
        sx, sy = int(sp.map_x * W), int(sp.map_y * H)
        connected_rssi = sp.ap_best_rssi.get(sp.connected_ap, -99)
        color = rssi_to_color(connected_rssi)
        r = 14

        # グラデーション円
        for gr in range(35, 0, -5):
            gc = rssi_to_color(connected_rssi, int(gr * 3))
            draw.ellipse([sx-gr, sy-gr, sx+gr, sy+gr], fill=gc)

        draw.ellipse([sx-r, sy-r, sx+r, sy+r], fill=color)
        draw.ellipse([sx-r, sy-r, sx+r, sy+r], outline=(255,255,255,255), width=2)

        # ローミング問題マーカー
        if sp.roaming_issue:
            draw.ellipse([sx-r-5, sy-r-5, sx+r+5, sy+r+5], outline=(255,50,50,255), width=3)

        # RSSIラベル
        if show_rssi_labels and connected_rssi > -999:
            label = str(connected_rssi)
            draw.rectangle([sx-14, sy-8, sx+14, sy+8], fill=(0,0,0,160))
            draw.text((sx-11, sy-7), label, fill=(255,255,255,255))

        # 時刻ラベル
        time_label = sp.name[-8:] if len(sp.name) >= 8 else sp.name
        draw.rectangle([sx-18, sy+r+2, sx+20, sy+r+14], fill=(0,0,0,180))
        draw.text((sx-16, sy+r+2), time_label, fill=(255,255,255,220))

    result = Image.alpha_composite(img, overlay).convert('RGB')
    buf = io.BytesIO()
    result.save(buf, format='PNG', quality=95)
    return buf.getvalue()

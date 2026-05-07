import zipfile
import json
import csv
import io
from collections import defaultdict
from dataclasses import dataclass, field
from typing import Optional

@dataclass
class ApData:
    name: str
    source: str
    mac: str
    model: str
    map_x: float
    map_y: float
    status: str

@dataclass
class SnapshotPoint:
    name: str
    timestamp: str
    map_x: Optional[float]
    map_y: Optional[float]
    connected_ssid: str
    connected_ap: str
    connected_bssid: str
    latitude: Optional[float]
    longitude: Optional[float]
    # AP単位集約データ (key=ap_name)
    ap_best_rssi: dict = field(default_factory=dict)
    # AP×Band単位 (key=(ap_name, band))
    ap_band_rssi: dict = field(default_factory=dict)
    # Band単位 (key=band) 全AP合計
    band_best_rssi: dict = field(default_factory=dict)
    # 生BSSIDデータ (分析用)
    raw_bssids: list = field(default_factory=list)
    # カバレッジ品質 (AP単位)
    good_aps: int = 0
    fair_aps: int = 0
    weak_aps: int = 0
    # ローミング問題
    roaming_issue: bool = False
    roaming_issue_detail: str = ""

@dataclass
class SurveyData:
    summary: dict
    floor_map_image: Optional[bytes]
    aps: list  # List[ApData]
    snapshots: list  # List[SnapshotPoint]
    map_width_m: Optional[float]
    map_height_m: Optional[float]

def load_survey_zip(zip_bytes: bytes) -> SurveyData:
    """ZIPバイト列を読み込んでSurveyDataを返す"""

    summary = {}
    floor_map_image = None
    aps = []
    raw_snapshots = defaultdict(list)

    with zipfile.ZipFile(io.BytesIO(zip_bytes)) as zf:
        for name in zf.namelist():
            data = zf.read(name)

            if name == 'summary.json':
                summary = json.loads(data.decode('utf-8'))

            elif name == 'floor_map.png':
                floor_map_image = data

            elif name == 'ap_locations.csv':
                reader = csv.DictReader(io.StringIO(data.decode('utf-8')))
                for row in reader:
                    try:
                        aps.append(ApData(
                            name=row.get('ap_name',''),
                            source=row.get('source',''),
                            mac=row.get('mac_address',''),
                            model=row.get('model',''),
                            map_x=float(row.get('map_x','0') or 0),
                            map_y=float(row.get('map_y','0') or 0),
                            status=row.get('status',''),
                        ))
                    except:
                        pass

            elif name == 'snapshots.csv':
                reader = csv.DictReader(io.StringIO(data.decode('utf-8')))
                for row in reader:
                    try:
                        rssi = int(row.get('rssi_dbm','0') or 0)
                        if rssi >= 0:
                            continue
                        raw_snapshots[row['snapshot_name']].append(row)
                    except:
                        pass

    # スナップショットをAP単位で集約
    snapshots = []
    for snap_name, rows in sorted(raw_snapshots.items()):
        r0 = rows[0]

        try:
            map_x = float(r0.get('map_x_relative','') or 0) or None
            map_y = float(r0.get('map_y_relative','') or 0) or None
        except:
            map_x = map_y = None

        try:
            lat = float(r0.get('latitude','') or 0) or None
            lon = float(r0.get('longitude','') or 0) or None
        except:
            lat = lon = None

        connected_ap = r0.get('connected_ap_name','')
        connected_ssid = r0.get('connected_ssid','')
        connected_bssid = r0.get('connected_bssid','')

        # AP単位で最良RSSIを集約
        ap_best = defaultdict(lambda: -999)
        ap_band_best = defaultdict(lambda: defaultdict(lambda: -999))
        band_best = defaultdict(lambda: -999)

        for r in rows:
            ap = r.get('aruba_ap_name') or r.get('mist_ap_name')
            if not ap:
                continue
            rssi = int(r.get('rssi_dbm', -999))
            band = r.get('band', '')

            if rssi > ap_best[ap]:
                ap_best[ap] = rssi
            if rssi > ap_band_best[ap][band]:
                ap_band_best[ap][band] = rssi
            if rssi > band_best[band]:
                band_best[band] = rssi

        # カバレッジ品質 (AP単位)
        ap_rssi_vals = [v for v in ap_best.values() if v > -999]
        good = sum(1 for v in ap_rssi_vals if v >= -65)
        fair = sum(1 for v in ap_rssi_vals if -75 <= v < -65)
        weak = sum(1 for v in ap_rssi_vals if v < -75)

        # ローミング問題検出
        connected_rssi = ap_best.get(connected_ap, -999)
        roaming_issue = False
        roaming_detail = ""
        if ap_best and connected_ap:
            best_ap_name = max(ap_best.items(), key=lambda x: x[1])
            if best_ap_name[0] != connected_ap and best_ap_name[1] - connected_rssi > 10:
                roaming_issue = True
                roaming_detail = (
                    f"Connected to {connected_ap} ({connected_rssi} dBm) "
                    f"but {best_ap_name[0]} is stronger ({best_ap_name[1]} dBm, "
                    f"+{best_ap_name[1]-connected_rssi} dBm)"
                )

        # band_best の -999 を None に変換
        band_best_clean = {
            b: (v if v > -999 else None)
            for b, v in band_best.items()
        }

        sp = SnapshotPoint(
            name=snap_name,
            timestamp=snap_name,
            map_x=map_x,
            map_y=map_y,
            connected_ssid=connected_ssid,
            connected_ap=connected_ap,
            connected_bssid=connected_bssid,
            latitude=lat,
            longitude=lon,
            ap_best_rssi=dict(ap_best),
            ap_band_rssi={ap: dict(bands) for ap, bands in ap_band_best.items()},
            band_best_rssi=band_best_clean,
            raw_bssids=rows,
            good_aps=good,
            fair_aps=fair,
            weak_aps=weak,
            roaming_issue=roaming_issue,
            roaming_issue_detail=roaming_detail,
        )
        snapshots.append(sp)

    map_width_m = summary.get('map_width_m') or summary.get('length_m')
    map_height_m = summary.get('map_height_m') or summary.get('breadth_m')

    return SurveyData(
        summary=summary,
        floor_map_image=floor_map_image,
        aps=aps,
        snapshots=snapshots,
        map_width_m=map_width_m,
        map_height_m=map_height_m,
    )


def get_roaming_indices(snapshots):
    """時刻順に並んだスナップショットリストからローミング発生インデックスを返す（0-indexed）"""
    indices = []
    for i in range(1, len(snapshots)):
        if snapshots[i].connected_ap != snapshots[i - 1].connected_ap:
            indices.append(i)
    return indices


def compute_unmanaged_interference(snapshots):
    """各スナップショットの管理外AP干渉データを返す"""
    result = []
    for s in snapshots:
        named_bssids = set()
        hidden_bssids = set()
        max_rssi = None
        for r in s.raw_bssids:
            if r.get('aruba_ap_name') or r.get('mist_ap_name'):
                continue
            try:
                rssi = int(r.get('rssi_dbm', 0) or 0)
            except Exception:
                continue
            if rssi >= 0:
                continue
            bssid = r.get('bssid', '')
            ssid = r.get('ssid', '')
            if ssid:
                named_bssids.add(bssid)
            else:
                hidden_bssids.add(bssid)
            if max_rssi is None or rssi > max_rssi:
                max_rssi = rssi
        result.append({
            'named': len(named_bssids),
            'hidden': len(hidden_bssids),
            'max_rssi': max_rssi,
        })
    return result


def compute_co_channel_interference(snapshots):
    """各スナップショットの同チャネル干渉データを返す"""
    result = []
    for s in snapshots:
        conn_bssid_lc = s.connected_bssid.lower() if s.connected_bssid else ''
        connected_ch = None
        for r in s.raw_bssids:
            if r.get('bssid', '').lower() == conn_bssid_lc:
                try:
                    connected_ch = int(r.get('channel', 0) or 0)
                except Exception:
                    pass
                break

        managed_ap_names = set()
        unmanaged_bssids = set()
        if connected_ch is not None:
            for r in s.raw_bssids:
                if r.get('bssid', '').lower() == conn_bssid_lc:
                    continue
                try:
                    ch = int(r.get('channel', 0) or 0)
                    rssi = int(r.get('rssi_dbm', 0) or 0)
                except Exception:
                    continue
                if ch != connected_ch or rssi >= 0:
                    continue
                ap_name = r.get('aruba_ap_name') or r.get('mist_ap_name')
                if ap_name:
                    managed_ap_names.add(ap_name)
                else:
                    bssid = r.get('bssid', '')
                    if bssid:
                        unmanaged_bssids.add(bssid)

        result.append({
            'channel': connected_ch,
            'managed': len(managed_ap_names),
            'unmanaged': len(unmanaged_bssids),
        })
    return result


def compute_optimal_ap_check(snapshots):
    """各スナップショットの最適AP選択チェックデータを返す"""
    result = []
    for s in snapshots:
        conn_bssid_lc = s.connected_bssid.lower() if s.connected_bssid else ''
        connected_ssid = s.connected_ssid

        connected_rssi = None
        for r in s.raw_bssids:
            if r.get('bssid', '').lower() == conn_bssid_lc:
                try:
                    connected_rssi = int(r.get('rssi_dbm', 0) or 0)
                except Exception:
                    pass
                if not connected_ssid:
                    connected_ssid = r.get('ssid', '')
                break

        best_rssi = None
        best_ap_name = None
        for r in s.raw_bssids:
            if connected_ssid and r.get('ssid', '') != connected_ssid:
                continue
            ap_name = r.get('aruba_ap_name') or r.get('mist_ap_name')
            if not ap_name:
                continue
            try:
                rssi = int(r.get('rssi_dbm', 0) or 0)
            except Exception:
                continue
            if rssi >= 0:
                continue
            if best_rssi is None or rssi > best_rssi:
                best_rssi = rssi
                best_ap_name = ap_name

        is_optimal = (best_ap_name is None or best_ap_name == s.connected_ap)

        result.append({
            'connected_rssi': connected_rssi,
            'best_rssi': best_rssi,
            'best_ap_name': best_ap_name,
            'is_optimal': is_optimal,
        })
    return result

import streamlit as st
import plotly.graph_objects as go
import plotly.express as px
import pandas as pd
import io
from analysis import load_survey_zip, SurveyData
from map_plot import plot_survey_map

st.set_page_config(
    page_title="Wi-Fi Survey Analyzer",
    page_icon="📶",
    layout="wide"
)

st.title("📶 Wi-Fi Survey Analyzer")
st.caption("Upload a survey ZIP exported from Wi-Fi Analyzer app")

# ファイルアップロード
uploaded = st.file_uploader(
    "Upload survey ZIP",
    type=['zip'],
    help="Export ZIP from the Wi-Fi Analyzer Android app (Map → Export button)"
)

if not uploaded:
    st.info("Upload a survey ZIP file to start analysis.")
    st.stop()

# データ読み込み
with st.spinner("Loading survey data..."):
    data = load_survey_zip(uploaded.read())

if not data.snapshots:
    st.error("No snapshot data found in ZIP.")
    st.stop()

# ========== サマリーカード ==========
st.subheader("Survey Summary")

roaming_issues = [s for s in data.snapshots if s.roaming_issue]
placed = [s for s in data.snapshots if s.map_x is not None]
connected_aps = set(s.connected_ap for s in data.snapshots if s.connected_ap)
roaming_events = 0
for i in range(1, len(data.snapshots)):
    if data.snapshots[i].connected_ap != data.snapshots[i-1].connected_ap:
        roaming_events += 1

col1, col2, col3, col4, col5 = st.columns(5)
col1.metric("Snapshots", len(data.snapshots))
col2.metric("Placed on Map", f"{len(placed)} / {len(data.snapshots)}")
col3.metric("Aruba APs", len(data.aps))
col4.metric("Roaming Events", roaming_events)
col5.metric(
    "Roaming Issues",
    len(roaming_issues),
    delta=f"{len(roaming_issues)} ⚠" if roaming_issues else "None",
    delta_color="inverse"
)

# ローミング問題アラート
for s in roaming_issues:
    st.warning(f"⚠ **{s.name[-8:]}**: {s.roaming_issue_detail}")

# ========== フロアマップ ==========
if data.floor_map_image and placed:
    st.subheader("Floor Map")

    col_opt1, col_opt2 = st.columns(2)
    show_lines = col_opt1.checkbox("Show connection lines", value=True)
    show_labels = col_opt2.checkbox("Show RSSI labels", value=True)

    map_img = plot_survey_map(
        data.floor_map_image,
        data.aps,
        [s for s in data.snapshots if s.map_x is not None],
        show_connection_lines=show_lines,
        show_rssi_labels=show_labels,
    )
    st.image(map_img, use_container_width=True)

    # 凡例
    st.caption(
        "🔺 = AP location | "
        "🟢 Good ≥ -65 dBm | "
        "🟡 Fair -65~-75 dBm | "
        "🔴 Weak < -75 dBm | "
        "🔴 Double ring = Roaming issue"
    )

# ========== Chart 1: 接続AP RSSI推移 ==========
st.subheader("Connected AP RSSI Along Path")

# 全APのカラーマップ
all_ap_names = list(set(
    ap for s in data.snapshots
    for ap in s.ap_best_rssi.keys()
))
ap_colors_map = {}
palette = px.colors.qualitative.Set2
for i, ap in enumerate(sorted(all_ap_names)):
    ap_colors_map[ap] = palette[i % len(palette)]

fig1 = go.Figure()

# 各APのRSSI推移（背景線）
for ap in sorted(all_ap_names):
    rssi_vals = [s.ap_best_rssi.get(ap) for s in data.snapshots]
    rssi_clean = [v if v and v > -999 else None for v in rssi_vals]
    fig1.add_trace(go.Scatter(
        x=[s.name[-8:] for s in data.snapshots],
        y=rssi_clean,
        mode='lines+markers',
        name=ap,
        line=dict(color=ap_colors_map[ap], width=1.5, dash='dot'),
        marker=dict(size=5),
        opacity=0.5,
        showlegend=True,
    ))

# 接続APのRSSI（太線・色付きマーカー）
for i, s in enumerate(data.snapshots):
    connected_rssi = s.ap_best_rssi.get(s.connected_ap)
    if connected_rssi and connected_rssi > -999:
        color = ap_colors_map.get(s.connected_ap, 'gray')
        marker_symbol = 'x' if s.roaming_issue else 'circle'
        fig1.add_trace(go.Scatter(
            x=[s.name[-8:]],
            y=[connected_rssi],
            mode='markers',
            marker=dict(
                size=14,
                color=color,
                symbol=marker_symbol,
                line=dict(color='white', width=2)
            ),
            name=f"Connected: {s.connected_ap}" if i == 0 else None,
            showlegend=(i == 0),
            hovertext=f"{s.name[-8:]}: {s.connected_ap} ({connected_rssi} dBm)",
            hoverinfo='text',
        ))

# しきい値線
fig1.add_hline(y=-65, line_dash="dash", line_color="green", opacity=0.4, annotation_text="-65 dBm")
fig1.add_hline(y=-75, line_dash="dash", line_color="red", opacity=0.4, annotation_text="-75 dBm")

fig1.update_layout(
    height=350,
    yaxis_title="RSSI (dBm)",
    xaxis_title="Measurement Point",
    yaxis=dict(range=[-95, -25]),
    legend=dict(orientation='h', yanchor='bottom', y=1.02),
    margin=dict(l=50, r=20, t=40, b=50),
)
st.plotly_chart(fig1, use_container_width=True)

# ========== Chart 2: AP間RSSI比較 ==========
st.subheader("AP RSSI Comparison per Point")

fig2 = go.Figure()
for ap in sorted(all_ap_names):
    rssi_vals = [s.ap_best_rssi.get(ap) for s in data.snapshots]
    rssi_clean = [v if v and v > -999 else None for v in rssi_vals]
    fig2.add_trace(go.Bar(
        name=ap,
        x=[s.name[-8:] for s in data.snapshots],
        y=rssi_clean,
        marker_color=ap_colors_map[ap],
    ))

fig2.update_layout(
    height=320,
    barmode='group',
    yaxis_title="Best RSSI (dBm)",
    yaxis=dict(range=[-95, -25]),
    legend=dict(orientation='h', yanchor='bottom', y=1.02),
    margin=dict(l=50, r=20, t=40, b=50),
)
st.plotly_chart(fig2, use_container_width=True)

# ========== Chart 3: バンド別RSSI ==========
st.subheader("Band RSSI per Point (AP-aggregated best)")

# 存在するバンドを動的に検出
all_bands = set()
for s in data.snapshots:
    all_bands.update(s.band_best_rssi.keys())
# 順序を固定
band_order = [b for b in ['2.4 GHz', '5 GHz', '6 GHz'] if b in all_bands]
band_colors = {'2.4 GHz': '#3266ad', '5 GHz': '#16a085', '6 GHz': '#8e44ad'}

if band_order:
    fig3 = go.Figure()
    for band in band_order:
        vals = [s.band_best_rssi.get(band) for s in data.snapshots]
        clean = [v if v is not None else None for v in vals]
        fig3.add_trace(go.Scatter(
            x=[s.name[-8:] for s in data.snapshots],
            y=clean,
            mode='lines+markers',
            name=band,
            line=dict(color=band_colors.get(band, 'gray'), width=2),
            marker=dict(size=7),
            connectgaps=False,
        ))
    fig3.add_hline(y=-65, line_dash="dash", line_color="green", opacity=0.3)
    fig3.add_hline(y=-75, line_dash="dash", line_color="red", opacity=0.3)
    fig3.update_layout(
        height=300,
        yaxis_title="Best RSSI (dBm)",
        yaxis=dict(range=[-95, -25]),
        legend=dict(orientation='h', yanchor='bottom', y=1.02),
        margin=dict(l=50, r=20, t=40, b=50),
    )
    st.plotly_chart(fig3, use_container_width=True)

# ========== Chart 4: カバレッジ品質 ==========
st.subheader("Coverage Quality per Point (AP-unit basis)")

fig4 = go.Figure()
fig4.add_trace(go.Bar(
    name='Good (≥ -65 dBm)',
    x=[s.name[-8:] for s in data.snapshots],
    y=[s.good_aps for s in data.snapshots],
    marker_color='#1D9E75',
))
fig4.add_trace(go.Bar(
    name='Fair (-65 ~ -75 dBm)',
    x=[s.name[-8:] for s in data.snapshots],
    y=[s.fair_aps for s in data.snapshots],
    marker_color='#EF9F27',
))
fig4.add_trace(go.Bar(
    name='Weak (< -75 dBm)',
    x=[s.name[-8:] for s in data.snapshots],
    y=[s.weak_aps for s in data.snapshots],
    marker_color='#D85A30',
))
fig4.update_layout(
    height=280,
    barmode='stack',
    yaxis_title="Number of APs",
    yaxis=dict(dtick=1),
    legend=dict(orientation='h', yanchor='bottom', y=1.02),
    margin=dict(l=50, r=20, t=40, b=50),
)
st.plotly_chart(fig4, use_container_width=True)

# ========== Chart 5: AP×Band ヒートマップ ==========
st.subheader("AP × Band RSSI Heatmap")

# AP×Band行列を作成
heatmap_data = []
for ap in sorted(all_ap_names):
    row = {'AP': ap}
    for band in band_order:
        vals = [
            s.ap_band_rssi.get(ap, {}).get(band)
            for s in data.snapshots
            if s.ap_band_rssi.get(ap, {}).get(band) is not None
            and s.ap_band_rssi.get(ap, {}).get(band) > -999
        ]
        row[band] = round(sum(vals)/len(vals)) if vals else None
    heatmap_data.append(row)

if heatmap_data and band_order:
    df_heat = pd.DataFrame(heatmap_data).set_index('AP')
    fig5 = go.Figure(go.Heatmap(
        z=df_heat.values.tolist(),
        x=df_heat.columns.tolist(),
        y=df_heat.index.tolist(),
        colorscale=[
            [0.0, '#D85A30'],
            [0.4, '#EF9F27'],
            [0.7, '#1D9E75'],
            [1.0, '#0a4f35'],
        ],
        zmin=-90, zmax=-30,
        text=[[f"{v} dBm" if v else "N/A" for v in row] for row in df_heat.values.tolist()],
        texttemplate="%{text}",
        colorbar=dict(title="dBm"),
    ))
    fig5.update_layout(
        height=200 + len(all_ap_names) * 60,
        margin=dict(l=120, r=20, t=20, b=50),
    )
    st.plotly_chart(fig5, use_container_width=True)

# ========== 詳細テーブル ==========
with st.expander("Detailed Data Table"):
    table_rows = []
    for s in data.snapshots:
        row = {
            'Time': s.name[-8:],
            'Connected AP': s.connected_ap,
            'Connected RSSI': s.ap_best_rssi.get(s.connected_ap, '-'),
            'Map X': f"{s.map_x:.3f}" if s.map_x else '-',
            'Map Y': f"{s.map_y:.3f}" if s.map_y else '-',
            'Good APs': s.good_aps,
            'Fair APs': s.fair_aps,
            'Weak APs': s.weak_aps,
            'Roaming Issue': '⚠' if s.roaming_issue else '✓',
        }
        for ap in sorted(all_ap_names):
            rssi = s.ap_best_rssi.get(ap)
            row[ap] = rssi if rssi and rssi > -999 else '-'
        table_rows.append(row)

    df_table = pd.DataFrame(table_rows)
    st.dataframe(df_table, use_container_width=True)

# ========== サマリーカード (Map情報) ==========
with st.expander("Map & AP Info"):
    st.json(data.summary)
    if data.aps:
        ap_df = pd.DataFrame([{
            'AP Name': ap.name,
            'Source': ap.source,
            'Model': ap.model,
            'Status': ap.status,
            'Map X': f"{ap.map_x:.3f}",
            'Map Y': f"{ap.map_y:.3f}",
        } for ap in data.aps])
        st.dataframe(ap_df, use_container_width=True)

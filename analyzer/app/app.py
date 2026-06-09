import streamlit as st
import plotly.graph_objects as go
import plotly.express as px
import pandas as pd
from analysis import (
    load_survey_zip,
    get_roaming_indices,
    compute_unmanaged_interference,
    compute_co_channel_interference,
    compute_optimal_ap_check,
)
from map_plot import plot_survey_map

st.set_page_config(
    page_title="Wi-Fi Survey Analyzer",
    page_icon="📶",
    layout="wide"
)

st.title("📶 Wi-Fi Survey Analyzer")
st.caption("Upload a survey ZIP exported from Wi-Fi Analyzer app")

uploaded = st.file_uploader(
    "Upload survey ZIP",
    type=['zip'],
    help="Export ZIP from the Wi-Fi Analyzer Android app (Map → Export button)"
)

if not uploaded:
    st.info("Upload a survey ZIP file to start analysis.")
    st.stop()

with st.spinner("Loading survey data..."):
    data = load_survey_zip(uploaded.read())

if not data.snapshots:
    st.error("No snapshot data found in ZIP.")
    st.stop()

# ========== サマリーカード ==========
st.subheader("Survey Summary")

roaming_issues = [s for s in data.snapshots if s.roaming_issue]
placed = [s for s in data.snapshots if s.map_x is not None]
roaming_indices = get_roaming_indices(data.snapshots)

col1, col2, col3, col4, col5 = st.columns(5)
col1.metric("Snapshots", len(data.snapshots))
col2.metric("Placed on Map", f"{len(placed)} / {len(data.snapshots)}")
col3.metric("Aruba APs", len(data.aps))
col4.metric("Roaming Events", len(roaming_indices))
col5.metric(
    "Roaming Issues",
    len(roaming_issues),
    delta=f"{len(roaming_issues)} ⚠" if roaming_issues else "None",
    delta_color="inverse"
)

for s in roaming_issues:
    st.warning(f"⚠ **{s.name[-8:]}**: {s.roaming_issue_detail}")

# ========== 共通 X 軸ラベル ==========
x_labels = [
    f"{s.name[-8:]}<br>[{s.connected_ap or '?'}]"
    for s in data.snapshots
]


def _add_roaming_markers(fig, labels):
    """ローミング発生点に縦破線を追加し、凡例エントリを1つ追加する"""
    for ri in roaming_indices:
        if ri < len(labels):
            fig.add_vline(
                x=labels[ri],
                line=dict(color='#FF6B35', width=1.5, dash='dash'),
                opacity=0.7,
            )
    if roaming_indices:
        fig.add_trace(go.Scatter(
            x=[None], y=[None],
            mode='lines',
            name='Roaming',
            line=dict(color='#FF6B35', width=1.5, dash='dash'),
            showlegend=True,
        ))


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
    st.caption(
        "🔺 = AP location | "
        "🟢 Good ≥ -65 dBm | "
        "🟡 Fair -65~-75 dBm | "
        "🔴 Weak < -75 dBm | "
        "🔴 Double ring = Roaming issue"
    )

# ========== 全APカラーマップ ==========
all_ap_names = list(set(
    ap for s in data.snapshots
    for ap in s.ap_best_rssi.keys()
))
ap_colors_map = {}
palette = px.colors.qualitative.Set2
for i, ap in enumerate(sorted(all_ap_names)):
    ap_colors_map[ap] = palette[i % len(palette)]

# ========== Chart 1: Connected AP RSSI Along Path ==========
st.subheader("Connected AP RSSI Along Path")

fig1 = go.Figure()

for ap in sorted(all_ap_names):
    rssi_vals = [s.ap_best_rssi.get(ap) for s in data.snapshots]
    rssi_clean = [v if v and v > -999 else None for v in rssi_vals]
    fig1.add_trace(go.Scatter(
        x=x_labels,
        y=rssi_clean,
        mode='lines+markers',
        name=ap,
        line=dict(color=ap_colors_map[ap], width=1.5, dash='dot'),
        marker=dict(size=5),
        opacity=0.5,
        showlegend=True,
    ))

for i, s in enumerate(data.snapshots):
    connected_rssi = s.ap_best_rssi.get(s.connected_ap)
    if connected_rssi and connected_rssi > -999:
        color = ap_colors_map.get(s.connected_ap, 'gray')
        marker_symbol = 'x' if s.roaming_issue else 'circle'
        fig1.add_trace(go.Scatter(
            x=[x_labels[i]],
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

fig1.add_hline(y=-65, line_dash="dash", line_color="green", opacity=0.4, annotation_text="-65 dBm")
fig1.add_hline(y=-75, line_dash="dash", line_color="red", opacity=0.4, annotation_text="-75 dBm")
_add_roaming_markers(fig1, x_labels)

fig1.update_layout(
    height=370,
    yaxis_title="RSSI (dBm)",
    xaxis_title="Measurement Point",
    yaxis=dict(range=[-95, -25]),
    legend=dict(orientation='h', yanchor='bottom', y=1.02),
    margin=dict(l=50, r=20, t=40, b=90),
)
st.plotly_chart(fig1, use_container_width=True)

# ========== Chart 2: AP RSSI Comparison per Point ==========
st.subheader("AP RSSI Comparison per Point")

fig2 = go.Figure()
for ap in sorted(all_ap_names):
    rssi_vals = [s.ap_best_rssi.get(ap) for s in data.snapshots]
    rssi_clean = [v if v and v > -999 else None for v in rssi_vals]
    fig2.add_trace(go.Bar(
        name=ap,
        x=x_labels,
        y=rssi_clean,
        marker_color=ap_colors_map[ap],
    ))

_add_roaming_markers(fig2, x_labels)

fig2.update_layout(
    height=340,
    barmode='group',
    yaxis_title="Best RSSI (dBm)",
    yaxis=dict(range=[-95, -25]),
    legend=dict(orientation='h', yanchor='bottom', y=1.02),
    margin=dict(l=50, r=20, t=40, b=90),
)
st.plotly_chart(fig2, use_container_width=True)

# ========== Chart 3: Band RSSI per Point ==========
st.subheader("Band RSSI per Point (AP-aggregated best)")

all_bands = set()
for s in data.snapshots:
    all_bands.update(s.band_best_rssi.keys())
band_order = [b for b in ['2.4 GHz', '5 GHz', '6 GHz'] if b in all_bands]
band_colors = {'2.4 GHz': '#3266ad', '5 GHz': '#16a085', '6 GHz': '#8e44ad'}

if band_order:
    fig3 = go.Figure()
    for band in band_order:
        vals = [s.band_best_rssi.get(band) for s in data.snapshots]
        fig3.add_trace(go.Scatter(
            x=x_labels,
            y=vals,
            mode='lines+markers',
            name=band,
            line=dict(color=band_colors.get(band, 'gray'), width=2),
            marker=dict(size=7),
            connectgaps=False,
        ))
    fig3.add_hline(y=-65, line_dash="dash", line_color="green", opacity=0.3)
    fig3.add_hline(y=-75, line_dash="dash", line_color="red", opacity=0.3)
    _add_roaming_markers(fig3, x_labels)
    fig3.update_layout(
        height=320,
        yaxis_title="Best RSSI (dBm)",
        yaxis=dict(range=[-95, -25]),
        legend=dict(orientation='h', yanchor='bottom', y=1.02),
        margin=dict(l=50, r=20, t=40, b=90),
    )
    st.plotly_chart(fig3, use_container_width=True)

# ========== Chart 4: Coverage Quality per Point ==========
st.subheader("Coverage Quality per Point (AP-unit basis)")

fig4 = go.Figure()
fig4.add_trace(go.Bar(
    name='Good (≥ -65 dBm)',
    x=x_labels,
    y=[s.good_aps for s in data.snapshots],
    marker_color='#1D9E75',
))
fig4.add_trace(go.Bar(
    name='Fair (-65 ~ -75 dBm)',
    x=x_labels,
    y=[s.fair_aps for s in data.snapshots],
    marker_color='#EF9F27',
))
fig4.add_trace(go.Bar(
    name='Weak (< -75 dBm)',
    x=x_labels,
    y=[s.weak_aps for s in data.snapshots],
    marker_color='#D85A30',
))
_add_roaming_markers(fig4, x_labels)
fig4.update_layout(
    height=300,
    barmode='stack',
    yaxis_title="Number of APs",
    yaxis=dict(dtick=1),
    legend=dict(orientation='h', yanchor='bottom', y=1.02),
    margin=dict(l=50, r=20, t=40, b=90),
)
st.plotly_chart(fig4, use_container_width=True)

# ========== Chart 5: Co-Channel Interference per Point ==========
st.subheader("Co-Channel Interference per Point")

co_channel_data = compute_co_channel_interference(data.snapshots)

x_labels_coch = [
    f"{s.name[-8:]}<br>[{s.connected_ap or '?'}]<br>Ch{d['channel'] or '?'}"
    for s, d in zip(data.snapshots, co_channel_data)
]

managed_vals = [d['managed'] for d in co_channel_data]
unmanaged_vals = [d['unmanaged'] for d in co_channel_data]
totals = [m + u for m, u in zip(managed_vals, unmanaged_vals)]

fig5 = go.Figure()
fig5.add_trace(go.Bar(
    name='Managed AP (same channel)',
    x=x_labels_coch,
    y=managed_vals,
    marker_color='#4CAF50',
))
fig5.add_trace(go.Bar(
    name='Unmanaged AP (same channel)',
    x=x_labels_coch,
    y=unmanaged_vals,
    marker_color='#F44336',
))

for xl, total in zip(x_labels_coch, totals):
    if total > 0:
        fig5.add_annotation(
            x=xl, y=total,
            text=str(total),
            showarrow=False,
            yshift=8,
            font=dict(size=11),
        )

_add_roaming_markers(fig5, x_labels_coch)

fig5.update_layout(
    height=340,
    barmode='stack',
    yaxis_title="Number of APs",
    yaxis=dict(dtick=1),
    legend=dict(orientation='h', yanchor='bottom', y=1.02),
    margin=dict(l=50, r=20, t=40, b=110),
)
st.plotly_chart(fig5, use_container_width=True)

# ========== Chart 6: Unmanaged AP Interference per Point ==========
st.subheader("Unmanaged AP Interference per Point")

unmanaged_data = compute_unmanaged_interference(data.snapshots)

named_vals = [d['named'] for d in unmanaged_data]
hidden_vals = [d['hidden'] for d in unmanaged_data]
max_rssi_vals = [d['max_rssi'] for d in unmanaged_data]

fig6 = go.Figure()
fig6.add_trace(go.Bar(
    name='Named Unmanaged',
    x=x_labels,
    y=named_vals,
    marker_color='#E07B54',
    yaxis='y',
))
fig6.add_trace(go.Bar(
    name='Hidden Unmanaged',
    x=x_labels,
    y=hidden_vals,
    marker_color='#999999',
    yaxis='y',
))
fig6.add_trace(go.Scatter(
    name='Max RSSI (unmanaged)',
    x=x_labels,
    y=max_rssi_vals,
    mode='lines+markers',
    line=dict(color='#CC0000', width=2),
    marker=dict(size=7),
    yaxis='y2',
    connectgaps=False,
))

_add_roaming_markers(fig6, x_labels)

fig6.update_layout(
    height=340,
    barmode='stack',
    yaxis=dict(title="Number of APs", dtick=1),
    yaxis2=dict(
        title="Max RSSI (dBm)",
        overlaying='y',
        side='right',
        range=[-95, -25],
    ),
    legend=dict(orientation='h', yanchor='bottom', y=1.02),
    margin=dict(l=50, r=70, t=40, b=90),
)
st.plotly_chart(fig6, use_container_width=True)

# ========== Chart 7: Optimal AP Selection Check ==========
st.subheader("Optimal AP Selection Check")

optimal_data = compute_optimal_ap_check(data.snapshots)

conn_rssi_vals = [d['connected_rssi'] for d in optimal_data]
best_rssi_vals = [d['best_rssi'] for d in optimal_data]

fig7 = go.Figure()
fig7.add_trace(go.Scatter(
    name='Connected AP',
    x=x_labels,
    y=conn_rssi_vals,
    mode='lines+markers',
    line=dict(color='#2196F3', width=2),
    marker=dict(size=8),
))
fig7.add_trace(go.Scatter(
    name='Best Available AP',
    x=x_labels,
    y=best_rssi_vals,
    mode='lines+markers',
    line=dict(color='#4CAF50', width=2, dash='dash'),
    marker=dict(size=8),
    connectgaps=False,
))

# Suboptimal zone ハイライト（categorical x: 隣接ラベル間を塗る）
suboptimal_exists = any(not d['is_optimal'] for d in optimal_data)
for i, d in enumerate(optimal_data):
    if not d['is_optimal'] and len(x_labels) > 1:
        x0 = x_labels[i]
        x1 = x_labels[i + 1] if i + 1 < len(x_labels) else x_labels[i - 1]
        if x0 != x1:
            fig7.add_vrect(
                x0=x0, x1=x1,
                fillcolor='rgba(255,0,0,0.15)',
                line_width=0,
                layer='below',
            )

if suboptimal_exists:
    fig7.add_trace(go.Scatter(
        x=[None], y=[None],
        mode='markers',
        name='Suboptimal zone',
        marker=dict(color='rgba(255,0,0,0.3)', symbol='square', size=12),
        showlegend=True,
    ))

# アノテーション
for i, d in enumerate(optimal_data):
    y_val = d['connected_rssi']
    if y_val is None:
        continue
    if d['is_optimal']:
        fig7.add_annotation(
            x=x_labels[i], y=y_val,
            text="✅",
            showarrow=False,
            yshift=16,
            font=dict(size=12, color='#4CAF50'),
        )
    else:
        label = f"⚠️ {d['best_ap_name']}" if d['best_ap_name'] else "⚠️"
        fig7.add_annotation(
            x=x_labels[i], y=y_val,
            text=label,
            showarrow=False,
            yshift=16,
            font=dict(size=10, color='#F44336'),
        )

fig7.add_hline(y=-65, line_dash="dash", line_color="green", opacity=0.4, annotation_text="-65 dBm")
fig7.add_hline(y=-75, line_dash="dash", line_color="red", opacity=0.4, annotation_text="-75 dBm")
_add_roaming_markers(fig7, x_labels)

fig7.update_layout(
    height=400,
    yaxis_title="RSSI (dBm)",
    yaxis=dict(range=[-95, -25]),
    legend=dict(orientation='h', yanchor='bottom', y=1.02),
    margin=dict(l=50, r=20, t=40, b=90),
)
st.plotly_chart(fig7, use_container_width=True)

# ========== 詳細テーブル ==========
with st.expander("Detailed Data Table"):
    table_rows = []
    for s in data.snapshots:
        timestamp = s.timestamp or ''
        time_display = timestamp.replace('T', ' ') if 'T' in timestamp else timestamp
        row = {
            'Time': time_display,
            'Snapshot Name': s.name,
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

# ========== Surrounding AP RSSI Matrix ==========
st.subheader("Surrounding AP RSSI Matrix")

# 列(測定点)情報を作成
columns = []
connected_aps_set = set()
for s in data.snapshots:
    conn_rssi = s.ap_best_rssi.get(s.connected_ap)
    conn_rssi = conn_rssi if (conn_rssi is not None and conn_rssi > -999) else None
    conn_str = f"{int(conn_rssi)} dBm" if conn_rssi is not None else "-"
    # 列ヘッダー用 (3行: AP名 / RSSI / Snapshot名)
    label = f"{s.connected_ap or '(none)'}\n({conn_str})\n{s.name}"
    # selectbox表示用 (1行)
    display_label = f"{s.connected_ap or '(none)'} ({conn_str}) - {s.name}"
    columns.append({
        'label': label,
        'display_label': display_label,
        'snapshot': s,
        'connected_ap': s.connected_ap,
    })
    if s.connected_ap:
        connected_aps_set.add(s.connected_ap)

# 周辺AP一覧 (Connected AP自身は除外)
surrounding_aps = set()
for s in data.snapshots:
    for ap, rssi in s.ap_best_rssi.items():
        if ap in connected_aps_set:
            continue
        if rssi is not None and rssi > -999:
            surrounding_aps.add(ap)

if not columns or not surrounding_aps:
    st.info("Not enough data for surrounding AP matrix.")
else:
    # マトリクス構築: 行=周辺AP, 列=測定点ラベル
    matrix = {}
    for ap in surrounding_aps:
        row = {}
        for col in columns:
            rssi = col['snapshot'].ap_best_rssi.get(ap)
            row[col['label']] = rssi if (rssi is not None and rssi > -999) else None
        matrix[ap] = row

    col_labels = [c['label'] for c in columns]
    df_matrix = pd.DataFrame(matrix).T
    df_matrix = df_matrix.reindex(columns=col_labels)

    # selectbox表示用ラベル → 列ラベル(改行付き) のマッピング
    display_labels = [c['display_label'] for c in columns]
    display_to_col = {c['display_label']: c['label'] for c in columns}

    # ソートUI
    c1, c2 = st.columns(2)
    with c1:
        sort_display = st.selectbox("Sort by column", display_labels, key="surr_sort_col")
    with c2:
        order = st.radio("Order", ["Strongest first", "Weakest first"],
                         horizontal=True, key="surr_sort_order")
    ascending = (order == "Weakest first")
    sort_col = display_to_col[sort_display]

    # 選んだ列でソート。値なし(None)は常に最下部。
    key = df_matrix[sort_col]
    has_value = key.notna()
    df_top = df_matrix[has_value].copy()
    df_bottom = df_matrix[~has_value].copy()
    df_top = df_top.sort_values(sort_col, ascending=ascending)
    df_matrix = pd.concat([df_top, df_bottom])

    # 表示用フォーマット
    def _fmt(v):
        return "-" if pd.isna(v) else f"{int(v)} dBm"
    df_display = df_matrix.map(_fmt)
    df_display.index.name = "Surrounding AP"

    # 色分け
    def color_rssi(val):
        if val == "-":
            return ""
        try:
            v = int(str(val).replace(" dBm", ""))
        except Exception:
            return ""
        if v >= -65:
            return "background-color: #E1F5EE"
        if v >= -75:
            return "background-color: #FAEEDA"
        return "background-color: #FAECE7"

    styled = df_display.style.map(color_rssi)
    st.dataframe(styled, use_container_width=True)
    st.caption(
        "Rows = surrounding APs · Columns = measurement points. "
        "Sorted by the selected column. Rows with no value in that column are at the bottom. "
        "🟢 ≥ -65 dBm · 🟡 -65~-75 dBm · 🔴 < -75 dBm"
    )

# ========== Map & AP Info ==========
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

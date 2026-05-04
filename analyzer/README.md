# Wi-Fi Survey Analyzer

Streamlit-based analysis tool for Wi-Fi survey data exported from the
Wi-Fi Analyzer Android app.

## Quick Start

```bash
cd analyzer
docker-compose up
```

Open http://localhost:8501 in your browser.

## Usage

1. Export a survey ZIP from the Android app (Map → Export button)
2. Upload the ZIP to the web interface
3. View analysis results

## Development

```bash
cd analyzer
pip install -r requirements.txt
streamlit run app/app.py
```

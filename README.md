# iFinbot Mobile

An Android app for the iFinbot project, with a Python API for risk profiling and portfolio generation.

## Architecture

The Android app captures client/survey inputs and sends them to a FastAPI backend. The backend loads the existing iFinbot ML and portfolio pipeline, predicts RT, interprets the risk profile, and returns a portfolio.

This keeps pandas, sklearn, scipy, and the portfolio optimizer in Python instead of trying to bundle the full ML stack inside an APK.

## Backend

From this repo:

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
export IFINBOT_CORE_PATH=/home/xryunix/code/Xryrapier/ifinbot
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Health check:

```bash
curl http://127.0.0.1:8000/health
```

Portfolio endpoint:

```bash
curl -X POST http://127.0.0.1:8000/api/portfolio \
  -H 'Content-Type: application/json' \
  -d '{
    "HHSEX": 1,
    "AGE": 35,
    "EDCL": 4,
    "MARRIED": 1,
    "KIDS": 2,
    "FAMSTRUCT": 4,
    "OCCAT1": 1,
    "INCOME": 85000,
    "WSAVED": 3,
    "YESFINRISK": 1,
    "NETWORTH": 250000,
    "investment": 100000,
    "ndays": 180
  }'
```

## Android APK

Open `android/` in Android Studio.

1. Start the backend first.
2. In an Android emulator, keep the API URL as `http://10.0.2.2:8000`.
3. On a physical phone, use your computer LAN IP, for example `http://192.168.1.20:8000`.
4. Build APK from Android Studio: `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

The project does not include a Gradle wrapper yet. Android Studio can create/use its local Gradle setup when opening the project.

## SCF Codes Used

- `HHSEX`: male `1`, female `2`.
- `MARRIED`: yes `1`, no `2`.
- `YESFINRISK`: yes `1`, no `0`.

Other categorical fields follow the SCF coding already used by the iFinbot model.

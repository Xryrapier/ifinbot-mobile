import os
import sys
from pathlib import Path
from typing import Any

import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field


CORE_PATH = Path(os.environ.get("IFINBOT_CORE_PATH", Path(__file__).resolve().parents[3] / "ifinbot")).resolve()
if not CORE_PATH.exists():
    raise RuntimeError(f"IFINBOT_CORE_PATH does not exist: {CORE_PATH}")

sys.path.insert(0, str(CORE_PATH))
sys.path.insert(0, str(CORE_PATH / "math_model"))

from IDprogram.id_cliente import process_and_predict
from math_model.math_model_ii import get_actions_opt_portfolio


class PortfolioRequest(BaseModel):
    HHSEX: int = Field(..., description="SCF code: male=1, female=2")
    AGE: int
    EDCL: int
    MARRIED: int = Field(..., description="SCF code: yes=1, no=2")
    KIDS: int
    FAMSTRUCT: int
    OCCAT1: int
    INCOME: float
    WSAVED: int
    YESFINRISK: int = Field(..., description="yes=1, no=0")
    NETWORTH: float
    investment: float = 100_000
    ndays: int = 180


app = FastAPI(title="iFinbot API", version="0.1.0")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "core_path": str(CORE_PATH)}


@app.post("/api/portfolio")
def create_portfolio(payload: PortfolioRequest) -> dict[str, Any]:
    model_fields = [
        "HHSEX", "AGE", "EDCL", "MARRIED", "KIDS", "FAMSTRUCT", "OCCAT1",
        "INCOME", "WSAVED", "YESFINRISK", "NETWORTH",
    ]
    row = {field: getattr(payload, field) for field in model_fields}
    row.update({"YY1": "mobile", "Y1": "client"})
    client_df = pd.DataFrame([row])

    try:
        client_profile = process_and_predict([client_df]).iloc[0]
        rt_score = float(client_profile["TR(tolerancia al riesgo)"])
        min_risk_portfolio, rt_portfolio = get_actions_opt_portfolio(
            ndays=payload.ndays,
            invest=payload.investment,
            rt_score=rt_score,
        )
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc

    return {
        "client": {
            "id": client_profile.get("ID"),
            "rt_score": rt_score,
            "risk_profile": client_profile.get("Perfil de riesgo"),
            "rt_meaning": client_profile.get("Significado RT"),
            "target_annual_volatility": float(client_profile.get("Volatilidad objetivo anual")),
        },
        "portfolio": rt_portfolio.to_dict(orient="records"),
        "minimum_risk_portfolio": min_risk_portfolio.to_dict(orient="records"),
    }

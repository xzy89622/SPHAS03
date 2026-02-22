# -*- coding: utf-8 -*-
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI()

class Features(BaseModel):
    userId: int
    riskScore: int

@app.post("/predict")
def predict(f: Features):
    predicted = min(100, f.riskScore + 5)
    return {
        "predictedRiskScore": predicted,
        "suggestion": "（Python模型）建议控制饮食，增加运动，连续监测7天"
    }
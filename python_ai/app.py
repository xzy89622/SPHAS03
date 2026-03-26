from flask import Flask, request, jsonify
import numpy as np
from sklearn.ensemble import RandomForestRegressor

app = Flask(__name__)

MODEL_NAME = "PYTHON_RF_V1"


def safe_float(value):
    try:
        if value is None or value == "":
            return np.nan
        return float(value)
    except Exception:
        return np.nan


def fill_nan_with_col_mean(x):
    if x.size == 0:
        return x

    # 这里把全列都是 nan 的情况也兜一下，避免均值还是 nan
    col_mean = np.nanmean(x, axis=0)
    col_mean = np.where(np.isnan(col_mean), 0.0, col_mean)

    inds = np.where(np.isnan(x))
    if len(inds[0]) > 0:
        x[inds] = np.take(col_mean, inds[1])

    return x


def normalize_history(history):
    """
    这里统一把前端/后端传来的历史记录洗干净
    只保留和预测有关的字段
    """
    rows = []
    if not isinstance(history, list):
        return rows

    for item in history:
        if not isinstance(item, dict):
            continue

        row = {
            "weightKg": safe_float(item.get("weightKg")),
            "bmi": safe_float(item.get("bmi")),
            "steps": safe_float(item.get("steps")),
            "sleepHours": safe_float(item.get("sleepHours")),
            "systolic": safe_float(item.get("systolic")),
            "diastolic": safe_float(item.get("diastolic")),
            "bloodSugar": safe_float(item.get("bloodSugar"))
        }
        rows.append(row)

    return rows


def build_dataset(history):
    """
    把历史记录转成训练集
    X = [dayIndex, bmi, steps, sleepHours, systolic, diastolic, bloodSugar]
    y = weightKg
    """
    x = []
    y = []

    for i, row in enumerate(history):
        weight = row.get("weightKg")
        if np.isnan(weight):
            continue

        features = [
            float(i),
            row.get("bmi"),
            row.get("steps"),
            row.get("sleepHours"),
            row.get("systolic"),
            row.get("diastolic"),
            row.get("bloodSugar")
        ]
        x.append(features)
        y.append(weight)

    if len(y) < 5:
        return None, None

    x = np.array(x, dtype=float)
    y = np.array(y, dtype=float)
    x = fill_nan_with_col_mean(x)
    return x, y


def latest_weight(history):
    for row in reversed(history):
        weight = row.get("weightKg")
        if not np.isnan(weight):
            return float(weight)
    return None


def build_trend(predicted_weight, latest_weight_value):
    if predicted_weight is None or latest_weight_value is None:
        return "STABLE"

    diff = predicted_weight - latest_weight_value
    if diff > 0.3:
        return "UP"
    if diff < -0.3:
        return "DOWN"
    return "STABLE"


def build_predicted_risk_score(current_score, trend):
    score = int(current_score or 0)

    if trend == "UP":
        score += 2
    elif trend == "DOWN":
        score -= 1
    else:
        score += 1

    if score < 0:
        score = 0
    if score > 100:
        score = 100
    return score


def build_level(score):
    if score >= 60:
        return "HIGH"
    if score >= 30:
        return "MID"
    return "LOW"


def build_suggestion(trend, level):
    if trend == "UP":
        return "预测体重有上升趋势，建议减少高糖高油饮食，并增加有氧和力量训练"
    if trend == "DOWN":
        if level == "HIGH":
            return "虽然体重预测有下降趋势，但整体风险仍偏高，建议继续监测核心指标"
        return "预测体重有下降趋势，建议继续保持当前节奏，并注意营养均衡"
    if level == "HIGH":
        return "整体趋势暂时稳定，但风险仍偏高，建议先优先处理血压、血糖或睡眠异常"
    return "预测整体较稳定，建议继续规律记录体质数据，并保持健康生活习惯"


def build_basis(history):
    basis = []

    basis.append("历史样本数：" + str(len(history)))

    lw = latest_weight(history)
    if lw is not None:
        basis.append("最新体重：" + str(round(lw, 2)) + "kg")

    if len(history) >= 2:
        recent_steps = []
        recent_sleep = []
        for row in history[-7:]:
            if not np.isnan(row.get("steps")):
                recent_steps.append(row.get("steps"))
            if not np.isnan(row.get("sleepHours")):
                recent_sleep.append(row.get("sleepHours"))

        if recent_steps:
            basis.append("近7次平均步数：" + str(int(round(sum(recent_steps) / len(recent_steps)))) + "步")
        if recent_sleep:
            basis.append("近7次平均睡眠：" + str(round(sum(recent_sleep) / len(recent_sleep), 2)) + "小时")

    return basis


def fallback_predict(history, risk_score, horizon_days):
    latest = latest_weight(history)

    if latest is None:
        predicted_weight = None
    else:
        predicted_weight = latest

    trend = "STABLE"
    predicted_risk_score = min(100, int(risk_score or 0) + 3)
    level = build_level(predicted_risk_score)

    return {
        "model": "FALLBACK",
        "modelName": MODEL_NAME,
        "predictedWeightKg": predicted_weight,
        "predictedRiskScore": predicted_risk_score,
        "predictedLevel": level,
        "trend": trend,
        "confidence": 0.45,
        "sampleCount": len(history),
        "horizonDays": horizon_days,
        "basis": build_basis(history),
        "suggestion": "历史数据不足，当前先返回保守预测，建议继续录入体质数据后再生成更稳定结果"
    }


@app.get("/health")
def health():
    return jsonify({
        "ok": True,
        "service": "python-ai",
        "modelName": MODEL_NAME
    })


@app.post("/predict")
def predict():
    payload = request.get_json(force=True) or {}

    raw_history = payload.get("history", [])
    history = normalize_history(raw_history)

    try:
        horizon_days = int(payload.get("horizonDays", 7))
    except Exception:
        horizon_days = 7

    try:
        risk_score = int(payload.get("riskScore", 0))
    except Exception:
        risk_score = 0

    x, y = build_dataset(history)

    if x is None or y is None:
        return jsonify(fallback_predict(history, risk_score, horizon_days))

    model = RandomForestRegressor(
        n_estimators=200,
        random_state=42
    )
    model.fit(x, y)

    # 这里取最后一条记录作为未来预测基准
    future = x[-1].copy()
    future[0] = float(x[-1][0] + horizon_days)
    future = future.reshape(1, -1)

    predicted_weight = float(model.predict(future)[0])
    latest_weight_value = float(y[-1])

    trend = build_trend(predicted_weight, latest_weight_value)
    predicted_risk_score = build_predicted_risk_score(risk_score, trend)
    level = build_level(predicted_risk_score)

    response = {
        "model": "PYTHON",
        "modelName": MODEL_NAME,
        "predictedWeightKg": round(predicted_weight, 2),
        "predictedRiskScore": predicted_risk_score,
        "predictedLevel": level,
        "trend": trend,
        "confidence": 0.82 if len(y) >= 10 else (0.76 if len(y) >= 7 else 0.70),
        "sampleCount": int(len(y)),
        "horizonDays": horizon_days,
        "basis": build_basis(history),
        "suggestion": build_suggestion(trend, level)
    }

    return jsonify(response)


if __name__ == "__main__":
    # 这里默认还是 5001，和 Java 后端配置保持一致
    app.run(host="0.0.0.0", port=5001, debug=True)
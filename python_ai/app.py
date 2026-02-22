from flask import Flask, request, jsonify
import numpy as np
from sklearn.ensemble import RandomForestRegressor

app = Flask(__name__)

def _safe_float(x):
    try:
        if x is None or x == "":
            return np.nan
        return float(x)
    except Exception:
        return np.nan

def _build_dataset(history):
    """
    ✅ 把历史手动录入数据转成训练数据
    X: [dayIndex, bmi, steps, sleepHours, systolic, diastolic, bloodSugar]
    y: weightKg
    """
    X = []
    y = []
    if not history:
        return None, None

    for i, r in enumerate(history):
        w = _safe_float(r.get("weightKg"))
        if np.isnan(w):
            continue

        row = [
            float(i),  # dayIndex
            _safe_float(r.get("bmi")),
            _safe_float(r.get("steps")),
            _safe_float(r.get("sleepHours")),
            _safe_float(r.get("systolic")),
            _safe_float(r.get("diastolic")),
            _safe_float(r.get("bloodSugar")),
        ]
        X.append(row)
        y.append(w)

    if len(y) < 5:
        # ✅ 数据太少，不训练
        return None, None

    X = np.array(X, dtype=float)
    y = np.array(y, dtype=float)

    # ✅ 简单处理缺失值：用列均值填充
    col_mean = np.nanmean(X, axis=0)
    inds = np.where(np.isnan(X))
    X[inds] = np.take(col_mean, inds[1])

    return X, y

@app.route("/predict", methods=["POST"])
def predict():
    payload = request.get_json(force=True) or {}

    history = payload.get("history", [])
    horizon_days = int(payload.get("horizonDays", 7))
    risk_score = int(payload.get("riskScore", 0))

    X, y = _build_dataset(history)
    if X is None:
        # ✅ 降级：不做模型，返回一个保守结果
        latest_w = None
        if history:
            latest_w = history[-1].get("weightKg")
        return jsonify({
            "predictedWeightKg": latest_w,
            "predictedRiskScore": min(100, risk_score + 3),
            "trend": "STABLE",
            "suggestion": "（Python降级）历史数据不足，建议继续录入体质数据以获得更准预测"
        })

    # ✅ 训练随机森林回归（预测体重）
    model = RandomForestRegressor(
        n_estimators=200,
        random_state=42
    )
    model.fit(X, y)

    # ✅ 构造未来一天的特征：dayIndex = lastIndex + horizon_days，其它用最后一天数据（再做缺失填充）
    last = X[-1].copy()
    future = last.copy()
    future[0] = float(X[-1][0] + horizon_days)  # dayIndex
    future = future.reshape(1, -1)

    pred_w = float(model.predict(future)[0])
    latest_w = float(y[-1])

    # ✅ 趋势
    if pred_w - latest_w > 0.3:
        trend = "UP"
    elif latest_w - pred_w > 0.3:
        trend = "DOWN"
    else:
        trend = "STABLE"

    # ✅ 风险分预测（简单示例：体重上升 +2，下降 -1，稳定 +1）
    pr = risk_score + (2 if trend == "UP" else (-1 if trend == "DOWN" else 1))
    pr = max(0, min(100, int(pr)))

    suggestion = "建议保持规律作息、控制饮食并持续记录体质数据"
    if trend == "UP":
        suggestion = "预测体重有上升趋势，建议减少高糖高油，增加有氧+力量训练"
    elif trend == "DOWN":
        suggestion = "预测体重有下降趋势，建议注意营养均衡，避免过度节食"

    return jsonify({
        "predictedWeightKg": round(pred_w, 2),
        "predictedRiskScore": pr,
        "trend": trend,
        "suggestion": suggestion
    })

if __name__ == "__main__":
    # ✅ 默认5001，和SpringBoot配置一致
    app.run(host="0.0.0.0", port=5001, debug=True)
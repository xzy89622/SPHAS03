# 这个文件现在只保留成兼容入口
# 以前这里有另一套 FastAPI 预测逻辑，容易和 app.py 打架
# 现在统一都走 app.py 里的 Flask 服务
# 以后不管你启动哪个文件，最终都是同一套 AI 逻辑

from app import app

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001, debug=True)
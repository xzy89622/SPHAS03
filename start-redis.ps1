# 启动 Docker Desktop（如果没启动）
$dockerInfo = docker info 2>$null
if (-not $?) {
  Write-Host "Docker 引擎未启动，正在启动 Docker Desktop..."
  Start-Process "$Env:ProgramFiles\Docker\Docker\Docker Desktop.exe"
  Start-Sleep -Seconds 10
}

# 确保 redis-project03 存在并启动
$exists = docker ps -a --format "{{.Names}}" | Select-String -Pattern "^redis-project03$"
if (-not $exists) {
  Write-Host "创建 Redis 容器..."
  docker run -d --restart=always --name redis-project03 -p 6379:6379 redis:7 | Out-Null
} else {
  Write-Host "启动 Redis 容器..."
  docker start redis-project03 | Out-Null
  docker update --restart=always redis-project03 | Out-Null
}

# 检查 PONG
Write-Host "检查 Redis..."
docker exec -it redis-project03 redis-cli ping
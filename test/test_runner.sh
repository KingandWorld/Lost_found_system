#!/bin/bash
# ================================================================
# 失物招领系统 API 测试脚本（curl 版本）
# ================================================================
# 使用说明：
#   1. 确保后端已启动：http://localhost:1235
#   2. 运行：bash test_runner.sh
#   3. 或直接：chmod +x test_runner.sh && ./test_runner.sh
# ================================================================

BASE_URL="${1:-http://localhost:1235}"
API="${BASE_URL}/api"

PASS=0
FAIL=0

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=============================================="
echo "  失物招领系统 API 快速测试"
echo "  目标: ${BASE_URL}"
echo "=============================================="

check() {
    local desc="$1"
    local code="$2"
    local expect="$3"
    if [ "$code" == "$expect" ]; then
        echo -e "  ${GREEN}✅ ${desc}${NC}"
        PASS=$((PASS + 1))
    else
        echo -e "  ${RED}❌ ${desc} (code=${code}, 期望=${expect})${NC}"
        FAIL=$((FAIL + 1))
    fi
}

extract_code() {
    echo "$1" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',''))" 2>/dev/null
}

extract_token() {
    echo "$1" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('token',''))" 2>/dev/null
}

# ---- 1. 用户登录 ----
echo ""
echo "--- 1. 用户管理 ---"

echo -n "  登录管理员..."
ADMIN_RESP=$(curl -s -X POST "${API}/user/login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin","password":"123456"}')
ADMIN_CODE=$(extract_code "$ADMIN_RESP")
ADMIN_TOKEN=$(extract_token "$ADMIN_RESP")
check "管理员登录" "$ADMIN_CODE" "200"
[ -n "$ADMIN_TOKEN" ] && echo -e "    ${GREEN}✅ 获取到 Token${NC}" || echo -e "    ${RED}❌ 未获取到 Token${NC}"

echo -n "  登录普通用户..."
USER_RESP=$(curl -s -X POST "${API}/user/login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"user1","password":"123456"}')
USER_CODE=$(extract_code "$USER_RESP")
USER_TOKEN=$(extract_token "$USER_RESP")
check "普通用户登录" "$USER_CODE" "200"

echo -n "  错误密码登录..."
ERR_RESP=$(curl -s -X POST "${API}/user/login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin","password":"wrong_pass"}')
check "错误密码拒绝" "$(extract_code "$ERR_RESP")" "-1"

echo -n "  获取当前用户..."
CUR_RESP=$(curl -s "${API}/user/current" -H "token: ${ADMIN_TOKEN}")
check "获取当前用户" "$(extract_code "$CUR_RESP")" "200"

echo -n "  分页查询用户..."
PAGE_RESP=$(curl -s "${API}/user/page?currentPage=1&size=10" -H "token: ${ADMIN_TOKEN}")
check "分页查询" "$(extract_code "$PAGE_RESP")" "200"

# ---- 2. 分类管理 ----
echo ""
echo "--- 2. 物品分类管理 ---"

echo -n "  获取分类列表..."
CAT_RESP=$(curl -s "${API}/category/list")
check "获取分类列表" "$(extract_code "$CAT_RESP")" "200"

echo -n "  分页查询分类..."
CAT_PAGE=$(curl -s "${API}/category/page?currentPage=1&size=10")
check "分页查询分类" "$(extract_code "$CAT_PAGE")" "200"

echo -n "  添加分类..."
CAT_ADD=$(curl -s -X POST "${API}/category" \
    -H "Content-Type: application/json" \
    -H "token: ${ADMIN_TOKEN}" \
    -d '{"name":"curl测试分类","sort":99}')
check "添加分类" "$(extract_code "$CAT_ADD")" "200"

# ---- 3. 失物信息 ----
echo ""
echo "--- 3. 失物信息管理 ---"

echo -n "  分页查询失物..."
LOST_PAGE=$(curl -s "${API}/lost-item/page?currentPage=1&size=10")
check "查询失物列表" "$(extract_code "$LOST_PAGE")" "200"

echo -n "  查询失物详情..."
LOST_DETAIL=$(curl -s "${API}/lost-item/1")
check "查询失物详情" "$(extract_code "$LOST_DETAIL")" "200"

echo -n "  获取统计数据..."
LOST_STAT=$(curl -s "${API}/lost-item/statistics")
check "获取统计" "$(extract_code "$LOST_STAT")" "200"

echo -n "  发布失物信息..."
LOST_ADD=$(curl -s -X POST "${API}/lost-item" \
    -H "Content-Type: application/json" \
    -H "token: ${ADMIN_TOKEN}" \
    -d '{
        "title":"curl测试-丢失物品",
        "description":"这是一个通过curl测试发布的失物信息，描述足够详细。",
        "categoryId":1,
        "lostPlace":"测试地点",
        "lostTime":"2026-06-28T12:00:00",
        "contactName":"测试",
        "contactPhone":"13800138001"
    }')
check "发布失物" "$(extract_code "$LOST_ADD")" "200"

# ---- 4. 招领信息 ----
echo ""
echo "--- 4. 招领信息管理 ---"

echo -n "  分页查询招领..."
FOUND_PAGE=$(curl -s "${API}/found-item/page?currentPage=1&size=10")
check "查询招领列表" "$(extract_code "$FOUND_PAGE")" "200"

echo -n "  查询招领详情..."
FOUND_DETAIL=$(curl -s "${API}/found-item/1")
check "查询招领详情" "$(extract_code "$FOUND_DETAIL")" "200"

echo -n "  发布招领信息..."
FOUND_ADD=$(curl -s -X POST "${API}/found-item" \
    -H "Content-Type: application/json" \
    -H "token: ${USER_TOKEN}" \
    -d '{
        "title":"curl测试-捡到物品",
        "description":"这是通过curl测试发布的招领信息，描述足够详细用于测试。",
        "categoryId":4,
        "foundPlace":"测试地点",
        "foundTime":"2026-06-28T12:00:00",
        "contactName":"测试员",
        "contactPhone":"13900139001"
    }')
check "发布招领" "$(extract_code "$FOUND_ADD")" "200"

# ---- 5. 认领申请 ----
echo ""
echo "--- 5. 认领申请管理 ---"

echo -n "  我的申请列表..."
MY_CLAIM=$(curl -s "${API}/claim/my?currentPage=1&size=10" -H "token: ${USER_TOKEN}")
check "我的申请" "$(extract_code "$MY_CLAIM")" "200"

echo -n "  管理员查询所有申请..."
ALL_CLAIM=$(curl -s "${API}/claim/page?currentPage=1&size=10" -H "token: ${ADMIN_TOKEN}")
check "管理员查申请" "$(extract_code "$ALL_CLAIM")" "200"

# ---- 6. 通知管理 ----
echo ""
echo "--- 6. 通知管理 ---"

echo -n "  获取通知列表..."
NOTI_LIST=$(curl -s "${API}/notification/list?currentPage=1&size=10" -H "token: ${USER_TOKEN}")
check "通知列表" "$(extract_code "$NOTI_LIST")" "200"

echo -n "  未读通知数..."
NOTI_UNREAD=$(curl -s "${API}/notification/unread-count" -H "token: ${USER_TOKEN}")
check "未读通知数" "$(extract_code "$NOTI_UNREAD")" "200"

# ---- 7. 异常测试 ----
echo ""
echo "--- 7. 异常场景 ---"

echo -n "  无Token访问..."
NO_AUTH=$(curl -s -o /dev/null -w "%{http_code}" "${API}/user/current")
if [ "$NO_AUTH" == "401" ]; then
    echo -e "  ${GREEN}✅ 无Token返回401${NC}"
    PASS=$((PASS + 1))
else
    echo -e "  ${RED}❌ 无Token期望401, 实际${NO_AUTH}${NC}"
    FAIL=$((FAIL + 1))
fi

echo -n "  无效Token..."
BAD_TOKEN_RESP=$(curl -s "${API}/user/current" -H "token: bad_token_here")
BAD_CODE=$(extract_code "$BAD_TOKEN_RESP")
if [ "$BAD_CODE" == "401" ] || echo "$BAD_TOKEN_RESP" | grep -q "认证失败"; then
    echo -e "  ${GREEN}✅ 无效Token被拒绝${NC}"
    PASS=$((PASS + 1))
else
    echo -e "  ${RED}❌ 无效Token未正确拦截${NC}"
    FAIL=$((FAIL + 1))
fi

echo -n "  不存在的失物ID..."
NOT_EXIST=$(curl -s "${API}/lost-item/99999")
CODE_NOT_EXIST=$(extract_code "$NOT_EXIST")
if [ "$CODE_NOT_EXIST" == "-1" ] || [ "$CODE_NOT_EXIST" == "404" ]; then
    echo -e "  ${GREEN}✅ 不存在ID正确返回错误${NC}"
    PASS=$((PASS + 1))
else
    echo -e "  ${RED}❌ 不存在ID返回异常: ${CODE_NOT_EXIST}${NC}"
    FAIL=$((FAIL + 1))
fi

# ---- 总结 ----
echo ""
echo "=============================================="
echo -e "  测试完成"
echo -e "  ${GREEN}✅ 通过: ${PASS}${NC}"
echo -e "  ${RED}❌ 失败: ${FAIL}${NC}"
echo "=============================================="

exit $FAIL
